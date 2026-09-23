package com.dexer.aquanaut.common.item;

import com.dexer.aquanaut.common.entity.BiologicalDetectorEntity;
import com.dexer.aquanaut.core.EntityRegistry;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

/**
 * Deploys a {@link BiologicalDetectorEntity}: right-click a block to set it down against that face,
 * or right-click open water to drop it into the water in front of you.
 *
 * <p>
 * Aiming into water is the ordinary case — a detector is deployed from mid-water, where there is
 * often no surface within reach — so the aim point lands in the middle of the ball rather than at
 * its base.
 *
 * <p>
 * The buoy comes back as this same item: a swing at a deployed detector picks it up, so one that has
 * been dropped is never lost, only moved. Detectors are not taggable — there is nothing to name on a
 * ball that is all sensor — so the stack that comes back is a plain one.
 */
public class BiologicalDetectorItem extends AbstractSpawnerItem {

    public BiologicalDetectorItem(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    protected EntityType<? extends Entity> entityType() {
        return EntityRegistry.BIOLOGICAL_DETECTOR.get();
    }

    /** It goes into the water rather than onto a block, so it lands with a splash. */
    @Override
    protected SoundEvent spawnSound() {
        return SoundEvents.FISHING_BOBBER_SPLASH;
    }

    @Override
    protected SpawnAnchor aimedAnchor() {
        return SpawnAnchor.CENTER;
    }
}
