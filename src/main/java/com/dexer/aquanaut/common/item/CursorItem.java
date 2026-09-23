package com.dexer.aquanaut.common.item;

import com.dexer.aquanaut.common.entity.CursorEntity;
import com.dexer.aquanaut.core.EntityRegistry;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

/**
 * Places a {@link CursorEntity}: right-click a block to pin it against that face, or right-click
 * open water to drop it into the water in front of you.
 *
 * <p>
 * Either way the pin is tagged on the spot: a stack that came from a cursor puts that cursor's tag
 * back, and a fresh one is given a unique {@code #N} label before it appears.
 */
public class CursorItem extends AbstractSpawnerItem {

    public CursorItem(Item.Properties properties) {
        // The stack carries the pin's tag, so two of them are never interchangeable.
        super(properties.stacksTo(1));
    }

    @Override
    protected EntityType<? extends Entity> entityType() {
        return EntityRegistry.CURSOR.get();
    }

    /**
     * Aiming into water puts the crosshair through the middle of the cursor, which is what the
     * player is actually pointing at; anchoring at the feet would leave it floating a full body
     * height above the crosshair.
     */
    @Override
    protected SpawnAnchor aimedAnchor() {
        return SpawnAnchor.CENTER;
    }
}
