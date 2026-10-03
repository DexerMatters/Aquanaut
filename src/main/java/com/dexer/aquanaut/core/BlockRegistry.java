package com.dexer.aquanaut.core;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.block.AshLayerBlock;
import com.dexer.aquanaut.common.block.CrystalClusterBlock;
import com.dexer.aquanaut.common.block.CrystalColumnBlock;
import com.dexer.aquanaut.common.block.DissectionTableBlock;
import com.dexer.aquanaut.common.block.MudBlock;
import com.dexer.aquanaut.common.block.NutrientRichMudBlock;
import com.dexer.aquanaut.common.block.ParasiticMudBlock;
import com.dexer.aquanaut.common.block.ShellPileBlock;
import com.dexer.aquanaut.common.block.DroopingSeaweedBlock;
import com.dexer.aquanaut.common.block.FishingNetBlock;
import com.dexer.aquanaut.common.block.GasPipeBlock;
import com.dexer.aquanaut.common.block.FumaroleBlock;
import com.dexer.aquanaut.common.block.InvestigationBoardBlock;
import com.dexer.aquanaut.common.block.MatCarpetBlock;
import com.dexer.aquanaut.common.block.PlexiglassBlock;
import com.dexer.aquanaut.common.block.PhotoRinsingBasinBlock;
import com.dexer.aquanaut.common.block.DynamicLightBlock;
import com.dexer.aquanaut.common.block.CrystalPlantBlock;
import com.dexer.aquanaut.common.block.SeaweedBlock;
import com.dexer.aquanaut.common.block.SeaweedStemBlock;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nullable;
import java.util.function.Function;

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
    public static final DeferredBlock<MudBlock> MUD = BLOCKS.register("mud",
            () -> new MudBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BROWN).strength(0.7F, 0.9F)
                    .sound(SoundType.MUD)
                    .isSuffocating((state, level, pos) -> false)));
    public static final DeferredBlock<Block> MUD_BRICKS = cube("mud_bricks",
            MapColor.TERRACOTTA_BROWN, 1.5F, 3.0F, SoundType.STONE);
    public static final DeferredBlock<StairBlock> MUD_BRICK_STAIRS = BLOCKS.register("mud_brick_stairs",
            () -> new StairBlock(MUD_BRICKS.get().defaultBlockState(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BROWN).strength(1.5F, 3.0F).sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));
    public static final DeferredBlock<SlabBlock> MUD_BRICK_SLAB = BLOCKS.register("mud_brick_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BROWN).strength(1.5F, 3.0F).sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));
    public static final DeferredBlock<WallBlock> MUD_BRICK_WALL = BLOCKS.register("mud_brick_wall",
            () -> new WallBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BROWN).strength(1.5F, 3.0F).sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));
    public static final DeferredBlock<ParasiticMudBlock> PARASITIC_MUD = BLOCKS.register("parasitic_mud",
            () -> new ParasiticMudBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BROWN).strength(0.7F, 0.9F).sound(SoundType.MUD)));
    public static final DeferredBlock<Block> FOSSIL_BED = cube("fossil_bed",
            MapColor.TERRACOTTA_GRAY, 1.5F, 3.0F, SoundType.STONE);
    public static final DeferredBlock<ShellPileBlock> SHELL_PILE = BLOCKS.register("shell_pile",
            () -> new ShellPileBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND).strength(0.5F, 0.8F).sound(SoundType.CORAL_BLOCK)));
    public static final DeferredBlock<RotatedPillarBlock> SEDIMENT_COLUMN = pillar("sediment_column",
            MapColor.COLOR_GRAY, 1.8F, 3.0F, SoundType.STONE, true);
    public static final DeferredBlock<Block> FOSSIL_DISPLAY = cube("fossil_display",
            MapColor.TERRACOTTA_GRAY, 2.0F, 3.0F, SoundType.STONE);
    public static final DeferredBlock<ColoredFallingBlock> CORAL_SAND = BLOCKS.register("coral_sand",
            () -> new ColoredFallingBlock(new ColorRGBA(0xFFC6C0B7), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_LIGHT_GRAY)
                    .strength(0.6F, 0.8F)
                    .sound(SoundType.SAND)));
    public static final DeferredBlock<NutrientRichMudBlock> NUTRIENT_RICH_MUD = BLOCKS.register("nutrient_rich_mud",
            () -> new NutrientRichMudBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BROWN)
                    .strength(0.7F, 0.9F)
                    .sound(SoundType.MUD)));
    public static final DeferredBlock<DroopingSeaweedBlock> DROOPING_SEAWEED = seaweed("drooping_seaweed");
    public static final DeferredBlock<Block> SHALE = cube("shale",
            MapColor.COLOR_GRAY, 1.5F, 3.0F, SoundType.STONE);
    public static final DeferredBlock<Block> LIMESTONE = cube("limestone",
            MapColor.TERRACOTTA_WHITE, 1.75F, 3.5F, SoundType.STONE);

    // Seaweed block — dense opaque foliage like leaves
    public static final DeferredBlock<SeaweedBlock> SEAWEED = leafSeaweed("seaweed");
    public static final DeferredBlock<SeaweedBlock> SEAWEED_FRUIT = leafSeaweed("seaweed_fruit");
    public static final DeferredBlock<SeaweedStemBlock> SEAWEED_STEM = seaweedStem("seaweed_stem");

    // Brine Mirror Gorge - evaporite minerals and crystal flora
    public static final DeferredBlock<Block> HALITE_CRUST = cube("halite_crust",
            MapColor.TERRACOTTA_WHITE, 0.8F, 1.2F, SoundType.CALCITE);
    public static final DeferredBlock<RotatedPillarBlock> HALITE_PIPE = pillar("halite_pipe",
            MapColor.TERRACOTTA_PINK, 1.4F, 2.2F, SoundType.CALCITE, true);
    public static final DeferredBlock<Block> VARVE_SHALE = cube("varve_shale",
            MapColor.COLOR_GRAY, 1.5F, 3.0F, SoundType.STONE);
    public static final DeferredBlock<Block> BRINE_MIRROR = BLOCKS.register("brine_mirror",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(0.4F, 0.8F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .isViewBlocking((state, level, pos) -> false)));
    public static final DeferredBlock<CrystalPlantBlock> CALCITE_QUILL = crystalPlant("calcite_quill", 2);
    public static final DeferredBlock<CrystalPlantBlock> HALITE_ROSETTE = crystalPlant("halite_rosette", 0);
    public static final DeferredBlock<DroopingSeaweedBlock> SALT_FRINGE = saltFringe("salt_fringe");

    // Brine Mirror Gorge - evaporite enrichment: hopper halite, druse, gypsum and potash
    public static final DeferredBlock<Block> HOPPER_HALITE = cube("hopper_halite",
            MapColor.TERRACOTTA_PINK, 1.2F, 2.2F, SoundType.CALCITE);
    public static final DeferredBlock<Block> HALITE_DRUSE = cube("halite_druse",
            MapColor.TERRACOTTA_WHITE, 1.0F, 1.8F, SoundType.CALCITE);
    public static final DeferredBlock<RotatedPillarBlock> GYPSUM_BLADE = pillar("gypsum_blade",
            MapColor.TERRACOTTA_WHITE, 1.3F, 2.2F, SoundType.CALCITE, true);
    public static final DeferredBlock<Block> SYLVITE_CRUST = cube("sylvite_crust",
            MapColor.TERRACOTTA_PINK, 0.8F, 1.2F, SoundType.CALCITE);
    public static final DeferredBlock<MatCarpetBlock> MIRROR_FLAKE = mat("mirror_flake",
            MapColor.COLOR_BLACK, SoundType.GLASS);
    public static final DeferredBlock<CrystalPlantBlock> GYPSUM_ROSE = crystalPlant("gypsum_rose", 1,
            MapColor.TERRACOTTA_WHITE);

    // Brimstone Caldera - volcanic rocks, sulfur minerals and hot spring deposits
    public static final DeferredBlock<RotatedPillarBlock> VOLCANIC_BASALT = pillar("volcanic_basalt",
            MapColor.COLOR_GRAY, 2.5F, 5.0F, SoundType.BASALT, true);
    public static final DeferredBlock<Block> SCORIA = cube("scoria",
            MapColor.TERRACOTTA_BLACK, 1.8F, 3.5F, SoundType.BASALT);
    public static final DeferredBlock<Block> PILLOW_BASALT = cube("pillow_basalt",
            MapColor.COLOR_GRAY, 2.2F, 4.5F, SoundType.BASALT);
    public static final DeferredBlock<Block> VOLCANIC_AGGLOMERATE = cube("volcanic_agglomerate",
            MapColor.TERRACOTTA_GRAY, 1.6F, 3.5F, SoundType.STONE);
    public static final DeferredBlock<Block> PUMICE = cube("pumice",
            MapColor.TERRACOTTA_WHITE, 0.9F, 1.4F, SoundType.CALCITE);
    public static final DeferredBlock<Block> OBSIDIAN_GLASS = cube("obsidian_glass",
            MapColor.COLOR_BLACK, 2.8F, 6.0F, SoundType.GLASS);
    public static final DeferredBlock<Block> ACID_ETCHED_BASALT = cube("acid_etched_basalt",
            MapColor.TERRACOTTA_LIGHT_GRAY, 1.4F, 3.0F, SoundType.STONE);
    public static final DeferredBlock<Block> SULFUR_CRUST = cube("sulfur_crust",
            MapColor.COLOR_YELLOW, 0.7F, 1.1F, SoundType.CALCITE);
    public static final DeferredBlock<Block> SINTER = cube("sinter",
            MapColor.QUARTZ, 1.1F, 2.0F, SoundType.CALCITE);
    public static final DeferredBlock<RotatedPillarBlock> VENT_CHIMNEY = pillar("vent_chimney",
            MapColor.COLOR_BLACK, 1.6F, 3.5F, SoundType.BASALT, true);
    public static final DeferredBlock<ColoredFallingBlock> VOLCANIC_ASH = BLOCKS.register("volcanic_ash",
            () -> new ColoredFallingBlock(new ColorRGBA(0xFF636468), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5F, 0.7F)
                    .sound(SoundType.SAND)));
    public static final DeferredBlock<AshLayerBlock> ASH_LAYER = BLOCKS.register("ash_layer",
            () -> new AshLayerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.3F, 0.5F)
                    .sound(SoundType.SAND)
                    .noOcclusion()
                    .isViewBlocking((state, level, pos) -> false)));
    public static final DeferredBlock<SeaweedBlock> SULFUR_MOSS = leafSeaweed("sulfur_moss");

    // Brimstone Caldera - hydrothermal flora, vents and acid pools
    public static final DeferredBlock<CrystalPlantBlock> SULFUR_CRYSTAL = crystalPlant("sulfur_crystal", 2,
            MapColor.COLOR_YELLOW);
    public static final DeferredBlock<CrystalPlantBlock> FIREBLOOM = crystalPlant("firebloom", 5,
            MapColor.COLOR_ORANGE);
    public static final DeferredBlock<DroopingSeaweedBlock> SULFUR_STALACTITE = drooping("sulfur_stalactite",
            MapColor.COLOR_YELLOW, SoundType.CALCITE);
    public static final DeferredBlock<DroopingSeaweedBlock> EMBER_KELP = drooping("ember_kelp",
            MapColor.COLOR_ORANGE, SoundType.WET_GRASS);
    public static final DeferredBlock<FumaroleBlock> FUMAROLE = BLOCKS.register("fumarole",
            () -> new FumaroleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(1.2F, 2.5F)
                    .sound(SoundType.BASALT)
                    .noOcclusion()
                    .lightLevel(state -> 1)
                    .dynamicShape()));
    public static final DeferredBlock<MatCarpetBlock> THERMOPHILIC_MAT_GOLD = mat("thermophilic_mat_gold",
            MapColor.COLOR_YELLOW);
    public static final DeferredBlock<MatCarpetBlock> THERMOPHILIC_MAT_RUST = mat("thermophilic_mat_rust",
            MapColor.COLOR_ORANGE);
    public static final DeferredBlock<MatCarpetBlock> THERMOPHILIC_MAT_OLIVE = mat("thermophilic_mat_olive",
            MapColor.PLANT);

    // Crystal Nest (水晶巢) — the geode lattice of the middle sea: nest rock, chamber
    // linings, wall crystals in six orientations, and the algae that carpets every surface
    public static final DeferredBlock<Block> CRYSTAL_NEST_STONE = cube("crystal_nest_stone",
            MapColor.COLOR_CYAN, 2.5F, 4.5F, SoundType.AMETHYST);
    public static final DeferredBlock<Block> CRYSTAL_DRUSE = cube("crystal_druse",
            MapColor.COLOR_LIGHT_BLUE, 1.2F, 2.2F, SoundType.AMETHYST_CLUSTER);
    public static final DeferredBlock<RotatedPillarBlock> CRYSTAL_COLUMN = crystalPillar("crystal_column",
            MapColor.COLOR_LIGHT_GRAY, 1.8F, 3.5F, SoundType.AMETHYST, true);

    // Wall crystals — every kind may bloom inside the core chambers (the glowing pair
    // rarely); the open surface grows only the quiet trio: white, smoky and amethyst.
    // All of them root on 晶巢岩 and nothing else (see TagRegistry.CRYSTAL_GROWTH_SUPPORT).
    public static final DeferredBlock<CrystalClusterBlock> WHITE_CRYSTAL_CLUSTER = cluster(
            "white_crystal_cluster", 0, MapColor.SNOW);
    public static final DeferredBlock<CrystalClusterBlock> ROSE_CRYSTAL_CLUSTER = cluster(
            "rose_crystal_cluster", 0, MapColor.COLOR_PINK);
    public static final DeferredBlock<CrystalClusterBlock> AMETHYST_CRYSTAL_CLUSTER = cluster(
            "amethyst_crystal_cluster", 0, MapColor.COLOR_PURPLE);
    public static final DeferredBlock<CrystalClusterBlock> AQUA_CRYSTAL_CLUSTER = cluster(
            "aqua_crystal_cluster", 0, MapColor.COLOR_LIGHT_BLUE);
    public static final DeferredBlock<CrystalClusterBlock> SMOKY_CRYSTAL_CLUSTER = cluster(
            "smoky_crystal_cluster", 0, MapColor.TERRACOTTA_GRAY);
    public static final DeferredBlock<CrystalClusterBlock> RESONANT_CRYSTAL_CLUSTER = cluster(
            "resonant_crystal_cluster", 4, MapColor.COLOR_CYAN);
    public static final DeferredBlock<CrystalClusterBlock> LIFE_GEM_CLUSTER = cluster(
            "life_gem_cluster", 5, MapColor.COLOR_LIGHT_GREEN);

    /** One whole solid block of algae turf: the green carpet over every top surface. */
    public static final DeferredBlock<Block> ALGAE_MAT = BLOCKS.register("algae_mat",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(0.6F, 0.8F)
                    .sound(SoundType.WET_GRASS)));
    public static final DeferredBlock<CrystalPlantBlock> ALGAE_TUFT = crystalPlant("algae_tuft", 0,
            MapColor.COLOR_GREEN);
    public static final DeferredBlock<CrystalPlantBlock> CRYSTAL_SPROUT = crystalPlant("crystal_sprout", 0,
            MapColor.ICE, TagRegistry.CRYSTAL_GROWTH_SUPPORT);
    public static final DeferredBlock<DroopingSeaweedBlock> CRYSTAL_FRINGE = drooping("crystal_fringe",
            MapColor.ICE, SoundType.AMETHYST_CLUSTER, TagRegistry.CRYSTAL_GROWTH_SUPPORT);

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

    /** Server-owned, inaccessible light source shared by Aquanaut dynamic-light providers. */
    public static final DeferredBlock<DynamicLightBlock> DYNAMIC_LIGHT = BLOCKS.register("dynamic_light",
            () -> new DynamicLightBlock(BlockBehaviour.Properties.of()
                    .replaceable()
                    .noCollission()
                    .noOcclusion()
                    .noLootTable()
                    .randomTicks()
                    .strength(-1.0F, 3600000.8F)
                    .mapColor(MapColor.NONE)
                    .lightLevel(state -> state.getValue(DynamicLightBlock.LIGHT_LEVEL))
                    .isViewBlocking((state, level, pos) -> false)
                    .pushReaction(PushReaction.DESTROY)));

    /** Dissection table: a 1x1 bench on its own, merged into 2x1 / 2x2 benches by placement. */
    public static final DeferredBlock<DissectionTableBlock> DISSECTION_TABLE = BLOCKS.register("dissection_table",
            () -> new DissectionTableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.5F, 4.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    /** Shell-lined workstation that develops exposed film in an underwater chemical rinse. */
    public static final DeferredBlock<PhotoRinsingBasinBlock> PHOTO_RINSING_BASIN = BLOCKS.register(
            "photo_rinsing_basin", () -> new PhotoRinsingBasinBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.5F, 4.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    /** Wall-mounted two-by-one investigation board, assembled from four linked cells. */
    public static final DeferredBlock<InvestigationBoardBlock> INVESTIGATION_BOARD = BLOCKS.register(
            "investigation_board", () -> new InvestigationBoardBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.5F, 4.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .dynamicShape()
                    .requiresCorrectToolForDrops()));

    private BlockRegistry() {
    }

    private static DeferredBlock<RotatedPillarBlock> log(String name, MapColor color, float hardness,
            float resistance) {
        return pillar(name, color, hardness, resistance, SoundType.CORAL_BLOCK, false);
    }

    private static DeferredBlock<RotatedPillarBlock> pillar(String name, MapColor color, float hardness,
            float resistance, SoundType sound, boolean requiresTool) {
        return pillarLike(name, color, hardness, resistance, sound, requiresTool, RotatedPillarBlock::new);
    }

    private static DeferredBlock<RotatedPillarBlock> crystalPillar(String name, MapColor color, float hardness,
            float resistance, SoundType sound, boolean requiresTool) {
        return pillarLike(name, color, hardness, resistance, sound, requiresTool, CrystalColumnBlock::new);
    }

    private static DeferredBlock<RotatedPillarBlock> pillarLike(String name, MapColor color, float hardness,
            float resistance, SoundType sound, boolean requiresTool,
            Function<BlockBehaviour.Properties, RotatedPillarBlock> factory) {
        var base = BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(hardness, resistance)
                .sound(sound);
        if (requiresTool) base = base.requiresCorrectToolForDrops();
        BlockBehaviour.Properties props = base;
        return BLOCKS.register(name, () -> factory.apply(props));
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

    private static DeferredBlock<CrystalPlantBlock> crystalPlant(String name, int light) {
        return crystalPlant(name, light, MapColor.ICE);
    }

    private static DeferredBlock<CrystalClusterBlock> cluster(String name, int light, MapColor color) {
        return BLOCKS.register(name, () -> new CrystalClusterBlock(BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(0.4F)
                .sound(SoundType.AMETHYST_CLUSTER)
                .noOcclusion()
                .dynamicShape()
                .lightLevel(state -> light)));
    }

    private static DeferredBlock<CrystalPlantBlock> crystalPlant(String name, int light, MapColor color) {
        return crystalPlant(name, light, color, null);
    }

    private static DeferredBlock<CrystalPlantBlock> crystalPlant(String name, int light, MapColor color,
            @Nullable TagKey<Block> supportBelow) {
        return BLOCKS.register(name, () -> new CrystalPlantBlock(BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(0.2F)
                .sound(SoundType.CALCITE)
                .noOcclusion()
                .dynamicShape()
                .lightLevel(state -> light), supportBelow));
    }

    private static DeferredBlock<DroopingSeaweedBlock> drooping(String name, MapColor color, SoundType sound) {
        return drooping(name, color, sound, null);
    }

    private static DeferredBlock<DroopingSeaweedBlock> drooping(String name, MapColor color, SoundType sound,
            @Nullable TagKey<Block> anchorAbove) {
        return BLOCKS.register(name, () -> new DroopingSeaweedBlock(BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(0.3F)
                .sound(sound)
                .noOcclusion()
                .dynamicShape(), anchorAbove));
    }

    private static DeferredBlock<MatCarpetBlock> mat(String name, MapColor color) {
        return mat(name, color, SoundType.WET_GRASS);
    }

    private static DeferredBlock<MatCarpetBlock> mat(String name, MapColor color, SoundType sound) {
        return BLOCKS.register(name, () -> new MatCarpetBlock(BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(0.2F)
                .sound(sound)
                .noOcclusion()));
    }

    private static DeferredBlock<DroopingSeaweedBlock> saltFringe(String name) {
        return BLOCKS.register(name, () -> new DroopingSeaweedBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.TERRACOTTA_WHITE)
                .strength(0.3F)
                .sound(SoundType.CALCITE)
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
