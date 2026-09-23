package com.dexer.aquanaut.client.renderer.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.dexer.aquanaut.common.entity.AbstractTaggableEntity;
import com.dexer.aquanaut.common.item.CompassTargeting;
import com.dexer.aquanaut.common.item.SubmarineCompassItem;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Drives the submarine compass needle, modelled directly on vanilla's
 * {@code CompassItemPropertyFunction}.
 *
 * <p>
 * Everything vanilla does here is deliberate and worth keeping:
 *
 * <ul>
 * <li>The needle is drawn <em>relative to the holder's facing</em>, so it reads as a compass in the
 * hand rather than a dial pinned to the world.</li>
 * <li>The yaw term is smoothed by a {@link CompassWobble}, which is what gives the vanilla compass
 * its slight lag when you spin on the spot.</li>
 * <li>When there is no valid target, the needle <em>wanders</em> instead of parking. That is
 * vanilla's "points at nothing" state, and it is why the compass needs no extra no-target frame:
 * a lost compass spins.</li>
 * </ul>
 *
 * <p>
 * The one departure is where the target comes from: vanilla reads a lodestone tracker, this reads
 * the marker chosen in the stack's NBT.
 */
public class SubmarineCompassItemPropertyFunction implements ClampedItemPropertyFunction {

    /** How far away a marker can be and still be pointed at. */
    private static final double SEARCH_RADIUS = 256.0D;

    private static final float SPIN_SCALE = 2.1474836E9F;

    private final CompassWobble wobble = new CompassWobble();
    private final CompassWobble wobbleRandom = new CompassWobble();

    @Override
    public float unclampedCall(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity holder,
            int seed) {
        Entity entity = holder != null ? holder : stack.getEntityRepresentation();
        if (entity == null) {
            return 0.0F;
        }
        ClientLevel clientLevel = level != null ? level
                : entity.level() instanceof ClientLevel client ? client : null;
        if (clientLevel == null) {
            return 0.0F;
        }
        return compassRotation(stack, clientLevel, seed, entity);
    }

    private float compassRotation(ItemStack stack, ClientLevel level, int seed, Entity holder) {
        AbstractTaggableEntity target = resolveTarget(stack, level, holder);
        long ticks = level.getGameTime();

        // Vanilla also treats a target you are standing inside as no target at all.
        if (target == null || target.position().distanceToSqr(holder.position()) < 1.0E-5D) {
            return wanderingRotation(seed, ticks);
        }
        return rotationTowards(holder, ticks, target);
    }

    private float rotationTowards(Entity holder, long ticks, AbstractTaggableEntity target) {
        double angle = CompassTargeting.angleToTarget(
                target.getX(), target.getZ(), holder.getX(), holder.getZ());
        double yaw = CompassTargeting.yawRotation(holder.getVisualRotationYInDegrees());

        if (holder instanceof Player player && player.isLocalPlayer()
                && player.level().tickRateManager().runsNormally()) {
            if (this.wobble.shouldUpdate(ticks)) {
                this.wobble.update(ticks, yaw);
            }
            return (float) CompassTargeting.positiveModulo(angle + this.wobble.rotation, 1.0D);
        }
        return (float) CompassTargeting.positiveModulo(angle + yaw, 1.0D);
    }

    /** Vanilla's lost-compass behaviour: the needle drifts, so it clearly points at nothing. */
    private float wanderingRotation(int seed, long ticks) {
        if (this.wobbleRandom.shouldUpdate(ticks)) {
            this.wobbleRandom.update(ticks, Math.random());
        }
        double offset = this.wobbleRandom.rotation + (double) ((float) hash(seed) / SPIN_SCALE);
        return Mth.positiveModulo((float) offset, 1.0F);
    }

    /**
     * Whichever marker the compass is currently set to: the nearest one by default, or the chosen
     * one, or nothing when that marker is gone.
     */
    @Nullable
    private static AbstractTaggableEntity resolveTarget(ItemStack stack, ClientLevel level, Entity holder) {
        List<AbstractTaggableEntity> markers = level.getEntitiesOfClass(AbstractTaggableEntity.class,
                holder.getBoundingBox().inflate(SEARCH_RADIUS));
        if (markers.isEmpty()) {
            return null;
        }

        List<CompassTargeting.Candidate> candidates = new ArrayList<>(markers.size());
        for (AbstractTaggableEntity marker : markers) {
            candidates.add(new CompassTargeting.Candidate(marker.getUUID(), marker.getX(), marker.getZ()));
        }

        Optional<CompassTargeting.Candidate> chosen;
        if (SubmarineCompassItem.getMode(stack) == SubmarineCompassItem.TargetMode.MARKER) {
            chosen = CompassTargeting.byId(candidates, SubmarineCompassItem.getTargetId(stack));
        } else {
            chosen = CompassTargeting.nearest(candidates, holder.getX(), holder.getZ());
        }
        if (chosen.isEmpty()) {
            return null;
        }

        UUID id = chosen.get().id();
        for (AbstractTaggableEntity marker : markers) {
            if (id.equals(marker.getUUID())) {
                return marker;
            }
        }
        return null;
    }

    private static int hash(int value) {
        return value * 1327217883;
    }

    /** Vanilla's needle smoothing, verbatim. */
    static class CompassWobble {
        double rotation;
        private double deltaRotation;
        private long lastUpdateTick;

        boolean shouldUpdate(long ticks) {
            return this.lastUpdateTick != ticks;
        }

        void update(long ticks, double target) {
            this.lastUpdateTick = ticks;
            double delta = target - this.rotation;
            delta = Mth.positiveModulo(delta + 0.5D, 1.0D) - 0.5D;
            this.deltaRotation += delta * 0.1D;
            this.deltaRotation *= 0.8D;
            this.rotation = Mth.positiveModulo(this.rotation + this.deltaRotation, 1.0D);
        }
    }
}
