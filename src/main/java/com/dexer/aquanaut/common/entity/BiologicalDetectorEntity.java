package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.core.ItemRegistry;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The biological detector: a sealed radar buoy that holds station in the water and reads it.
 *
 * <p>
 * The whole machine is the ball, so it has no heading of its own: it is dropped into the water by
 * its own item, holds the depth it was placed at, and the animation tells the rest of the story. A
 * freshly deployed buoy is still folded shut, so its one animation is the deployment itself —
 * {@code release} plays once, the clamshell lifts off the array belt and the sensor core wakes up,
 * and {@code working} takes over and loops for the rest of its life. A buoy that has been picked up
 * and put back down deploys again, which is exactly what a redeployed instrument should do.
 *
 * <p>
 * It is deliberately <em>not</em> taggable, unlike the drone and the cursor: it is ballasted to
 * neutral buoyancy rather than being something to name and file away, so it extends {@code Entity}
 * directly and carries no name plate. A swing at one still means "pick that up" — see
 * {@link #hurt} and {@link #getPickResult} — which is the part of the taggable machinery a machine
 * actually wants.
 *
 * <p>
 * It has no gravity either. A deployed buoy hovers where it was set down: the water damps anything
 * that nudges it and nothing pulls it anywhere, so the hover in the working loop is the only motion
 * it has. The azimuth turn is likewise left to code: the animation only nods and hovers, so the
 * entity itself turns the hull at {@link DetectorScan#SPIN_DEGREES_PER_TICK} once the deployment
 * clip has handed over, which the client-side renderer forwards to GeckoLib <em>and</em> plots the
 * hologram's sweep on (see {@code BiologicalDetectorRenderer}). One number, so the spin the player
 * watches and the bearing the beam is reading can never disagree.
 */
public class BiologicalDetectorEntity extends Entity implements GeoEntity {

    /**
     * Deploy, then scan. GeckoLib queues the stages of one {@code RawAnimation}, so the release
     * clip is played exactly once and the working loop carries on from its end pose — the poses are
     * authored to meet, so the handover is seamless.
     */
    private static final RawAnimation DEPLOY_ANIMATION = RawAnimation.begin()
            .thenPlay(DetectorGeometry.RELEASE_ANIMATION)
            .thenLoop(DetectorGeometry.WORKING_ANIMATION);

    /**
     * How quickly the water and the air bleed off a shove, so the hull comes back to rest.
     *
     * <p>
     * Water is only slightly heavier than air: a buoy that has been bumped should drift for the
     * better part of a second and settle, not stop against the swimmer's hands.
     */
    private static final double WATER_DRAG = 0.86D;
    private static final double AIR_DRAG = 0.96D;

    /** Below this speed the buoy is simply still, and the integrator is skipped entirely. */
    private static final double REST_SPEED_SQUARED = 1.0E-6D;

    /** Where the server last said the hull is, and how many ticks the client has to get there. */
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private float lerpYRot;

    /** Built on first use: passing {@code this} at field-init time would leak a
     * half-constructed entity to GeckoLib before the subclass constructor has run. */
    private AnimatableInstanceCache cache;

    public BiologicalDetectorEntity(EntityType<? extends BiologicalDetectorEntity> type, Level level) {
        super(type, level);
    }

    /**
     * No synched data: a buoy has no shared state to keep in step.
     *
     * <p>
     * The taggable machinery this entity deliberately left behind is what usually needs synched
     * fields — a name and a colour that both sides must agree on. A detector has neither: its
     * deployment is animation, and its position and heading are the base class's own tracked data.
     */
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    /**
     * Takes a tracked position as a target to ease toward rather than a place to be.
     *
     * <p>
     * This override is the whole reason a pushed buoy moves smoothly. {@code Entity.lerpTo} — what a
     * plain entity inherits — is nothing but {@code setPos} and {@code setRot}: the interpolation
     * that makes the movement between server updates look continuous lives in {@code LivingEntity},
     * whose {@code lerpTo} stores the packet's target and whose {@code aiStep} spreads it over the
     * requested number of ticks. A machine that is moved by the server therefore stutters once per
     * position packet — twenty times a second, snapped — which is exactly what "laggy when pushed"
     * looks like. Storing the target and doing that same arithmetic here removes the snap.
     */
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (steps <= 0) {
            // No interpolation asked for: this is a teleport, so take it as one.
            this.lerpSteps = 0;
            this.setPos(x, y, z);
            this.setYRot(yRot);
            this.setXRot(xRot);
            return;
        }
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpSteps = steps;
    }

    /**
     * Holds station, damps shoves, and on the client eases toward the last server position.
     *
     * <p>
     * There is no gravity term on purpose: a buoy that sank would leave the depth the player chose,
     * and one that floated would end up pinned to the surface. Neutral buoyancy plus drag is what
     * makes it a fixture — a swimmer can still bump it out of the way, and it comes to rest a moment
     * later wherever they left it.
     *
     * <p>
     * Only the server moves it. A plain entity's position is tracked and sent, so the client copy
     * merely reports where the server says it is — running the same integration on both sides would
     * only give the two copies something to disagree about.
     */
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            interpolate();
            return;
        }
        scan();
        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() < REST_SPEED_SQUARED) {
            return;
        }
        this.move(MoverType.SELF, motion);
        double drag = this.isInWater() ? WATER_DRAG : AIR_DRAG;
        this.setDeltaMovement(new Vec3(motion.x * drag, motion.y * drag, motion.z * drag));
    }

    /**
     * Turns the hull on the spot, which is the whole of the scan as far as the entity is concerned.
     *
     * <p>
     * Nothing is reported here: the hull is radially symmetric, so its yaw is not a reading, it is
     * the direction the beam is pointing. The renderer takes the same yaw and draws the hologram's
     * sweep on it, so the ball the player watches turn and the blade of light going round inside the
     * sphere are the same rotation by construction rather than by two clocks that have to agree.
     *
     * <p>
     * Held back until {@link DetectorScan#DEPLOY_TICKS} so the hull stays put while the clamshell is
     * still opening — a buoy that started spinning on the seabed would look like it was failing to
     * deploy rather than deploying.
     */
    private void scan() {
        if (this.tickCount < DetectorScan.DEPLOY_TICKS) {
            return;
        }
        this.setYRot(Mth.wrapDegrees(this.getYRot() + DetectorScan.SPIN_DEGREES_PER_TICK));
    }

    /**
     * How far the hologram has come up, from nothing to fully deployed.
     *
     * <p>
     * The client's own tick counter, not the server's: this is a projection being drawn, and it has
     * to switch on in step with the animation the client is playing, which restarts whenever the
     * detector is loaded afresh.
     */
    public float hologramBoot(float partialTick) {
        float age = this.tickCount + partialTick - DetectorScan.DEPLOY_TICKS;
        return Mth.clamp(age / DetectorScan.BOOT_TICKS, 0.0F, 1.0F);
    }

    /**
     * Eases the client copy toward the last position the server sent, the way a living entity does.
     *
     * <p>
     * The renderer draws between {@code xo}/{@code yo}/{@code zo} and the current position, so
     * closing a fixed fraction of the remaining distance every tick — and wrapping the yaw, which a
     * full turn would otherwise take the long way around — is all the smoothing a tracked machine
     * needs.
     */
    private void interpolate() {
        if (this.lerpSteps <= 0) {
            return;
        }
        double x = this.getX() + (this.lerpX - this.getX()) / this.lerpSteps;
        double y = this.getY() + (this.lerpY - this.getY()) / this.lerpSteps;
        double z = this.getZ() + (this.lerpZ - this.getZ()) / this.lerpSteps;
        float yaw = this.getYRot() + Mth.wrapDegrees(this.lerpYRot - this.getYRot()) / this.lerpSteps;
        --this.lerpSteps;
        this.setPos(x, y, z);
        this.setYRot(yaw);
    }

    /**
     * A swing picks the buoy up rather than damaging it.
     *
     * <p>
     * Deliberately not damage: this is equipment with no health to take, and a player swinging at
     * one means "pick that up". The client reports the hit as accepted so the swing animates, but
     * only the server decides, so the item cannot be duplicated by a prediction that later turns
     * out to be wrong.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        if (!this.level().isClientSide) {
            playBrokenSound();
            this.spawnAtLocation(this.getPickResult());
            this.discard();
        }
        return true;
    }

    /**
     * What a swing — and a middle-click — hands over: the buoy itself.
     *
     * <p>
     * A detector is never renamed and carries no tag, so this is always a plain stack: one deployed
     * instrument is as good as another.
     */
    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ItemRegistry.BIOLOGICAL_DETECTOR.get());
    }

    /** A retrieved instrument powers down rather than yelping. */
    private void playBrokenSound() {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.NEUTRAL, 0.7F, 1.4F);
    }

    /**
     * Pickable but not solid, exactly like the drone: the swing that picks a buoy up needs it, and
     * the absence of {@code canBeCollidedWith} is what keeps it from blocking a diver.
     */
    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    /**
     * A ballasted hull does not drift in a current either, so the only thing that moves a deployed
     * buoy is a swimmer bumping into it. This is the fluid-type aware NeoForge hook — the one the
     * fluid-movement code calls; the no-argument vanilla overload is deprecated and ignored.
     */
    @Override
    public boolean isPushedByFluid(FluidType fluidType) {
        return false;
    }

    // ------------------------------------------------------------------
    // animation
    // ------------------------------------------------------------------

    /**
     * One controller with one queued animation: the buoy deploys itself as it hits the water and
     * then scans. There is no state to choose between — a detector has nothing to say other than
     * that it is working — so the controller never needs to stop.
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "detector", 0,
                state -> state.setAndContinue(DEPLOY_ANIMATION)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        AnimatableInstanceCache c = this.cache;
        if (c == null) {
            c = this.cache = GeckoLibUtil.createInstanceCache(this);
        }
        return c;
    }

    // ------------------------------------------------------------------
    // persistence
    // ------------------------------------------------------------------

    /**
     * Nothing to write: a buoy keeps no state of its own.
     *
     * <p>
     * Where it is and which way it faces is the ordinary entity data the base class already saves,
     * and its deployment is not a state to remember — a detector that is picked up and put down
     * again folds shut and redeploys, which is what the animation is for.
     */
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    /**
     * The hull's own heading, which is what the renderer hands to GeckoLib.
     *
     * <p>
     * GeckoLib reads a model's yaw from the living-body fields, which nothing but a
     * {@code LivingEntity} has, so a machine would otherwise be drawn at a fixed heading however it
     * was turned. Handing over the entity's yaw is what lets the code spin the ball.
     */
    public float getViewYRot(float partialTick) {
        return partialTick == 1.0F ? this.getYRot() : Mth.lerp(partialTick, this.yRotO, this.getYRot());
    }
}
