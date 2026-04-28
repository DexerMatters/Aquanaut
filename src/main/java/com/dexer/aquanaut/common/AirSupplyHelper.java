package com.dexer.aquanaut.common;

import com.dexer.aquanaut.core.AttachmentRegistry;
import net.minecraft.world.entity.LivingEntity;

/**
 * Shared helpers for Aquanaut's unified air system.
 *
 * <p>
 * The vanilla air bar remains the single source of truth. Extra capacity is
 * stored as {@code MAX_EXTRA_AIR_SUPPLY}, and max air is computed as
 * {@code BASE_AIR_SUPPLY_TICKS + maxExtra}.
 */
public final class AirSupplyHelper {

    /** 60 seconds of base air at 20 ticks/second. */
    public static final int BASE_AIR_SUPPLY_TICKS = 20 * 60;

    private static final int REGEN_TIER_1 = 12;
    private static final int REGEN_TIER_2 = 16;
    private static final int REGEN_TIER_3 = 20;
    private static final int REGEN_TIER_4 = 24;

    private AirSupplyHelper() {
    }

    public static int sanitizeExtraCapacity(int value) {
        return Math.max(0, value);
    }

    public static int getMaxExtraAir(LivingEntity entity) {
        if (!entity.hasData(AttachmentRegistry.MAX_EXTRA_AIR_SUPPLY.get())) {
            return 0;
        }
        return sanitizeExtraCapacity(entity.getData(AttachmentRegistry.MAX_EXTRA_AIR_SUPPLY.get()));
    }

    public static void setMaxExtraAir(LivingEntity entity, int value) {
        int maxExtra = sanitizeExtraCapacity(value);
        entity.setData(AttachmentRegistry.MAX_EXTRA_AIR_SUPPLY.get(), maxExtra);

        int unifiedMax = BASE_AIR_SUPPLY_TICKS + maxExtra;
        if (entity.getAirSupply() > unifiedMax) {
            entity.setAirSupply(unifiedMax);
        }
    }

    public static int getUnifiedMaxAir(LivingEntity entity) {
        return BASE_AIR_SUPPLY_TICKS + getMaxExtraAir(entity);
    }

    public static void addAir(LivingEntity entity, int amount) {
        if (amount <= 0) {
            return;
        }
        int unifiedMax = getUnifiedMaxAir(entity);
        entity.setAirSupply(Math.min(entity.getAirSupply() + amount, unifiedMax));
    }

    /**
     * Regeneration speed scales by extra-capacity tiers.
     *
     * <p>
     * Capacity tiers are counted in multiples of BASE_AIR_SUPPLY_TICKS.
     */
    public static int getRegenPerTick(LivingEntity entity) {
        int maxExtra = getMaxExtraAir(entity);
        if (maxExtra <= 0) {
            return 0;
        }

        int tier = Math.max(1, (int) Math.ceil((double) maxExtra / BASE_AIR_SUPPLY_TICKS));
        return switch (Math.min(tier, 4)) {
            case 1 -> REGEN_TIER_1;
            case 2 -> REGEN_TIER_2;
            case 3 -> REGEN_TIER_3;
            default -> REGEN_TIER_4;
        };
    }
}