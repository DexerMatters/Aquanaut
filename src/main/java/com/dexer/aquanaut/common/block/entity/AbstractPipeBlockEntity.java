package com.dexer.aquanaut.common.block.entity;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Shared pipe block-entity state.
 *
 * <p>
 * The pipe's static shape still comes from blockstate connections. This entity
 * only carries dynamic transport state such as flow strength/direction for
 * client-side rendering and future gas-network logic.
 */
public abstract class AbstractPipeBlockEntity extends BlockEntity {
    private static final String FLOW_TAG = "Flow";
    private static final String FACE_FLOW_TAG_PREFIX = "FaceFlow";

    private final EnumMap<Direction, Integer> faceFlows = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, Integer> sampledFaceFlows = new EnumMap<>(Direction.class);

    private int flow;
    private long sampledTick = Long.MIN_VALUE;

    protected AbstractPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (Direction direction : Direction.values()) {
            faceFlows.put(direction, 0);
            sampledFaceFlows.put(direction, 0);
        }
    }

    public int getFlow() {
        return flow;
    }

    public void setFlow(int flow) {
        if (this.flow == flow) {
            return;
        }
        setFlowInternal(flow);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getFaceFlow(Direction direction) {
        return faceFlows.getOrDefault(direction, 0);
    }

    public Map<Direction, Integer> getFaceFlows() {
        return Map.copyOf(faceFlows);
    }

    public int getSampledFaceFlow(Direction direction) {
        return sampledFaceFlows.getOrDefault(direction, 0);
    }

    public void prepareFlowSnapshot(long gameTime) {
        if (sampledTick == gameTime) {
            return;
        }
        for (Direction direction : Direction.values()) {
            sampledFaceFlows.put(direction, faceFlows.getOrDefault(direction, 0));
        }
        sampledTick = gameTime;
    }

    public void setFaceFlows(Map<Direction, Integer> flows) {
        boolean changed = false;
        int totalFlow = 0;
        for (Direction direction : Direction.values()) {
            int amount = flows.getOrDefault(direction, 0);
            Integer previous = faceFlows.put(direction, amount);
            if (previous == null || previous.intValue() != amount) {
                changed = true;
            }
            if (amount > 0) {
                totalFlow += amount;
            }
        }
        if (!changed && flow == totalFlow) {
            return;
        }
        setFlowInternal(totalFlow);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    protected final void setFlowInternal(int flow) {
        this.flow = flow;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(FLOW_TAG, flow);
        for (Direction direction : Direction.values()) {
            tag.putInt(faceFlowTag(direction), faceFlows.getOrDefault(direction, 0));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        flow = tag.contains(FLOW_TAG) ? tag.getInt(FLOW_TAG) : 0;
        for (Direction direction : Direction.values()) {
            int amount = tag.contains(faceFlowTag(direction)) ? tag.getInt(faceFlowTag(direction)) : 0;
            faceFlows.put(direction, amount);
            sampledFaceFlows.put(direction, amount);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt(FLOW_TAG, flow);
        for (Direction direction : Direction.values()) {
            tag.putInt(faceFlowTag(direction), faceFlows.getOrDefault(direction, 0));
        }
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private static String faceFlowTag(Direction direction) {
        return FACE_FLOW_TAG_PREFIX + direction.getSerializedName().toUpperCase();
    }
}
