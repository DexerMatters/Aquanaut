package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.entity.serpentine.SegmentDefinition;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

public class LightningBranchSegment extends PartEntity<AbstractBranchingLightningEntity> {
    private final SegmentDefinition definition;
    private final int index;
    private final EntityDimensions dimensions;
    private LightningBoltGeometry.Segment localSegment;

    public LightningBranchSegment(AbstractBranchingLightningEntity parent, SegmentDefinition definition, int index) {
        super(parent);
        this.definition = definition;
        this.index = index;
        this.dimensions = EntityDimensions.scalable(definition.width, definition.height);
        this.refreshDimensions();
    }

    @Override
    public boolean is(net.minecraft.world.entity.Entity entity) {
        return this == entity || this.getParent() == entity;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return dimensions;
    }

    @Override
    public boolean isPickable() {
        return definition.canBeHit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!definition.canBeHit) {
            return false;
        }
        AbstractBranchingLightningEntity parent = getParent();
        return !parent.isInvulnerableTo(source) && parent.hurt(source, amount);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(
            net.minecraft.server.level.ServerEntity serverEntity) {
        throw new UnsupportedOperationException("LightningBranchSegment is a PartEntity");
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        /* no-op */
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        /* no-op */
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        /* no-op */
    }

    public SegmentDefinition getDefinition() {
        return definition;
    }

    public int getSegmentIndex() {
        return index;
    }

    public LightningBoltGeometry.Segment getLocalSegment() {
        return localSegment;
    }

    public void setLocalSegment(LightningBoltGeometry.Segment localSegment) {
        this.localSegment = localSegment;
    }

    public double getCenterX() {
        return getX();
    }

    public double getCenterY() {
        return getY() + getBbHeight() * 0.5D;
    }

    public double getCenterZ() {
        return getZ();
    }

    public Vec3 getCenterPos() {
        return new Vec3(getCenterX(), getCenterY(), getCenterZ());
    }

    public void setCenterPos(double x, double y, double z) {
        setPos(x, y - getBbHeight() * 0.5D, z);
    }
}
