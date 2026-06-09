package com.dexer.aquanaut.core;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.block.AirPumpBlock;
import com.dexer.aquanaut.common.block.AirSupplyBlock;
import com.dexer.aquanaut.common.block.BubbleMachineBlock;
import com.dexer.aquanaut.common.block.FishingNetBlock;
import com.dexer.aquanaut.common.block.GasPipeBlock;
import com.dexer.aquanaut.common.block.OmnidirectionalMachineBlock;
import com.dexer.aquanaut.common.block.ShieldGeneratorBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BlockRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Aquanaut.MODID);

    // Coral blocks — behave like logs (RotatedPillarBlock with axis property)
    public static final DeferredBlock<RotatedPillarBlock> RED_CORAL_BLOCK = log("red_coral_block",
            MapColor.COLOR_RED, 1.5F, 2.0F);
    public static final DeferredBlock<RotatedPillarBlock> BLUE_CORAL_BLOCK = log("blue_coral_block",
            MapColor.COLOR_BLUE, 1.5F, 2.0F);
    public static final DeferredBlock<RotatedPillarBlock> BLUE_SMOOTH_CORAL_BLOCK = log(
            "blue_smooth_coral_block",
            MapColor.COLOR_BLUE, 1.5F, 2.0F);
    public static final DeferredBlock<RotatedPillarBlock> BLUE_CORAL_BRICKS = log("blue_coral_bricks",
            MapColor.COLOR_BLUE, 2.0F, 3.0F);
    public static final DeferredBlock<RotatedPillarBlock> PURPLE_CORAL_BLOCK = log("purple_coral_block",
            MapColor.COLOR_PURPLE, 1.5F, 2.0F);
    public static final DeferredBlock<RotatedPillarBlock> GREEN_CORAL_BLOCK = log("green_coral_block",
            MapColor.COLOR_GREEN, 1.5F, 2.0F);
    public static final DeferredBlock<RotatedPillarBlock> FLUORASCENT_BLUE_CORAL_BLOCK = log(
            "fluorescent_blue_coral_block",
            MapColor.COLOR_LIGHT_BLUE, 1.5F, 2.0F);

    // Ringed coral blocks
    public static final DeferredBlock<RotatedPillarBlock> RINGED_BLUE_CORAL_BLOCK = log("ringed_blue_coral_block",
            MapColor.COLOR_BLUE, 1.5F, 2.0F);
    public static final DeferredBlock<RotatedPillarBlock> RINGED_GREEN_CORAL_BLOCK = log("ringed_green_coral_block",
            MapColor.COLOR_GREEN, 1.5F, 2.0F);
    public static final DeferredBlock<RotatedPillarBlock> RINGED_PURPLE_CORAL_BLOCK = log("ringed_purple_coral_block",
            MapColor.COLOR_PURPLE, 1.5F, 2.0F);
    public static final DeferredBlock<RotatedPillarBlock> RINGED_RED_CORAL_BLOCK = log("ringed_red_coral_block",
            MapColor.COLOR_RED, 1.5F, 2.0F);
    public static final DeferredBlock<RotatedPillarBlock> RINGED_FLUORASCENT_BLUE_CORAL_BLOCK = log(
            "ringed_fluorescent_blue_coral_block",
            MapColor.COLOR_LIGHT_BLUE, 1.5F, 2.0F);
    public static final DeferredBlock<RotatedPillarBlock> SHELL_BLOCK = pillar("shell_block",
            MapColor.TERRACOTTA_LIGHT_GRAY, 2.0F, 3.0F, SoundType.STONE);
    public static final DeferredBlock<RotatedPillarBlock> SHELL_BRICKS = pillar("shell_bricks",
            MapColor.TERRACOTTA_LIGHT_GRAY, 2.25F, 3.5F, SoundType.STONE);
    public static final DeferredBlock<RotatedPillarBlock> HARD_SHELL_BLOCK = pillar("hard_shell_block",
            MapColor.STONE, 3.0F, 4.5F, SoundType.STONE);
    public static final DeferredBlock<RotatedPillarBlock> HARD_SHELL_BRICKS = pillar("hard_shell_bricks",
            MapColor.STONE, 3.25F, 4.75F, SoundType.STONE);
    public static final DeferredBlock<Block> POLISHED_HARD_SHELL_BLOCK = cube("polished_hard_shell_block",
            MapColor.QUARTZ, 3.5F, 5.0F, SoundType.STONE);
    public static final DeferredBlock<Block> HARD_SHELL_FRAME = cube("hard_shell_frame",
            MapColor.QUARTZ, 3.5F, 5.0F, SoundType.STONE);
    public static final DeferredBlock<GasPipeBlock> GAS_PIPE = pipe("gas_pipe");

    public static final DeferredBlock<FishingNetBlock> FISHING_NET = fishingNet("fishing_net");

    public static final DeferredBlock<OmnidirectionalMachineBlock> LIGHTNING_GENERATOR = machine(
            "lightning_generator");
    public static final DeferredBlock<BubbleMachineBlock> BUBBLE_MACHINE = bubbleMachine("bubble_machine");
    public static final DeferredBlock<OmnidirectionalMachineBlock> SWIRL_GENERATOR = machine("swirl_generator");
    public static final DeferredBlock<OmnidirectionalMachineBlock> TORPEDO_LAUNCHER = machine("torpedo_launcher");
    public static final DeferredBlock<AirPumpBlock> AIR_PUMP = airPump("air_pump");
    public static final DeferredBlock<ShieldGeneratorBlock> SHIELD_GENERATOR = shieldGenerator("shield_generator");
    public static final DeferredBlock<AirSupplyBlock> AIR_SUPPLY = airSupply("air_supply");

    private BlockRegistry() {
    }

    private static DeferredBlock<RotatedPillarBlock> log(String name, MapColor color, float hardness,
            float resistance) {
        return pillar(name, color, hardness, resistance, SoundType.CORAL_BLOCK);
    }

    private static DeferredBlock<RotatedPillarBlock> pillar(String name, MapColor color, float hardness,
            float resistance, SoundType sound) {
        return BLOCKS.register(name, () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                .mapColor(state -> state.getValue(RotatedPillarBlock.AXIS) == net.minecraft.core.Direction.Axis.Y
                        ? color
                        : color)
                .strength(hardness, resistance)
                .sound(sound)
                .requiresCorrectToolForDrops()));
    }

    private static DeferredBlock<Block> cube(String name, MapColor color, float hardness, float resistance,
            SoundType sound) {
        return BLOCKS.register(name, () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(hardness, resistance)
                .sound(sound)
                .requiresCorrectToolForDrops()));
    }

    private static DeferredBlock<OmnidirectionalMachineBlock> machine(String name) {
        return BLOCKS.register(name, () -> new OmnidirectionalMachineBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.QUARTZ)
                .strength(3.5F, 5.0F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops()));
    }

    private static DeferredBlock<BubbleMachineBlock> bubbleMachine(String name) {
        return BLOCKS.register(name, () -> new BubbleMachineBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.QUARTZ)
                .strength(3.5F, 5.0F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops()));
    }

    private static DeferredBlock<AirPumpBlock> airPump(String name) {
        return BLOCKS.register(name, () -> new AirPumpBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.QUARTZ)
                .strength(3.5F, 5.0F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops()));
    }

    private static DeferredBlock<AirSupplyBlock> airSupply(String name) {
        return BLOCKS.register(name, () -> new AirSupplyBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.QUARTZ)
                .strength(3.5F, 5.0F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops()));
    }

    private static DeferredBlock<ShieldGeneratorBlock> shieldGenerator(String name) {
        return BLOCKS.register(name, () -> new ShieldGeneratorBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.QUARTZ)
                .strength(3.5F, 5.0F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops()));
    }

    private static DeferredBlock<GasPipeBlock> pipe(String name) {
        return BLOCKS.register(name, () -> new GasPipeBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_GRAY)
                .strength(2.5F, 4.0F)
                .sound(SoundType.METAL)
                .noOcclusion()
                .dynamicShape()
                .requiresCorrectToolForDrops()));
    }

    private static DeferredBlock<FishingNetBlock> fishingNet(String name) {
        return BLOCKS.register(name, () -> new FishingNetBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BROWN)
                .strength(1.5F, 2.0F)
                .sound(SoundType.WOOL)
                .noOcclusion()
                .dynamicShape()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
