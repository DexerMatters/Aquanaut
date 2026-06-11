package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.common.inventory.aquarium.AquariumEntitySnapshot;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumFish;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumFishEntry;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumFishSpec;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumHealthTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

public final class AquariumPreviewRenderer {

    public static final int VERTICAL_OVERFLOW = 9;
    private static final int SLOT_SIZE = 18;

    private static final Map<String, LivingEntity> ENTITY_CACHE = new HashMap<>();

    private AquariumPreviewRenderer() {
    }

    public static void renderFish(GuiGraphics graphics, int gridX, int gridY, AquariumFishEntry entry, AquariumFishSpec spec) {
        LivingEntity entity = getOrCreatePreviewEntity(entry, spec);
        if (entity == null) {
            return;
        }

        if (entry != null) {
            if (entry.health() > 0.0F && entry.health() != entity.getHealth()) {
                entity.setHealth(Math.min(entry.health(), entity.getMaxHealth()));
            }
            if (AquariumHealthTracker.isRecentlyHurt(entry)) {
                entity.hurtTime = entity.hurtDuration = 10;
            } else {
                entity.hurtTime = entity.hurtDuration = 0;
            }
        }

        entity.tickCount = (int) (System.currentTimeMillis() / 50);

        int availableWidth = spec.gridWidth() * SLOT_SIZE;
        int availableHeight = spec.gridHeight() * SLOT_SIZE + 2 * VERTICAL_OVERFLOW;

        float modelLength = getModelLength(entity);
        float modelHeight = getModelHeight(entity);

        float fitScale = Math.min(
                modelLength > 0 ? availableWidth / modelLength : availableWidth,
                modelHeight > 0 ? availableHeight / modelHeight : availableHeight);
        int baseScale = 6 + Math.max(spec.gridWidth(), spec.gridHeight()) * 2;
        int scale = Math.round(baseScale * (fitScale / 18.0F));

        int centerX = gridX + availableWidth / 2;
        int x1 = centerX - 500;
        int x2 = centerX + 500;
        int y1 = gridY - VERTICAL_OVERFLOW;
        int y2 = gridY + spec.gridHeight() * SLOT_SIZE + VERTICAL_OVERFLOW;

        renderLivingEntityPreview(graphics, x1, y1, x2, y2, scale, 0.0F, -2F, 0.0F, entity);
    }

    public static void renderLivingEntityPreview(GuiGraphics graphics, int x1, int y1, int x2, int y2, int scale,
            float xRot, float yRot, float zRot, LivingEntity entity) {
        InventoryScreen.renderEntityInInventoryFollowsAngle(
                graphics,
                x1, y1, x2, y2,
                scale,
                xRot,
                yRot,
                zRot,
                entity);
    }

    private static float getModelLength(LivingEntity entity) {
        if (entity instanceof AquariumFish fish) {
            return fish.getAquariumModelLength();
        }
        return entity.getBbWidth();
    }

    private static float getModelHeight(LivingEntity entity) {
        if (entity instanceof AquariumFish fish) {
            return fish.getAquariumModelHeight();
        }
        return entity.getBbHeight();
    }

    public static LivingEntity getOrCreatePreviewEntity(AquariumFishEntry entry, AquariumFishSpec spec) {
        String cacheKey = entry == null ? spec.id().toString() : entry.cacheKey();
        LivingEntity cached = ENTITY_CACHE.get(cacheKey);
        if (cached != null) {
            if (entry != null && entry.health() > 0.0F && entry.health() != cached.getHealth()) {
                cached.setHealth(Math.min(entry.health(), cached.getMaxHealth()));
            }
            return cached;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }

        LivingEntity living = AquariumEntitySnapshot.createLivingEntity(mc.level,
                entry == null ? AquariumFishEntry.EMPTY : entry).orElse(null);
        if (living == null && spec.entityType().create(mc.level) instanceof LivingEntity created) {
            living = created;
        }
        if (living != null) {
            ENTITY_CACHE.put(cacheKey, living);
        }
        return living;
    }
}
