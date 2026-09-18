package com.dexer.aquanaut.common.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;

/**
 * Block entity for every cell of a dissection table.
 *
 * <p>
 * Rendering only needs the merge group, which the block entity recomputes from the level, so this
 * class stays deliberately thin. The specimen fields are the extension point for the dissection
 * gameplay that is intentionally not implemented yet: a captured specimen gets stored here, the
 * table runs a progress bar, and the result is handed back to the player.
 */
public class DissectionTableBlockEntity extends BlockEntity implements GeoBlockEntity {
    /** Serialised id of the specimen currently on the bench, or {@code ""} when empty. */
    private String specimenId = "";
    /** Client-facing dissection progress in ticks. */
    private int progress;
    private int progressTotal;

    public DissectionTableBlockEntity(BlockPos pos, BlockState state) {
        super(com.dexer.aquanaut.core.BlockEntityRegistry.DISSECTION_TABLE.get(), pos, state);
    }

    // -- deferred dissection gameplay -------------------------------------

    /** The specimen currently on the bench, if any. */
    public Optional<ResourceLocation> specimen() {
        if (this.specimenId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(this.specimenId));
    }

    public int progress() {
        return this.progress;
    }

    public float progressFraction() {
        return this.progressTotal <= 0 ? 0.0F : Math.min(1.0F, (float) this.progress / this.progressTotal);
    }

    /**
     * Places a specimen on the bench.
     *
     * <p>
     * TODO(dissection): only the origin of a merge group may hold a specimen, the specimen must fit
     * the bench footprint (1x1 friendly, 2x1 medium, 2x2 titan — derived from the aquarium
     * footprint), and inserting a specimen should start {@link #progress}.
     */
    public void setSpecimen(ResourceLocation id) {
        this.specimenId = id == null ? "" : id.toString();
        this.progress = 0;
        this.setChanged();
    }

    public void clearSpecimen() {
        this.specimenId = "";
        this.progress = 0;
        this.progressTotal = 0;
        this.setChanged();
    }

    // -- persistence ------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.specimenId.isBlank()) {
            tag.putString("Specimen", this.specimenId);
        }
        tag.putInt("Progress", this.progress);
        tag.putInt("ProgressTotal", this.progressTotal);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.specimenId = tag.getString("Specimen");
        this.progress = tag.getInt("Progress");
        this.progressTotal = tag.getInt("ProgressTotal");
    }

    // -- geckolib ---------------------------------------------------------

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // The table models are static; a controller is only needed so future work can animate them.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
