package com.dexer.aquanaut.core;

import com.dexer.aquanaut.Aquanaut;
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

    private BlockRegistry() {
    }

    private static DeferredBlock<RotatedPillarBlock> log(String name, MapColor color, float hardness,
            float resistance) {
        return BLOCKS.register(name, () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                .mapColor(state -> state.getValue(RotatedPillarBlock.AXIS) == net.minecraft.core.Direction.Axis.Y
                        ? color
                        : color)
                .strength(hardness, resistance)
                .sound(SoundType.CORAL_BLOCK)
                .requiresCorrectToolForDrops()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
