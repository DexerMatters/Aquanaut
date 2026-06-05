package com.dexer.aquanaut.common.entity.serpentine;

import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.entity.PartEntity;

import java.util.List;

public abstract class AbstractSerpentineEntity extends Mob {

    private static final EntityDataAccessor<CompoundTag> SEGMENT_TRANSFORMS = net.minecraft.network.syncher.SynchedEntityData
            .defineId(AbstractSerpentineEntity.class, EntityDataSerializers.COMPOUND_TAG);

    private final SerpentineSegment[] segments;
    private final SegmentChainAlgorithm chainAlgorithm;

    protected AbstractSerpentineEntity(EntityType<? extends AbstractSerpentineEntity> type, Level level) {
        super(type, level);
        List<SegmentDefinition> defs = createSegmentDefinitions();
        this.segments = new SerpentineSegment[defs.size()];
        for (int i = 0; i < defs.size(); i++) {
            this.segments[i] = new SerpentineSegment(this, defs.get(i), i);
        }
        this.chainAlgorithm = createChainAlgorithm();
    }

    protected abstract List<SegmentDefinition> createSegmentDefinitions();

    protected abstract SegmentChainAlgorithm createChainAlgorithm();

    protected void driveHead(ServerLevel level) {
    }

    protected void afterSegmentsUpdated(ServerLevel level) {
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return segments;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        AABB bounds = this.getBoundingBox();
        for (SerpentineSegment segment : segments) {
            bounds = bounds.minmax(segment.getBoundingBox());
        }
        return bounds;
    }

    @Override
    public void tick() {
        super.tick();

        this.yBodyRotO = this.yBodyRot;
        this.yHeadRotO = this.yHeadRot;

        for (SerpentineSegment seg : segments) {
            seg.setOldPosAndRot();
            seg.rollO = seg.getRoll();
            seg.tickCount++;
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            driveHead(serverLevel);
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();
            chainAlgorithm.updateSegments(this, segments);
            afterSegmentsUpdated(serverLevel);
            this.getEntityData().set(SEGMENT_TRANSFORMS, packSegmentTransforms());
        } else {
            applySegmentTransforms(this.getEntityData().get(SEGMENT_TRANSFORMS));
        }
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        double centerY = this.getY() + this.getBbHeight() * 0.5D;
        for (int i = 0; i < segments.length; i++) {
            segments[i].setId(packet.getId() + i + 1);
            segments[i].setCenterPos(this.getX(), centerY, this.getZ());
            segments[i].setYRot(this.getYRot());
            segments[i].setXRot(this.getXRot());
            segments[i].setOldPosAndRot();
            segments[i].rollO = 0.0F;
        }
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SEGMENT_TRANSFORMS, new CompoundTag());
    }

    public SerpentineSegment[] getSegments() {
        return segments;
    }

    private CompoundTag packSegmentTransforms() {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();

        for (SerpentineSegment segment : segments) {
            CompoundTag tag = new CompoundTag();
            tag.putDouble("x", segment.getCenterX());
            tag.putDouble("y", segment.getCenterY());
            tag.putDouble("z", segment.getCenterZ());
            tag.putFloat("yaw", segment.getYRot());
            tag.putFloat("pitch", segment.getXRot());
            tag.putFloat("roll", segment.getRoll());
            list.add(tag);
        }

        root.put("segments", list);
        return root;
    }

    private void applySegmentTransforms(CompoundTag root) {
        if (root.isEmpty() || !root.contains("segments", Tag.TAG_LIST)) {
            return;
        }

        ListTag list = root.getList("segments", Tag.TAG_COMPOUND);
        int count = Math.min(list.size(), segments.length);

        for (int i = 0; i < count; i++) {
            CompoundTag tag = list.getCompound(i);
            SerpentineSegment segment = segments[i];
            segment.setCenterPos(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"));
            segment.setYRot(tag.getFloat("yaw"));
            segment.setXRot(tag.getFloat("pitch"));
            segment.setRoll(tag.getFloat("roll"));
        }
    }
}
