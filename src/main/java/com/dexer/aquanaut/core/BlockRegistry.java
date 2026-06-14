package com.dexer.aquanaut.core;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.block.AirPumpBlock;
import com.dexer.aquanaut.common.block.AirSupplyBlock;
import com.dexer.aquanaut.common.block.BubbleMachineBlock;
import com.dexer.aquanaut.common.block.DroopingSeaweedBlock;
import com.dexer.aquanaut.common.block.FishingNetBlock;
import com.dexer.aquanaut.common.block.GasPipeBlock;
import com.dexer.aquanaut.common.block.OmnidirectionalMachineBlock;
import com.dexer.aquanaut.common.block.PlexiglassBlock;
import com.dexer.aquanaut.common.block.ShieldGeneratorBlock;
import com.dexer.aquanaut.common.block.SeaweedBlock;
import com.dexer.aquanaut.common.block.SeaweedStemBlock;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SlimeBlock;
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
            MapColor.TERRACOTTA_LIGHT_GRAY, 2.0F, 3.0F, SoundType.STONE, true);
    public static final DeferredBlock<RotatedPillarBlock> SHELL_BRICKS = pillar("shell_bricks",
            MapColor.TERRACOTTA_LIGHT_GRAY, 2.25F, 3.5F, SoundType.STONE, true);
    public static final DeferredBlock<RotatedPillarBlock> HARD_SHELL_BLOCK = pillar("hard_shell_block",
            MapColor.STONE, 3.0F, 4.5F, SoundType.STONE, true);
    public static final DeferredBlock<RotatedPillarBlock> HARD_SHELL_BRICKS = pillar("hard_shell_bricks",
            MapColor.STONE, 3.25F, 4.75F, SoundType.STONE, true);
    public static final DeferredBlock<Block> POLISHED_HARD_SHELL_BLOCK = cube("polished_hard_shell_block",
            MapColor.QUARTZ, 3.5F, 5.0F, SoundType.STONE);
    public static final DeferredBlock<Block> HARD_SHELL_FRAME = cube("hard_shell_frame",
            MapColor.QUARTZ, 3.5F, 5.0F, SoundType.STONE);

    // Natural sediment / stone blocks
    public static final DeferredBlock<ColoredFallingBlock> CORAL_SAND = BLOCKS.register("coral_sand",
            () -> new ColoredFallingBlock(new ColorRGBA(0xFFC6C0B7), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_LIGHT_GRAY)
                    .strength(0.6F, 0.8F)
                    .sound(SoundType.SAND)));
    public static final DeferredBlock<Block> NUTRIENT_RICH_MUD = cube("nutrient_rich_mud",
            MapColor.TERRACOTTA_BROWN, 0.7F, 0.9F, SoundType.MUD);
    public static final DeferredBlock<DroopingSeaweedBlock> DROOPING_SEAWEED = seaweed("drooping_seaweed");
    public static final DeferredBlock<Block> SHALE = cube("shale",
            MapColor.COLOR_GRAY, 1.5F, 3.0F, SoundType.STONE);
    public static final DeferredBlock<Block> LIMESTONE = cube("limestone",
            MapColor.TERRACOTTA_WHITE, 1.75F, 3.5F, SoundType.STONE);

    // Seaweed block — dense opaque foliage like leaves
    public static final DeferredBlock<SeaweedBlock> SEAWEED = leafSeaweed("seaweed");
    public static final DeferredBlock<SeaweedBlock> SEAWEED_FRUIT = leafSeaweed("seaweed_fruit");
    public static final DeferredBlock<SeaweedStemBlock> SEAWEED_STEM = seaweedStem("seaweed_stem");

    // Jelly blocks — translucent, bouncy like slime, easier to destroy
    public static final DeferredBlock<SlimeBlock> LIGHT_RED_JELLY_BLOCK = jelly("light_red_jelly_block",
            MapColor.COLOR_RED, 0.3F, 0.3F);
    public static final DeferredBlock<SlimeBlock> LIGHT_CYAN_JELLY_BLOCK = jelly("light_cyan_jelly_block",
            MapColor.COLOR_CYAN, 0.3F, 0.3F);
    public static final DeferredBlock<SlimeBlock> WHITE_JELLY_BLOCK = jelly("white_jelly_block",
            MapColor.SNOW, 0.3F, 0.3F);
    public static final DeferredBlock<SlimeBlock> LIGHT_GOLDEN_JELLY_BLOCK = jelly("light_golden_jelly_block",
            MapColor.COLOR_YELLOW, 0.3F, 0.3F);

    // Seaweed-wrapped jelly variants
    public static final DeferredBlock<SlimeBlock> LIGHT_RED_JELLY_BLOCK_SEAWEED = jelly("light_red_jelly_block_seaweed",
            MapColor.COLOR_RED, 0.3F, 0.3F);
    public static final DeferredBlock<SlimeBlock> LIGHT_CYAN_JELLY_BLOCK_SEAWEED = jelly("light_cyan_jelly_block_seaweed",
            MapColor.COLOR_CYAN, 0.3F, 0.3F);
    public static final DeferredBlock<SlimeBlock> WHITE_JELLY_BLOCK_SEAWEED = jelly("white_jelly_block_seaweed",
            MapColor.SNOW, 0.3F, 0.3F);
    public static final DeferredBlock<SlimeBlock> LIGHT_GOLDEN_JELLY_BLOCK_SEAWEED = jelly("light_golden_jelly_block_seaweed",
            MapColor.COLOR_YELLOW, 0.3F, 0.3F);

    // Vanilla coral slabs
    public static final DeferredBlock<SlabBlock> TUBE_CORAL_SLAB = slab("tube_coral_slab",
            MapColor.COLOR_BLUE, 1.5F, 1.5F);
    public static final DeferredBlock<SlabBlock> BRAIN_CORAL_SLAB = slab("brain_coral_slab",
            MapColor.COLOR_PINK, 1.5F, 1.5F);
    public static final DeferredBlock<SlabBlock> BUBBLE_CORAL_SLAB = slab("bubble_coral_slab",
            MapColor.COLOR_PURPLE, 1.5F, 1.5F);
    public static final DeferredBlock<SlabBlock> FIRE_CORAL_SLAB = slab("fire_coral_slab",
            MapColor.COLOR_RED, 1.5F, 1.5F);
    public static final DeferredBlock<SlabBlock> HORN_CORAL_SLAB = slab("horn_coral_slab",
            MapColor.COLOR_YELLOW, 1.5F, 1.5F);
    public static final DeferredBlock<SlabBlock> DEAD_TUBE_CORAL_SLAB = slab("dead_tube_coral_slab",
            MapColor.COLOR_GRAY, 1.5F, 1.5F);
    public static final DeferredBlock<SlabBlock> DEAD_BRAIN_CORAL_SLAB = slab("dead_brain_coral_slab",
            MapColor.COLOR_GRAY, 1.5F, 1.5F);
    public static final DeferredBlock<SlabBlock> DEAD_BUBBLE_CORAL_SLAB = slab("dead_bubble_coral_slab",
            MapColor.COLOR_GRAY, 1.5F, 1.5F);
    public static final DeferredBlock<SlabBlock> DEAD_FIRE_CORAL_SLAB = slab("dead_fire_coral_slab",
            MapColor.COLOR_GRAY, 1.5F, 1.5F);
    public static final DeferredBlock<SlabBlock> DEAD_HORN_CORAL_SLAB = slab("dead_horn_coral_slab",
            MapColor.COLOR_GRAY, 1.5F, 1.5F);

    public static final DeferredBlock<GasPipeBlock> GAS_PIPE = pipe("gas_pipe");

    public static final DeferredBlock<FishingNetBlock> FISHING_NET = fishingNet("fishing_net");
    public static final DeferredBlock<PlexiglassBlock> PLEXIGLASS = plexiglass("plexiglass");

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
        return pillar(name, color, hardness, resistance, SoundType.CORAL_BLOCK, false);
    }

    private static DeferredBlock<RotatedPillarBlock> pillar(String name, MapColor color, float hardness,
            float resistance, SoundType sound, boolean requiresTool) {
        var base = BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(hardness, resistance)
                .sound(sound);
        if (requiresTool) base = base.requiresCorrectToolForDrops();
        BlockBehaviour.Properties props = base;
        return BLOCKS.register(name, () -> new RotatedPillarBlock(props));
    }

    private static DeferredBlock<Block> cube(String name, MapColor color, float hardness, float resistance,
            SoundType sound) {
        return BLOCKS.register(name, () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(hardness, resistance)
                .sound(sound)
                .requiresCorrectToolForDrops()));
    }

    private static DeferredBlock<SlabBlock> slab(String name, MapColor color, float hardness,
            float resistance) {
        return BLOCKS.register(name, () -> new SlabBlock(BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(hardness, resistance)
                .sound(SoundType.CORAL_BLOCK)
                .requiresCorrectToolForDrops()));
    }

    private static DeferredBlock<SeaweedBlock> leafSeaweed(String name) {
        return BLOCKS.register(name, () -> new SeaweedBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GREEN)
                .strength(0.2F)
                .sound(SoundType.GRASS)
                .noOcclusion()
                .dynamicShape()));
    }

    private static DeferredBlock<SeaweedStemBlock> seaweedStem(String name) {
        return BLOCKS.register(name, () -> new SeaweedStemBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GREEN)
                .strength(0.3F)
                .sound(SoundType.GRASS)
                .noOcclusion()
                .dynamicShape()));
    }

    private static DeferredBlock<DroopingSeaweedBlock> seaweed(String name) {
        return BLOCKS.register(name, () -> new DroopingSeaweedBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GREEN)
                .strength(0.2F)
                .sound(SoundType.GRASS)
                .noOcclusion()
                .dynamicShape()));
    }

    private static DeferredBlock<SlimeBlock> jelly(String name, MapColor color, float hardness,
            float resistance) {
        return BLOCKS.register(name, () -> new SlimeBlock(BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(hardness, resistance)
                .sound(SoundType.SLIME_BLOCK)
                .friction(0.8F)
                .noOcclusion()
                .isViewBlocking((state, level, pos) -> false)));
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
                .strength(0.3F)
                .sound(SoundType.WOOL)
                .noOcclusion()
                .dynamicShape()));
    }

    private static DeferredBlock<PlexiglassBlock> plexiglass(String name) {
        return BLOCKS.register(name, () -> new PlexiglassBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.ICE)
                .strength(0.4F)
                .sound(SoundType.GLASS)
                .noOcclusion()
                .dynamicShape()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
