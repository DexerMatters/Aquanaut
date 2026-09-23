package com.dexer.aquanaut.common.entity;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.dexer.aquanaut.common.drone.DroneControlInput;
import com.dexer.aquanaut.common.drone.SubmarineDroneService;
import com.dexer.aquanaut.common.item.SubmarineDroneControllerItem;
import com.dexer.aquanaut.core.ItemRegistry;
import com.dexer.aquanaut.core.SoundRegistry;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The submarine drone: a small stainless ROV that a player flies with a
 * {@link SubmarineDroneControllerItem}.
 *
 * <h3>A machine, not a fish</h3>
 * It is deliberately not a {@code WaterAnimal} and not a {@code LivingEntity} at all. It does not
 * breathe, does not age, has no health and appears in no spawn table; it is equipment that a player
 * deploys as an item and picks back up as an item. That is also what keeps it out of the fishing
 * pipelines: the scoop net only reaches {@code WaterAnimal}s and the aquarium catalogue is built
 * from the aquatic mob categories, so a drone can neither be caught nor filed away, without either
 * of those systems having to know the drone exists.
 *
 * <p>
 * Everything a living body gave the drone for free is therefore written here: its own integration
 * (thrust, drag, buoyancy), its own rigid rotation, and the same position replay a mob receives from
 * {@code LivingEntity} so that other players see it move smoothly. It keeps the half it shares with
 * every other marker in the pack — its tag — in {@link AbstractTaggableEntity}.
 *
 * <h3>Two states</h3>
 * <ul>
 * <li><b>Dead (unbound)</b> — an inert machine. The movement controller is off, the animation
 * controller returns {@link PlayState#STOP} so the model freezes in its bind pose with the impeller
 * still, the hull is drawn with the deactivated texture and the thrusters are cold. It is still a
 * physical object: it floats, and swimmers can shove it around.</li>
 * <li><b>Live (bound)</b> — a pilot holds the controller. The idle loop runs, the impeller spins up
 * with speed, the dive planes point the nose up and down, the rudder turns it, and the jet drives it
 * ahead or astern. Depth follows from the first and the last together: point the nose, then drive.</li>
 * </ul>
 *
 * <h3>A rigid hull has exactly one rotation</h3>
 * A steered machine has one heading. This drone therefore writes yaw and pitch straight onto the
 * entity and never derives them from a look target: what is drawn, what is steered and what the jet
 * pushes along are the same two numbers. The model's pitch is applied by
 * {@code SubmarineDroneRenderer}, since a rigid hull trims in a dive where a fish would bend.
 */
public class SubmarineDroneEntity extends AbstractTaggableEntity implements GeoEntity {

    /** The drone's single animation: a station-keeping idle with the impeller turning. */
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");

    private static final EntityDataAccessor<Boolean> CONTROLLED = SynchedEntityData.defineId(
            SubmarineDroneEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HEADLIGHT = SynchedEntityData.defineId(
            SubmarineDroneEntity.class, EntityDataSerializers.BOOLEAN);

    private static final String OPERATOR_KEY = "Operator";
    private static final String HEADLIGHT_KEY = "Headlight";

    // ── flight model ──────────────────────────────────────────────────────────

    /** Water-jet acceleration along the hull, blocks per tick squared. */
    private static final double THRUST_ACCELERATION = 0.040D;

    /** Speed ceiling along the hull. The water drag below supplies the rest. */
    private static final double MAX_SPEED = 0.36D;

    /** Speed ceiling vertically, so a hard climb does not launch the drone out of the water. */
    private static final double MAX_VERTICAL_SPEED = 0.24D;

    /** Velocity retained per tick submerged. A hull sheds a fifth of its speed every tick. */
    private static final double WATER_DRAG = 0.80D;

    /** Velocity retained per tick in air. */
    private static final double AIR_DRAG = 0.98D;

    /** Downward acceleration out of water, in blocks/tick². */
    private static final double AIR_GRAVITY = 0.06D;

    /** Terminal fall speed, so a drone dropped off a shelf cannot tunnel through the seabed. */
    private static final double MAX_FALL_SPEED = 1.0D;

    /** Degrees of yaw the rudder buys per tick. */
    private static final float YAW_RATE_DEGREES = 4.0F;

    /** Degrees of pitch the dive planes buy per tick, and how far they can take the nose. */
    private static final float PITCH_RATE_DEGREES = 2.5F;
    private static final float MAX_PITCH_DEGREES = 55.0F;

    /**
     * How much of a shove the drone takes, and how much of it the hull passes back. It is a heavy,
     * stiff body: it gives less than a buoy and shoves back harder, which is the opposite of the
     * cursor's trade.
     */
    private static final double PUSH_IMPULSE = 0.035D;
    private static final double PUSH_BACK_RATIO = 0.6D;

    /**
     * How long a command keeps applying after the last packet. A dropped packet or a lag spike must
     * not leave the drone running at full ahead, so the command is a dead-man switch rather than a
     * latched state.
     */
    private static final int CONTROL_TIMEOUT_TICKS = 8;

    /** Position error past which local prediction gives up and snaps to the server. */
    private static final double RECONCILE_SNAP_DISTANCE_SQ = 4.0D;

    /** The hull speed that counts as "cruise" when scaling the idle animation. */
    private static final double ANIMATION_REFERENCE_SPEED = 0.30D;

    /** Ticks between thruster hums while the drone is under power. */
    private static final int THRUSTER_SOUND_MIN_INTERVAL = 10;
    private static final int THRUSTER_SOUND_RANDOM_INTERVAL = 6;

    /** Built on first use: passing {@code this} at field-init time would leak a
     * half-constructed entity to GeckoLib before the subclass constructor has run. */
    private AnimatableInstanceCache cache;

    @Nullable
    private UUID operator;

    /** Latest command from the pilot. Server only; zeroed on timeout. */
    private int controlInput;

    /** Ticks since the last command arrived. */
    private int controlInputAge = CONTROL_TIMEOUT_TICKS;

    /** Counts down to the next thruster hum. */
    private int thrusterSoundCooldown;

    /**
     * Set by the client while this client is the one flying this drone, and never set on the server.
     * It is what turns the client from a spectator of the drone's motion into a participant.
     */
    private boolean locallyPiloted;

    /** The command read from the local keyboard. Client only, and only while {@link #locallyPiloted}. */
    private int localControlInput;

    /** Where the server says this drone is, replayed while somebody else is flying it. */
    private double replayX;
    private double replayY;
    private double replayZ;
    private float replayYRot;
    private float replayXRot;
    private int replaySteps;

    public SubmarineDroneEntity(EntityType<? extends SubmarineDroneEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CONTROLLED, false);
        builder.define(HEADLIGHT, false);
    }

    // ------------------------------------------------------------------
    // control state
    // ------------------------------------------------------------------

    /** Whether a pilot is bound to this drone. */
    public boolean isControlled() {
        return this.entityData.get(CONTROLLED);
    }

    /** Whether the forward floodlight is currently enabled. */
    public boolean isHeadlightOn() {
        return this.entityData.get(HEADLIGHT);
    }

    /** Changes the floodlight state. Server side; the entity data mirrors it to clients. */
    public void setHeadlightOn(boolean enabled) {
        this.entityData.set(HEADLIGHT, enabled && isControlled());
    }

    /** Toggles the floodlight and returns its new state. */
    public boolean toggleHeadlight() {
        setHeadlightOn(!isHeadlightOn());
        return isHeadlightOn();
    }

    /** The pilot bound to this drone, if any. Server side only. */
    public Optional<UUID> operatorId() {
        return Optional.ofNullable(this.operator);
    }

    /** Binds a pilot, or releases the drone when given {@code null}. */
    public void setOperator(@Nullable UUID operator) {
        this.operator = operator;
        this.entityData.set(CONTROLLED, operator != null);
        if (operator == null) {
            this.entityData.set(HEADLIGHT, false);
        }
        if (operator == null) {
            // Releasing the controls must also release the thrust, or the drone would keep its last
            // command until the dead-man switch timed out.
            this.controlInput = 0;
            this.controlInputAge = CONTROL_TIMEOUT_TICKS;
        }
    }

    /** Whether {@code player} is the pilot currently bound to this drone. */
    public boolean isOperatedBy(Player player) {
        return this.operator != null && this.operator.equals(player.getUUID());
    }

    /** Records a command from the pilot. Server side. */
    public void setControlInput(int input) {
        this.controlInput = DroneControlInput.sanitize(input);
        this.controlInputAge = 0;
    }

    /** The command actually in force, which is nothing once the pilot stops reporting in. */
    public int activeControlInput() {
        return this.controlInputAge > CONTROL_TIMEOUT_TICKS ? 0 : this.controlInput;
    }

    // ------------------------------------------------------------------
    // local prediction
    // ------------------------------------------------------------------

    /**
     * Marks this drone as flown by the local client, which switches the client from replaying the
     * server's motion to simulating it.
     *
     * <p>
     * The alternative is a round trip of input lag on every keypress, which is very obvious on
     * something steered by hand. The server stays authoritative: the client's copy is a prediction,
     * and {@link #lerpTo} quietly reconciles the two whenever they disagree by more than a nudge.
     */
    public void setLocallyPiloted(boolean piloted) {
        this.locallyPiloted = piloted;
        if (!piloted) {
            this.localControlInput = 0;
        }
    }

    /** Registers the command the local client is applying this tick. */
    public void setLocalControlInput(int input) {
        this.localControlInput = DroneControlInput.sanitize(input);
    }

    /**
     * Locally-flown drones are simulated by the client, exactly as a ridden boat is.
     *
     * <p>
     * This is what makes the client the authority on its own prediction: the client ignores every
     * position packet that arrives while it is flying ({@code ClientPacketListener} drops them for
     * anything that reports itself controlled locally), so the two simulations never fight.
     */
    @Override
    public boolean isControlledByLocalInstance() {
        return this.locallyPiloted || super.isControlledByLocalInstance();
    }

    /**
     * Server corrections arrive here. While predicting, small ones are the difference between two
     * simulations of the same thing and are dropped — applying them would drag the drone backwards
     * on every packet — and only a real disagreement is honoured.
     *
     * <p>
     * While <em>not</em> predicting, the position is not applied directly: it becomes the target of
     * the same few-tick replay a mob gets from {@code LivingEntity}, which is what turns a packet
     * every third tick into smooth motion for everybody watching somebody else fly.
     */
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (this.locallyPiloted) {
            double dx = x - this.getX();
            double dy = y - this.getY();
            double dz = z - this.getZ();
            if (dx * dx + dy * dy + dz * dz < RECONCILE_SNAP_DISTANCE_SQ) {
                return;
            }
            this.setLocallyPiloted(false);
            super.lerpTo(x, y, z, yRot, xRot, steps);
            this.setLocallyPiloted(true);
            return;
        }
        this.replayX = x;
        this.replayY = y;
        this.replayZ = z;
        this.replayYRot = yRot;
        this.replayXRot = xRot;
        this.replaySteps = Math.max(1, steps);
    }

    /** The same argument as {@link #lerpTo}: the server's velocity would undo the prediction. */
    @Override
    public void lerpMotion(double x, double y, double z) {
        if (this.locallyPiloted) {
            return;
        }
        super.lerpMotion(x, y, z);
    }

    /**
     * Where the replay is heading. Vanilla's packet for a rotation-only update re-sends the last
     * position it knows about, and it reads it from here, so a drone that is only turning still
     * keeps the place it had.
     */
    @Override
    public double lerpTargetX() {
        return this.replaySteps > 0 ? this.replayX : this.getX();
    }

    @Override
    public double lerpTargetY() {
        return this.replaySteps > 0 ? this.replayY : this.getY();
    }

    @Override
    public double lerpTargetZ() {
        return this.replaySteps > 0 ? this.replayZ : this.getZ();
    }

    @Override
    public float lerpTargetXRot() {
        return this.replaySteps > 0 ? this.replayXRot : this.getXRot();
    }

    @Override
    public float lerpTargetYRot() {
        return this.replaySteps > 0 ? this.replayYRot : this.getYRot();
    }

    /**
     * Advances the replay by one of its steps.
     *
     * <p>
     * The movement is written back as the drone's velocity as well, because that is what the
     * impeller animation is scaled by and what the feed's speed readout shows: replaying the
     * position alone would leave a drone that is visibly travelling with its instruments reading
     * zero.
     */
    private void replayServerMotion() {
        if (this.replaySteps <= 0) {
            return;
        }

        double x = this.getX() + (this.replayX - this.getX()) / (double) this.replaySteps;
        double y = this.getY() + (this.replayY - this.getY()) / (double) this.replaySteps;
        double z = this.getZ() + (this.replayZ - this.getZ()) / (double) this.replaySteps;
        float yRot = this.getYRot() + Mth.wrapDegrees(this.replayYRot - this.getYRot()) / (float) this.replaySteps;
        float xRot = this.getXRot() + (this.replayXRot - this.getXRot()) / (float) this.replaySteps;

        this.setDeltaMovement(x - this.getX(), y - this.getY(), z - this.getZ());
        this.setPos(x, y, z);
        this.setRot(yRot, xRot);
        this.replaySteps--;
    }

    // ------------------------------------------------------------------
    // simulation
    // ------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (this.isRemoved()) {
            return;
        }

        if (this.level().isClientSide) {
            if (this.locallyPiloted) {
                this.applyControls(this.localControlInput);
                this.integrate();
            } else {
                this.replayServerMotion();
            }
        } else {
            this.controlInputAge++;
            this.applyControls(this.activeControlInput());
            this.tickThrusterNoise();
            // A link that has run out of radio is dropped by the drone rather than by the pilot: the
            // pilot's client is the one end that has already stopped being able to see it.
            SubmarineDroneService.enforceLinkRange(this);
            this.integrate();
        }
    }

    /**
     * Applies one tick of the pilot's command.
     *
     * <p>
     * The rudder and the dive planes are independent of the jet: attitude answers on its own so the
     * drone can be pointed before it is driven, and the jet pushes along the <em>level</em> heading
     * rather than along the camera, so the cosmetic trim can never curve the flight path.
     */
    private void applyControls(int input) {
        if (!this.isControlled() || input == 0) {
            return;
        }

        boolean turned = false;

        float pitch = this.getXRot();
        if (DroneControlInput.has(input, DroneControlInput.PITCH_UP)) {
            pitch -= PITCH_RATE_DEGREES;
        }
        if (DroneControlInput.has(input, DroneControlInput.PITCH_DOWN)) {
            pitch += PITCH_RATE_DEGREES;
        }
        pitch = Mth.clamp(pitch, -MAX_PITCH_DEGREES, MAX_PITCH_DEGREES);
        if (pitch != this.getXRot()) {
            this.setXRot(pitch);
            turned = true;
        }

        float yaw = this.getYRot();
        if (DroneControlInput.has(input, DroneControlInput.YAW_LEFT)) {
            yaw -= YAW_RATE_DEGREES;
        }
        if (DroneControlInput.has(input, DroneControlInput.YAW_RIGHT)) {
            yaw += YAW_RATE_DEGREES;
        }
        if (yaw != this.getYRot()) {
            this.setYRot(yaw);
            turned = true;
        }

        if (turned) {
            this.hasImpulse = true;
        }

        if (!this.isInWater()) {
            return;
        }

        // The jet pushes along the hull, pitch included: that is the whole mechanism by which the
        // dive planes change depth.
        Vec3 motion = this.getDeltaMovement();
        boolean ahead = DroneControlInput.has(input, DroneControlInput.THRUST_FORWARD);
        boolean astern = DroneControlInput.has(input, DroneControlInput.THRUST_BACKWARD);
        if (ahead != astern) {
            motion = motion.add(this.getLookAngle().scale(ahead ? THRUST_ACCELERATION : -THRUST_ACCELERATION));
        }

        this.setDeltaMovement(clampSpeed(motion));
        if (ahead || astern) {
            this.hasImpulse = true;
        }
    }

    /**
     * Integrates one tick of motion: drag, then displacement.
     *
     * <p>
     * Submerged it is neutrally buoyant, so there is no net vertical force and the only thing that
     * changes the drone's speed is the water itself — which is what makes the controls feel like a
     * boat rather than a bullet. In air it falls like any other dropped machine, with a terminal
     * velocity so it cannot tunnel through the seabed.
     */
    private void integrate() {
        Vec3 motion = this.getDeltaMovement();
        if (this.isInWater()) {
            motion = motion.scale(WATER_DRAG);
        } else {
            motion = motion.add(0.0D, -AIR_GRAVITY, 0.0D).scale(AIR_DRAG);
            if (motion.y < -MAX_FALL_SPEED) {
                motion = new Vec3(motion.x, -MAX_FALL_SPEED, motion.z);
            }
        }

        this.setDeltaMovement(motion);
        this.move(MoverType.SELF, motion);
    }

    private static Vec3 clampSpeed(Vec3 motion) {
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (horizontal > MAX_SPEED) {
            double scale = MAX_SPEED / horizontal;
            motion = new Vec3(motion.x * scale, motion.y, motion.z * scale);
        }
        return new Vec3(motion.x, Mth.clamp(motion.y, -MAX_VERTICAL_SPEED, MAX_VERTICAL_SPEED), motion.z);
    }

    /**
     * A sparse thruster hum, in two places: at the drone for anyone nearby, and routed straight to
     * the pilot, who is not near the drone but is watching through it.
     */
    private void tickThrusterNoise() {
        if (this.thrusterSoundCooldown > 0) {
            this.thrusterSoundCooldown--;
        }
        // Only a powered thruster makes noise; working the rudder alone does not.
        if (!DroneControlInput.powered(this.activeControlInput())
                || !this.isInWater() || this.thrusterSoundCooldown > 0) {
            return;
        }

        this.thrusterSoundCooldown = THRUSTER_SOUND_MIN_INTERVAL
                + this.random.nextInt(THRUSTER_SOUND_RANDOM_INTERVAL);
        float speed = (float) Mth.clamp(this.getDeltaMovement().length() / MAX_SPEED, 0.0D, 1.0D);

        this.level().playSound(null, this, SoundRegistry.SUBMARINE_DRONE_THRUSTER.get(), SoundSource.NEUTRAL,
                0.22F + 0.16F * speed, 0.85F + 0.35F * speed);

        ServerPlayer pilot = this.operator == null || this.level().getServer() == null
                ? null
                : this.level().getServer().getPlayerList().getPlayer(this.operator);
        if (pilot != null) {
            pilot.connection.send(new ClientboundSoundPacket(
                    SoundRegistry.SUBMARINE_DRONE_THRUSTER, SoundSource.PLAYERS,
                    pilot.getX(), pilot.getY(), pilot.getZ(),
                    0.30F + 0.20F * speed, 0.85F + 0.35F * speed, this.random.nextLong()));
        }
    }

    // ------------------------------------------------------------------
    // rigid rotation
    // ------------------------------------------------------------------

    /**
     * The camera, the look vector and the jet all point where the hull points.
     *
     * <p>
     * {@code Entity} interpolates its view rotation with a plain lerp, which takes the long way
     * round whenever a heading crosses due north; a rudder that answers in four-degree steps crosses
     * it constantly, so this uses the wrapping interpolation instead.
     */
    @Override
    public float getViewYRot(float partialTick) {
        return partialTick == 1.0F ? this.getYRot() : Mth.rotLerp(partialTick, this.yRotO, this.getYRot());
    }

    // ------------------------------------------------------------------
    // pushability
    // ------------------------------------------------------------------

    @Override
    public boolean isPushable() {
        return true;
    }

    /**
     * Pickable but not solid: a swimmer who bumps into a drone shoves it and keeps swimming, exactly
     * as they do with a cursor or a boat. See {@code CursorEntity#isPickable} for why the absence of
     * {@code canBeCollidedWith} is what makes that work.
     */
    @Override
    public boolean isPickable() {
        return true;
    }

    /**
     * A shove is applied on both sides, so the prediction and the authority stay in step. On the
     * client that is only legitimate for a drone this very client is flying: an unpiloted one is
     * owned by the server and is only replayed here, so moving it locally would be a guess the next
     * position packet has to undo.
     */
    @Override
    public void push(Entity entity) {
        if ((this.level().isClientSide && !this.locallyPiloted) || entity.noPhysics || this.noPhysics) {
            return;
        }
        if (this.hasPassenger(entity) || this.isPassengerOfSameVehicle(entity)) {
            return;
        }

        double dx = entity.getX() - this.getX();
        double dz = entity.getZ() - this.getZ();
        double distanceSq = dx * dx + dz * dz;
        if (distanceSq < 1.0E-4D) {
            return;
        }

        double distance = Math.sqrt(distanceSq);
        dx /= distance;
        dz /= distance;
        double strength = Math.min(1.0D, 1.0D / distance);

        this.push(-dx * strength * PUSH_IMPULSE, 0.0D, -dz * strength * PUSH_IMPULSE);
        if (entity.isPushable()) {
            entity.push(dx * strength * PUSH_IMPULSE * PUSH_BACK_RATIO, 0.0D,
                    dz * strength * PUSH_IMPULSE * PUSH_BACK_RATIO);
        }
    }

    // ------------------------------------------------------------------
    // interaction
    // ------------------------------------------------------------------

    /**
     * Right-clicking with a controller swallows the click.
     *
     * <p>
     * Attaching is owned by the network layer: the client watches the raw mouse button and reports
     * the drone under the crosshair, and the server decides. Consuming the press here is what stops
     * the same click from also being read as a world interaction.
     *
     * <p>
     * With anything else in hand the press falls through to the ordinary interaction, which for a
     * taggable means the tag editor: the drone is named exactly like a cursor, by right-clicking it
     * with the controller put away.
     */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!(player.getItemInHand(hand).getItem() instanceof SubmarineDroneControllerItem)) {
            return super.interact(player, hand);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    /**
     * Picking a drone up ends the flight, so the pilot's controller is unlinked before the hull
     * becomes an item again: a link pointing at an entity that no longer exists would leave the
     * controller reading "attached" with nothing to fly.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide) {
            releasePilot();
        }
        return super.hurt(source, amount);
    }

    private void releasePilot() {
        UUID pilotId = this.operator;
        this.setOperator(null);
        if (pilotId == null || !(this.level() instanceof ServerLevel level) || level.getServer() == null) {
            return;
        }
        ServerPlayer pilot = level.getServer().getPlayerList().getPlayer(pilotId);
        if (pilot != null) {
            SubmarineDroneService.forgetDrone(pilot, this.getUUID());
        }
    }

    /** A drone, which is what tells it apart from the pins in a list that holds both. */
    @Override
    public Component kindLabel() {
        return Component.translatable("gui.aquanaut.marker.drone");
    }

    /** The retrieved hull, so a drone that was picked up can be deployed again — tag and all. */
    @Override
    protected Item taggedItem() {
        return ItemRegistry.SUBMARINE_DRONE.get();
    }

    /** A retrieved machine clunks out of the water rather than yelping. */
    @Override
    protected void playBrokenSound() {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundRegistry.SUBMARINE_DRONE_RETRIEVE.get(), SoundSource.NEUTRAL, 0.8F, 1.0F);
    }

    // ------------------------------------------------------------------
    // persistence
    // ------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.operator != null) {
            tag.putUUID(OPERATOR_KEY, this.operator);
        }
        if (isHeadlightOn()) {
            tag.putBoolean(HEADLIGHT_KEY, true);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setOperator(tag.hasUUID(OPERATOR_KEY) ? tag.getUUID(OPERATOR_KEY) : null);
        setHeadlightOn(tag.getBoolean(HEADLIGHT_KEY));
    }

    // ------------------------------------------------------------------
    // animation
    // ------------------------------------------------------------------

    /**
     * One controller, three outcomes: an unbound drone stops, which snaps the bones back to the bind
     * pose — a dead machine rather than a swimming fish; a bound drone runs the idle loop, with the
     * impeller wound up in proportion to how fast the hull is actually travelling.
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (!this.isControlled()) {
                return PlayState.STOP;
            }
            double speed = 0.7D + Mth.clamp(this.getDeltaMovement().length() / ANIMATION_REFERENCE_SPEED,
                    0.0D, 1.0D) * 1.8D;
            state.setControllerSpeed((float) speed);
            return state.setAndContinue(IDLE_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        AnimatableInstanceCache c = this.cache;
        if (c == null) {
            c = this.cache = GeckoLibUtil.createInstanceCache(this);
        }
        return c;
    }
}
