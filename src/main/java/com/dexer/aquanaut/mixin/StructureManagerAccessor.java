package com.dexer.aquanaut.mixin;

import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.WorldOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The structure manager's world options, so worldgen callbacks can read the level seed without
 * reaching back through the server. The worldgen-scoped manager carries the seed of its world.
 */
@Mixin(StructureManager.class)
public interface StructureManagerAccessor {
    @Accessor("worldOptions")
    WorldOptions aquanaut$getWorldOptions();
}
