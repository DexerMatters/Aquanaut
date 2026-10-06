package com.dexer.aquanaut.common.mud;

import com.dexer.aquanaut.common.entity.HermitCrabEntity;
import com.dexer.aquanaut.core.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A test spawn egg that drops a hermit crab already wearing its largest shell, so the grown tiers
 * and the scaled model can be inspected without waiting for the ten-percent shell-swap roll.
 */
public final class LargeHermitCrabSpawnEggItem extends Item {
    public LargeHermitCrabSpawnEggItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        HermitCrabEntity crab = EntityRegistry.HERMIT_CRAB.get().create(server);
        if (crab == null) {
            return InteractionResult.FAIL;
        }
        crab.setShellSize(MudZoneConfig.SHELL_MAX_SIZE);
        crab.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                context.getPlayer() != null ? context.getPlayer().getYRot() : 0.0F, 0.0F);
        crab.finalizeSpawn(server, server.getCurrentDifficultyAt(pos), MobSpawnType.SPAWN_EGG, null);
        server.addFreshEntity(crab);
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
