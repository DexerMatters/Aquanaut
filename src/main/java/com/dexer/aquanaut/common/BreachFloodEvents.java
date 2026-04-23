package com.dexer.aquanaut.common;

import com.dexer.aquanaut.Aquanaut;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Aquanaut.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class BreachFloodEvents {
    private static final int MAX_VOLUME = 2000;
    private static final int MIN_FILL_PER_TICK = 1;
    private static final int MAX_FILL_PER_TICK = 4;
    private static final int PENDING_ACTIVATION_TICKS = 20;
    private static final double SUCTION_RADIUS = 6.0D;
    private static final double SUCTION_STRENGTH = 0.06D;
    private static final Direction[] DIRECTIONS = Direction.values();

    private static final List<FloodSeed> PENDING_SEEDS = new ArrayList<>();
    private static final Map<ResourceKey<Level>, Deque<FloodTask>> ACTIVE_TASKS = new HashMap<>();
    private static final Set<FloodKey> ACTIVE_KEYS = new HashSet<>();

    private BreachFloodEvents() {
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        queueFloodIfBreach(level, event.getPos());
    }

    @SubscribeEvent
    public static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!event.getOriginalState().isAir() || !event.getNewState().getFluidState().is(FluidTags.WATER)) {
            return;
        }

        queueFloodIfBreach(level, event.getPos());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        activatePendingSeeds();
        processFloodTasks(server);
    }

    private static void activatePendingSeeds() {
        Iterator<FloodSeed> iterator = PENDING_SEEDS.iterator();
        while (iterator.hasNext()) {
            FloodSeed seed = iterator.next();
            List<BlockPos> entryPoints = findEntryPoints(seed.level, seed.pos);
            if (!entryPoints.isEmpty()) {
                ACTIVE_TASKS.computeIfAbsent(seed.key.dimension(), dimension -> new ArrayDeque<>())
                        .add(new FloodTask(seed.level, seed.pos, entryPoints, seed.pressure, seed.key));
                iterator.remove();
                continue;
            }

            seed.attempts++;
            if (seed.attempts >= PENDING_ACTIVATION_TICKS) {
                ACTIVE_KEYS.remove(seed.key);
                iterator.remove();
            }
        }
    }

    private static void processFloodTasks(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            Deque<FloodTask> tasks = ACTIVE_TASKS.get(level.dimension());
            if (tasks == null || tasks.isEmpty()) {
                continue;
            }

            Iterator<FloodTask> iterator = tasks.iterator();
            while (iterator.hasNext()) {
                FloodTask task = iterator.next();
                if (task.tick()) {
                    ACTIVE_KEYS.remove(task.key);
                    iterator.remove();
                }
            }

            if (tasks.isEmpty()) {
                ACTIVE_TASKS.remove(level.dimension());
            }
        }
    }

    private static void queueFloodIfBreach(ServerLevel level, BlockPos pos) {
        if (!isBreachCandidate(level, pos)) {
            return;
        }

        float pressure = PressureHelper.getPressure(level, pos);
        FloodKey key = new FloodKey(level.dimension(), pos);
        if (!ACTIVE_KEYS.add(key)) {
            return;
        }

        PENDING_SEEDS.add(new FloodSeed(key, level, pos, pressure));
    }

    private static boolean isBreachCandidate(ServerLevel level, BlockPos pos) {
        return hasAdjacentWater(level, pos) && hasAdjacentAir(level, pos);
    }

    private static boolean hasAdjacentWater(ServerLevel level, BlockPos pos) {
        for (Direction direction : DIRECTIONS) {
            BlockState state = level.getBlockState(pos.relative(direction));
            if (state.getFluidState().is(FluidTags.WATER)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasAdjacentAir(ServerLevel level, BlockPos pos) {
        for (Direction direction : DIRECTIONS) {
            if (level.getBlockState(pos.relative(direction)).isAir()) {
                return true;
            }
        }

        return false;
    }

    private static List<BlockPos> findEntryPoints(ServerLevel level, BlockPos breachPos) {
        List<BlockPos> entryPoints = new ArrayList<>();
        if (level.getBlockState(breachPos).isAir()) {
            entryPoints.add(breachPos);
        }

        for (Direction direction : DIRECTIONS) {
            BlockPos neighbor = breachPos.relative(direction);
            if (level.getBlockState(neighbor).isAir()) {
                entryPoints.add(neighbor);
            }
        }

        return entryPoints;
    }

    private static final class FloodTask {
        private final ServerLevel level;
        private final BlockPos breachPos;
        private final FloodKey key;
        private final Deque<BlockPos> frontier = new ArrayDeque<>();
        private final Set<BlockPos> visited = new HashSet<>();
        private final int batchSize;
        private int filledBlocks;

        private FloodTask(ServerLevel level, BlockPos breachPos, List<BlockPos> entryPoints, float pressure,
                FloodKey key) {
            this.level = level;
            this.breachPos = breachPos;
            this.key = key;
            this.batchSize = Mth.clamp(Math.round(Mth.lerp(pressure, MIN_FILL_PER_TICK, MAX_FILL_PER_TICK)),
                    MIN_FILL_PER_TICK, MAX_FILL_PER_TICK);

            for (BlockPos entryPoint : entryPoints) {
                if (this.visited.add(entryPoint)) {
                    this.frontier.addLast(entryPoint);
                }
            }
        }

        private boolean tick() {
            if (this.frontier.isEmpty() || this.filledBlocks >= MAX_VOLUME) {
                return true;
            }

            int filledThisTick = 0;
            while (!this.frontier.isEmpty() && filledThisTick < this.batchSize && this.filledBlocks < MAX_VOLUME) {
                BlockPos current = this.frontier.pollFirst();
                if (current == null) {
                    break;
                }

                if (!this.level.getBlockState(current).isAir()) {
                    continue;
                }

                this.level.setBlock(current, Blocks.WATER.defaultBlockState(), 3);
                this.filledBlocks++;
                filledThisTick++;

                for (Direction direction : DIRECTIONS) {
                    BlockPos neighbor = current.relative(direction);
                    if (this.visited.add(neighbor) && this.level.getBlockState(neighbor).isAir()) {
                        this.frontier.addLast(neighbor);
                    }
                }
            }

            if (filledThisTick > 0) {
                this.level.sendParticles(ParticleTypes.BUBBLE, this.breachPos.getX() + 0.5D,
                        this.breachPos.getY() + 0.5D, this.breachPos.getZ() + 0.5D, 8 + filledThisTick, 0.45D, 0.25D,
                        0.45D, 0.015D);
                this.level.sendParticles(ParticleTypes.SPLASH, this.breachPos.getX() + 0.5D,
                        this.breachPos.getY() + 0.5D, this.breachPos.getZ() + 0.5D, 4 + filledThisTick / 2, 0.4D,
                        0.2D, 0.4D, 0.03D);
                this.applySuction();
            }

            return this.frontier.isEmpty() || this.filledBlocks >= MAX_VOLUME;
        }

        private void applySuction() {
            Vec3 holeCenter = Vec3.atCenterOf(this.breachPos);
            AABB suctionBox = new AABB(this.breachPos).inflate(SUCTION_RADIUS, SUCTION_RADIUS, SUCTION_RADIUS);
            for (Player player : this.level.getEntitiesOfClass(Player.class, suctionBox, player -> player.isAlive()
                    && !player.isSpectator())) {
                Vec3 pullVector = holeCenter.subtract(player.getEyePosition());
                double distanceSqr = pullVector.lengthSqr();
                if (distanceSqr < 1.0E-6D || distanceSqr > SUCTION_RADIUS * SUCTION_RADIUS) {
                    continue;
                }

                double distance = Math.sqrt(distanceSqr);
                double pressure = PressureHelper.getPressure(this.level, this.breachPos);
                double strength = Mth.clamp(pressure * SUCTION_STRENGTH * (1.0D - distance / SUCTION_RADIUS), 0.0D,
                        SUCTION_STRENGTH);
                if (strength <= 0.0D) {
                    continue;
                }

                Vec3 pull = pullVector.normalize().scale(strength);
                player.push(pull.x, pull.y, pull.z);
            }
        }
    }

    private record FloodKey(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private static final class FloodSeed {
        private final FloodKey key;
        private final ServerLevel level;
        private final BlockPos pos;
        private final float pressure;
        private int attempts;

        private FloodSeed(FloodKey key, ServerLevel level, BlockPos pos, float pressure) {
            this.key = key;
            this.level = level;
            this.pos = pos;
            this.pressure = pressure;
        }
    }
}