package com.dexer.aquanaut.common.mud;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.core.BiomeRegistry;
import com.dexer.aquanaut.core.BlockRegistry;
import com.dexer.aquanaut.core.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.stream.Stream;

/**
 * Natural-looking spawns for the mud zone's creatures.
 *
 * <p>The biome is painted onto the shelf only, so the water the animals live in still reports the
 * vanilla ocean biome and vanilla's spawner never consults the mud zone's spawn list. This
 * spawner fills that gap: near each player it looks for open water directly above a mud-zone
 * floor, then releases the right animals, with a per-type cap so the flats never boil over.</p>
 */
@EventBusSubscriber(modid = Aquanaut.MODID)
public final class MudZoneSpawner {
    private static final int INTERVAL_TICKS = 200;
    private static final int ATTEMPTS_PER_PLAYER = 10;
    private static final int RADIUS = 56;
    /** How far above the player the column search starts. */
    private static final int SCAN_ABOVE = 16;
    private static final int MAX_PER_TYPE = 12;
    private static final int COUNT_RADIUS = 64;
    /** Monsters only surface where the light is dim, so the lit flats stay friendly. */
    private static final int MONSTER_LIGHT_LIMIT = 7;

    private record Spawn(EntityType<? extends Mob> type, int weight, int min, int max, boolean monster) {
    }

    private static List<Spawn> water;
    private static List<Spawn> all;

    private MudZoneSpawner() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        if (level.getGameTime() % INTERVAL_TICKS != 0L) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            trySpawnAround(level, player);
        }
    }

    private static void trySpawnAround(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.random;
        boolean monsters = level.getDifficulty() != Difficulty.PEACEFUL;
        int fromY = Math.min(player.getBlockY() + SCAN_ABOVE, level.getMaxBuildHeight() - 1);
        for (int attempt = 0; attempt < ATTEMPTS_PER_PLAYER; attempt++) {
            int x = player.getBlockX() + random.nextInt(RADIUS * 2 + 1) - RADIUS;
            int z = player.getBlockZ() + random.nextInt(RADIUS * 2 + 1) - RADIUS;
            BlockPos water = findWaterOnMud(level, x, z, fromY);
            if (water == null) {
                continue;
            }
            Spawn spawn = pick(random, monsters);
            if (spawn == null || countNear(level, water, spawn.type()) >= MAX_PER_TYPE) {
                continue;
            }
            if (spawn.monster() && level.getMaxLocalRawBrightness(water) > MONSTER_LIGHT_LIMIT) {
                continue;
            }
            if (release(level, water, spawn, random)) {
                return;
            }
        }
    }

    /**
     * Walks the column down from {@code fromY}, past any air, and returns the first water block
     * that has water above it and sits on a mud-zone floor. Stops at the first solid, non-water
     * block, so nothing is ever placed inside the shelf or the middle sea.
     */
    private static BlockPos findWaterOnMud(ServerLevel level, int x, int z, int fromY) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, fromY, z);
        int bottom = level.getMinBuildHeight() + 1;
        while (cursor.getY() > bottom) {
            BlockState state = level.getBlockState(cursor);
            if (!state.getFluidState().is(FluidTags.WATER)) {
                if (state.isAir()) {
                    cursor.move(Direction.DOWN);
                    continue;
                }
                return null;
            }
            BlockPos floor = cursor.below();
            if (isMudFloor(level.getBlockState(floor)) && level.getBiome(floor).is(BiomeRegistry.MUD_ZONE)) {
                return level.getFluidState(floor).is(FluidTags.WATER) ? cursor.immutable() : null;
            }
            cursor.move(Direction.DOWN);
        }
        return null;
    }

    private static boolean isMudFloor(BlockState state) {
        return state.is(BlockRegistry.MUD.get())
                || state.is(BlockRegistry.NUTRIENT_RICH_MUD.get())
                || state.is(BlockRegistry.PARASITIC_MUD.get())
                || state.is(BlockRegistry.PACKED_MUD.get())
                || state.is(BlockRegistry.SILTSTONE.get())
                || state.is(BlockRegistry.FOSSIL_BED.get());
    }

    private static Spawn pick(RandomSource random, boolean monsters) {
        List<Spawn> pool = pool(monsters);
        int total = 0;
        for (Spawn spawn : pool) {
            total += spawn.weight();
        }
        int roll = random.nextInt(total);
        for (Spawn spawn : pool) {
            roll -= spawn.weight();
            if (roll < 0) {
                return spawn;
            }
        }
        return pool.get(pool.size() - 1);
    }

    /** Built on first use, after the entity registry has been frozen. */
    private static List<Spawn> pool(boolean monsters) {
        if (all == null) {
            water = List.of(
                    new Spawn(EntityRegistry.AMBUSH_FISH.get(), 9, 2, 4, false),
                    new Spawn(EntityRegistry.GARDEN_EEL.get(), 8, 3, 6, false),
                    new Spawn(EntityRegistry.HERMIT_CRAB.get(), 8, 1, 3, false),
                    new Spawn(EntityRegistry.TRILOBITE.get(), 7, 2, 4, false),
                    new Spawn(EntityRegistry.HUMUS_JELLY.get(), 7, 1, 3, false),
                    new Spawn(EntityRegistry.ANCIENT_NAUTILUS.get(), 2, 1, 1, false));
            all = Stream.concat(water.stream(), Stream.of(
                    new Spawn(EntityRegistry.SEDIMENT_WORM.get(), 5, 1, 1, true),
                    new Spawn(EntityRegistry.MUD_SILVERFISH.get(), 4, 1, 2, true))).toList();
        }
        return monsters ? all : water;
    }

    private static int countNear(ServerLevel level, BlockPos pos, EntityType<?> type) {
        AABB box = new AABB(pos.getX() - COUNT_RADIUS, pos.getY() - COUNT_RADIUS,
                pos.getZ() - COUNT_RADIUS, pos.getX() + COUNT_RADIUS, pos.getY() + COUNT_RADIUS,
                pos.getZ() + COUNT_RADIUS);
        return level.getEntitiesOfClass(Entity.class, box, entity -> entity.getType() == type).size();
    }

    private static boolean release(ServerLevel level, BlockPos pos, Spawn spawn, RandomSource random) {
        int count = spawn.min() + random.nextInt(spawn.max() - spawn.min() + 1);
        boolean any = false;
        for (int i = 0; i < count; i++) {
            Mob mob = spawn.type().create(level);
            if (mob == null) {
                break;
            }
            mob.moveTo(pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 3.0D, pos.getY() + 0.1D,
                    pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 3.0D,
                    random.nextFloat() * 360.0F, 0.0F);
            if (!mob.checkSpawnRules(level, MobSpawnType.NATURAL) || !mob.checkSpawnObstruction(level)) {
                mob.discard();
                continue;
            }
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                    MobSpawnType.NATURAL, null);
            level.addFreshEntity(mob);
            any = true;
        }
        return any;
    }
}
