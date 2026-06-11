package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.common.inventory.aquarium.AquariumFish;
import com.dexer.aquanaut.common.notebook.NotebookSpecies;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

public final class NotebookPreviewRenderer {

    private static final float TARGET_FILL_RATIO = 0.60F;
    private static final float FIT_MARGIN = 0.92F;
    private static final int MIN_SCALE = 10;
    private static final int MAX_SCALE = 256;
    private static final Map<ResourceLocation, LivingEntity> ENTITY_CACHE = new HashMap<>();

    private NotebookPreviewRenderer() {
    }

    public static void renderSpeciesPreview(GuiGraphics graphics, int left, int top, int width, int height,
            NotebookSpecies species) {
        LivingEntity entity = previewEntity(species);
        if (entity == null) {
            return;
        }

        entity.tickCount = (int) (System.currentTimeMillis() / 50L);

        int x1 = left;
        int x2 = left + width;
        int y1 = top;
        int y2 = top + height;
        int scale = fittedScale(width, height, entity);

        AquariumPreviewRenderer.renderLivingEntityPreview(graphics, x1, y1, x2, y2, scale, 0.0F, -2.0F, 0.0F,
                entity);
    }

    private static int fittedScale(int width, int height, LivingEntity entity) {
        float targetWidth = width * TARGET_FILL_RATIO;
        float targetHeight = height * TARGET_FILL_RATIO;
        float modelLength = Math.max(0.25F, modelLength(entity));
        float modelHeight = Math.max(0.25F, modelHeight(entity));
        float fitScale = Math.min(targetWidth / modelLength, targetHeight / modelHeight);
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, Math.round(fitScale * FIT_MARGIN)));
    }

    private static float modelLength(LivingEntity entity) {
        if (entity instanceof AquariumFish fish) {
            return fish.getAquariumModelLength();
        }
        return entity.getBbWidth();
    }

    private static float modelHeight(LivingEntity entity) {
        if (entity instanceof AquariumFish fish) {
            return fish.getAquariumModelHeight();
        }
        return entity.getBbHeight();
    }

    public static LivingEntity previewEntity(NotebookSpecies species) {
        ResourceLocation entityId = species.entityId();
        LivingEntity cached = ENTITY_CACHE.get(entityId);
        if (cached != null) {
            return cached;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
        if (type == null) {
            return null;
        }

        if (!(type.create(minecraft.level) instanceof LivingEntity living)) {
            return null;
        }

        ENTITY_CACHE.put(entityId, living);
        return living;
    }

    public static void clearCache() {
        ENTITY_CACHE.clear();
    }
}
