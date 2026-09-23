package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.core.ItemRegistry;

import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The underwater location cursor: a placed marker that pins a spot and carries a coloured tag.
 *
 * <p>
 * It is deliberately <em>not</em> a living entity. It behaves like a minecart instead:
 * {@link #isPushable()} and {@link #isPickable()} are on, {@link #push(Entity)} mirrors
 * {@code AbstractMinecart}'s separation, and — crucially — it is <em>not</em> a collision obstacle,
 * so walking into one shoves it instead of stopping against it. In water it is neutrally buoyant and
 * heavily damped, so a nudge settles instead of drifting — the pin stays where it was put. Out of
 * water it falls normally.
 *
 * <p>
 * Its tag — the label and its colour, the rule that no two markers share a name, and the right-click
 * that opens the editor — is the half it shares with every other marker in the pack, so it lives in
 * {@link AbstractTaggableEntity}.
 */
public class CursorEntity extends AbstractTaggableEntity implements GeoEntity {

    private static final RawAnimation FLOAT_ANIMATION = RawAnimation.begin().thenLoop("float");

    /** Velocity retained per tick while submerged. */
    private static final double WATER_DRAG = 0.86D;

    /** Velocity retained per tick in air. */
    private static final double AIR_DRAG = 0.98D;

    /** Downward acceleration out of water, in blocks/tick². */
    private static final double GRAVITY = 0.04D;

    /** Terminal fall speed, so a cursor dropped off a cliff cannot tunnel through the floor. */
    private static final double MAX_FALL_SPEED = 0.7D;

    /**
     * How much of a shove the cursor takes. It is a light floating marker, so it yields more than it
     * gives: the same impulse is passed on to the other entity at {@link #PUSH_BACK_RATIO}.
     */
    private static final double PUSH_IMPULSE = 0.06D;

    private static final double PUSH_BACK_RATIO = 0.25D;

    /** Built on first use: passing {@code this} at field-init time would leak a
     * half-constructed entity to GeckoLib before the subclass constructor has run. */
    private AnimatableInstanceCache cache;

    public CursorEntity(EntityType<? extends CursorEntity> type, Level level) {
        super(type, level);
    }

    // ------------------------------------------------------------------
    // physics — boat/minecart style
    // ------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (this.isRemoved()) {
            return;
        }

        Vec3 motion = this.getDeltaMovement();
        if (this.isInWater()) {
            // Neutrally buoyant: no net vertical force, and enough drag that a shove settles
            // instead of drifting the pin away from where it was placed.
            motion = motion.scale(WATER_DRAG);
        } else {
            motion = motion.add(0.0D, -GRAVITY, 0.0D).scale(AIR_DRAG);
            if (motion.y < -MAX_FALL_SPEED) {
                motion = new Vec3(motion.x, -MAX_FALL_SPEED, motion.z);
            }
        }

        this.setDeltaMovement(motion);
        this.move(MoverType.SELF, motion);
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    /**
     * Deliberately <b>not</b> overridden to {@code true}, which is the whole reason the cursor can be
     * pushed at all.
     *
     * <p>
     * {@code canBeCollidedWith()} makes the entity a collision obstacle, so a player walking into it
     * is stopped flush against its hitbox. {@code AABB.intersects} is strict, so boxes that merely
     * touch do not count as overlapping, and {@code LivingEntity.pushEntities} searches with the
     * player's exact bounding box — meaning a solid entity is never found and never pushed. This is
     * the minecart's recipe exactly: {@code AbstractMinecart} and {@code VehicleEntity} declare
     * {@code canBeCollidedWith} nowhere, so they inherit {@code false} and stay pushable.
     */
    @Override
    public boolean isPickable() {
        return true;
    }

    /**
     * Separation modelled on {@code AbstractMinecart#push}: the shove is normalised in the
     * horizontal plane and falls off with distance, so a cursor only really moves when something is
     * right up against it.
     *
     * <p>
     * Reached the same way a boat is: {@code LivingEntity#pushEntities} treats anything with
     * {@link #isPushable()} as pushable and calls {@code cursor.push(walker)}.
     */
    @Override
    public void push(Entity entity) {
        if (this.level().isClientSide || entity.noPhysics || this.noPhysics) {
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

    /** A location pin, and the one kind of marker the pack has had from the start. */
    @Override
    public Component kindLabel() {
        return Component.translatable("gui.aquanaut.marker.cursor");
    }

    /** The cursor item, so a broken cursor can be picked up and placed again — tag and all. */
    @Override
    protected Item taggedItem() {
        return ItemRegistry.CURSOR.get();
    }

    // ------------------------------------------------------------------
    // animation
    // ------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0,
                state -> state.setAndContinue(FLOAT_ANIMATION)));
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
