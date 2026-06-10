package com.dexer.aquanaut.common.block;

import com.dexer.aquanaut.common.inventory.aquarium.AquariumEntitySnapshot;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumInventoryHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Map;
import java.util.Set;

public final class FishingNetBlock extends AbstractPanelBlock {

    public static final MapCodec<FishingNetBlock> CODEC =
            simpleCodec(FishingNetBlock::new);

    public FishingNetBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<FishingNetBlock> codec() {
        return CODEC;
    }

    // ── Collection logic ───────────────────────────────────────────

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        Direction face = hit.getDirection();
        if (state.getValue(property(face))) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp && level instanceof ServerLevel sl) {
            if (collectStructure(sp, sl, pos, face)) {
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    public boolean collectStructure(ServerPlayer player, Level level, BlockPos pos,
            Direction face) {
        Set<BlockPos> component = getComponent(level, pos);
        Map<BlockPos, BlockState> raw = getRawStates(level, component);

        BlockPos interiorStart = pos.relative(face.getOpposite());
        if (isOwnBlock(level.getBlockState(interiorStart))
                || canReachOutsideFrom(level, interiorStart, raw)) {
            return false;
        }

        Set<BlockPos> interior = collectInterior(level, interiorStart, raw);
        for (BlockPos ip : interior) {
            AABB box = new AABB(ip);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e instanceof WaterAnimal)) {
                AquariumEntitySnapshot.snapshot(e).ifPresent(snapshot -> {
                    AquariumInventoryHelper.addFishEntry(player, snapshot);
                });
                e.discard();
            }
        }

        int count = 0;
        for (BlockPos p : component) {
            if (isOwnBlock(level.getBlockState(p))) {
                level.destroyBlock(p, true);
                count++;
            }
        }
        if (count > 0) {
            level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_LEATHER.value(),
                    SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        return true;
    }
}
