package com.dexer.aquanaut.common.sonar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.dexer.aquanaut.Aquanaut;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * What one pulse finds.
 *
 * <p>
 * The pulse is listened for in a fixed set of directions spreading out from the diver's eye, and
 * each direction is followed until it meets something. What it meets is then sorted into a voice:
 *
 * <ul>
 * <li>a body answers for itself — {@link SonarSignal#BIOLOGICAL}, or
 * {@link SonarSignal#ABYSS} for something too large to be ordinary;</li>
 * <li>a block in the {@code aquanaut:sonar_minerals} tag answers as crystal —
 * {@link SonarSignal#MINERAL};</li>
 * <li>anything else solid is followed a little further, and if the pulse comes out the far side the
 * contact is the void it came out into, not the wall it went through —
 * {@link SonarSignal#CAVITY}.</li>
 * </ul>
 *
 * <p>
 * That last rule is the whole reason a sounder is worth carrying. Rock is not a contact: it is the
 * medium the pulse dies in, and a wall with stone behind it tells the diver nothing they could not
 * see. A wall with a cave behind it tells them everything, and it is precisely the thing they cannot
 * see from where they are floating. So the scan ignores the face and reports the hollow, which is
 * why one ray per direction is enough and why the returned contacts are few and all worth reading.
 * The hollow has to be <em>behind</em> the rock, though, in both senses: reached by going into the
 * face rather than along it, and out of sight from where the diver is floating. Get either wrong and
 * the instrument spends the dive reporting the water the diver is already standing in.
 *
 * <p>
 * Bodies are read straight through rock, because sound does not care about the wall between the
 * diver and a shoal, and a sounder that could not hear through the wreck it is swimming around
 * would be a worse instrument than the eyes it replaces.
 *
 * <h3>Extending it</h3>
 *
 * <p>
 * Nothing here knows the name of a single block or creature. Crystal is whatever the block tag
 * lists; a leviathan is whatever the entity-type tag lists or whatever is simply bigger than
 * {@link #ABYSS_SIZE}. New ore, new fish, new caverns and the abyssal content still to come all
 * arrive by pointing data at them — see {@code data/aquanaut/tags}.
 */
public final class SonarScan {

    /**
     * What the pulse treats as crystal and ore. A tag rather than a list in code, so a modpack or a
     * later content pass can make its own minerals sing without this file changing.
     */
    public static final TagKey<net.minecraft.world.level.block.Block> MINERALS = BlockTags.create(
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "sonar_minerals"));

    /** The creatures whose echo is the long one, whatever their size. */
    public static final TagKey<EntityType<?>> ABYSSAL = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "sonar_abyssal"));

    /**
     * How many directions one ping is listened in.
     *
     * <p>
     * Each ray is a full raycast, and a ping is fired at most once every five seconds, so the cost
     * is paid rarely; what buys the number is the far end of the range. Six hundred and forty rays
     * put the gaps between neighbouring directions at a little over a block at twenty blocks out, so
     * a one-block vein or a one-block crevice cannot hide between two of them.
     */
    public static final int RAY_COUNT = 640;

    /** How many contacts a single ping may report, strongest first. */
    public static final int MAX_CONTACTS = 24;

    /** How many bodies are read before the rest are left for the next ping. */
    private static final int MAX_BODIES = 48;

    /**
     * How large a creature has to be, in blocks across or tall, before its echo is the long one.
     * Three blocks is a body far past anything a diver meets by accident.
     */
    public static final float ABYSS_SIZE = 3.0F;

    /** How close a body has to be before it is the diver's own reflection and not a contact. */
    private static final double SELF_RADIUS_SQ = 0.16D;

    private SonarScan() {
    }

    /**
     * Reads the world around a diver and returns the contacts, strongest first and already folded
     * together. Empty when the diver is floating in open water with nothing in earshot, which is a
     * reading in itself.
     */
    public static List<SonarReturn> scan(Level level, Player player) {
        Vec3 origin = player.getEyePosition();
        List<SonarReturn> rays = new ArrayList<>();

        bodies(level, player, origin, rays);
        terrain(level, player, origin, rays);

        return SonarSphere.cluster(rays, MAX_CONTACTS);
    }

    // ------------------------------------------------------------------
    // what is swimming
    // ------------------------------------------------------------------

    private static void bodies(Level level, Player player, Vec3 origin, List<SonarReturn> rays) {
        AABB reach = new AABB(origin, origin).inflate(SonarPulse.RANGE);
        List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, reach,
                entity -> entity != player && entity.isAlive() && !entity.isSpectator());
        found.sort(Comparator.comparingDouble(
                entity -> entity.position().distanceToSqr(origin)));

        int read = 0;

        for (LivingEntity body : found) {
            if (read++ >= MAX_BODIES) {
                break;
            }

            Vec3 offset = body.getBoundingBox().getCenter().subtract(origin);
            double squared = offset.lengthSqr();

            if (squared < SELF_RADIUS_SQ || squared > SonarPulse.RANGE * SonarPulse.RANGE) {
                continue;
            }

            SonarSignal voice = classify(body);
            rays.add(SonarReturn.at(voice, offset, bodyStrength(body, Math.sqrt(squared)) * voice.gain));
        }
    }

    /**
     * Which voice a body answers in. The tag is checked first so that a small abyssal creature still
     * reads as what it is, and the size fallback means the leviathans that already ship do not have
     * to be listed to be heard.
     */
    public static SonarSignal classify(LivingEntity body) {
        if (body.getType().is(ABYSSAL)) {
            return SonarSignal.ABYSS;
        }
        return Math.max(body.getBbWidth(), body.getBbHeight()) >= ABYSS_SIZE
                ? SonarSignal.ABYSS
                : SonarSignal.BIOLOGICAL;
    }

    /**
     * How loud a body is. A diver hears the near ones best and the large ones best, and everything
     * has a floor: a fish at the edge of the range is still a fish.
     */
    private static float bodyStrength(LivingEntity body, double distance) {
        float bulk = Mth.clamp(Math.max(body.getBbWidth(), body.getBbHeight()) / ABYSS_SIZE, 0.12F, 1.0F);
        float near = (float) (1.0D - distance / SonarPulse.RANGE);
        return Mth.clamp(0.22F + 0.48F * near + 0.30F * bulk, 0.05F, 1.0F);
    }

    // ------------------------------------------------------------------
    // what is not
    // ------------------------------------------------------------------

    private static void terrain(Level level, Player player, Vec3 origin, List<SonarReturn> rays) {
        for (Vec3 direction : SonarSphere.directions(RAY_COUNT)) {
            Vec3 end = origin.add(direction.scale(SonarPulse.RANGE));
            // Fluids are ignored on purpose: the pulse is *in* the water, and a sounder that
            // stopped at the surface of it would report the sea as a wall.
            BlockHitResult hit = level.clip(new ClipContext(origin, end,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

            if (hit.getType() != HitResult.Type.BLOCK) {
                continue;
            }

            Vec3 face = hit.getLocation();
            double distance = face.distanceTo(origin);
            BlockState state = level.getBlockState(hit.getBlockPos());

            if (state.is(MINERALS)) {
                rays.add(SonarReturn.at(SonarSignal.MINERAL, face.subtract(origin),
                        (0.55F + 0.45F * (float) (1.0D - distance / SonarPulse.RANGE)) * SonarSignal.MINERAL.gain));
                continue;
            }

            BlockPos hollow = hollowBehind(level, player, origin, hit);

            if (hollow != null) {
                rays.add(SonarReturn.at(SonarSignal.CAVITY,
                        Vec3.atCenterOf(hollow).subtract(origin),
                        (0.45F + 0.40F * (float) (1.0D - distance / SonarPulse.RANGE)) * SonarSignal.CAVITY.gain));
            }
        }
    }

    /**
     * Follows the pulse past the face it stopped on, looking for the first gap. Returns the position
     * of the gap, or {@code null} when the rock is solid all the way in — which is every ordinary
     * wall, and the reason a ping in a corridor reports nothing at all.
     *
     * <p>
     * A gap is only a cavity if the diver could not already see it. Open water glimpsed round the
     * side of a pillar is a gap with a face in front of it, but it is not somewhere to go looking
     * for anything, and a sounder that lit up for it would be reporting the diver's own surroundings
     * back at them. The test is a straight line from the eye to the gap: if it arrives unobstructed
     * then the pulse came back off a surface the diver can simply look at, and the reading is worth
     * nothing.
     */
    private static BlockPos hollowBehind(Level level, Player player, Vec3 origin, BlockHitResult hit) {
        for (BlockPos probe : SonarCavity.probes(hit.getBlockPos(), hit.getDirection())) {
            if (level.isOutsideBuildHeight(probe) || !level.isLoaded(probe)) {
                return null;
            }

            if (!level.getBlockState(probe).getCollisionShape(level, probe).isEmpty()) {
                continue;
            }

            // The first gap is the one the reading is about, whether it turns out to be a cavity or
            // the diver's own water on the other side of a thin wall.
            Vec3 centre = Vec3.atCenterOf(probe);
            BlockHitResult line = level.clip(new ClipContext(origin, centre,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

            return line.getType() == HitResult.Type.BLOCK ? probe : null;
        }

        return null;
    }
}
