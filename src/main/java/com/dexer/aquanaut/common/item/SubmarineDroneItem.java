package com.dexer.aquanaut.common.item;

import com.dexer.aquanaut.common.entity.SubmarineDroneEntity;
import com.dexer.aquanaut.core.EntityRegistry;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

/**
 * Deploys a {@link SubmarineDroneEntity}: right-click a block to set it down against that face, or
 * right-click open water to launch it into the water in front of you.
 *
 * <p>
 * Launching is the ordinary case — a drone is deployed from mid-water, where there is no surface to
 * click — so aiming puts the crosshair through the middle of the hull rather than at its skids.
 *
 * <p>
 * The drone comes back as this same item: hitting one picks the hull up and breaks the pilot's link,
 * so a drone that has been deployed is never lost, only moved — and it comes back wearing the tag it
 * had, so deploying it again keeps the name it was known by.
 */
public class SubmarineDroneItem extends AbstractSpawnerItem {

    public SubmarineDroneItem(Item.Properties properties) {
        // The stack carries the hull's tag, so two of them are never interchangeable.
        super(properties.stacksTo(1));
    }

    @Override
    protected EntityType<? extends Entity> entityType() {
        return EntityRegistry.SUBMARINE_DRONE.get();
    }

    /** Thrown into the water rather than dropped beside it: the deployment sound is a splash. */
    @Override
    protected SoundEvent spawnSound() {
        return SoundEvents.FISHING_BOBBER_SPLASH;
    }

    @Override
    protected SpawnAnchor aimedAnchor() {
        return SpawnAnchor.CENTER;
    }
}
