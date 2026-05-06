package com.dexer.aquanaut.common;

import com.dexer.aquanaut.common.diving.DivingEquipmentHelper;
import com.dexer.aquanaut.core.AttachmentRegistry;
import com.dexer.aquanaut.core.GameRuleRegistry;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.WaterAnimal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared helpers for Aquanaut's unified air system.
 *
 * <p>
 * Base air is stored in vanilla {@code airSupply}
 * (0..{@link #BASE_AIR_SUPPLY_TICKS}).
 * Extra air from equipment is stored separately and consumed before base,
 * and regenerated after base is full.
 */
public final class AirSupplyHelper {

    /** 60 seconds of base air at 20 ticks/second. */
    public static final int BASE_AIR_SUPPLY_TICKS = 20 * 60;
    public static final int BUBBLE_COUNT = 10;

    private static final Map<Integer, Integer> EXTRA_AIR = new ConcurrentHashMap<>();

    private AirSupplyHelper() {
    }

    public static int sanitizeExtraCapacity(int value) {
        return Math.max(0, value);
    }

    public static int bubblesToAirTicks(int bubbles) {
        return (BASE_AIR_SUPPLY_TICKS / BUBBLE_COUNT) * Math.max(0, bubbles);
    }

    public static boolean usesExtraAirSupply(LivingEntity entity) {
        return !(entity instanceof WaterAnimal) && !entity.getType().is(EntityTypeTags.CAN_BREATHE_UNDER_WATER);
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
        clampAir(entity);
    }

    public static int getEffectiveExtraCapacity(LivingEntity entity) {
        return sanitizeExtraCapacity(getMaxExtraAir(entity) + DivingEquipmentHelper.getTankCapacityBonus(entity));
    }

    public static int getTotalMaxAir(LivingEntity entity) {
        return BASE_AIR_SUPPLY_TICKS + getEffectiveExtraCapacity(entity);
    }

    public static void clampAir(LivingEntity entity) {
        int baseMax = BASE_AIR_SUPPLY_TICKS;
        if (entity.getAirSupply() > baseMax) {
            entity.setAirSupply(baseMax);
        }
        int extraMax = getEffectiveExtraCapacity(entity);
        int extra = getExtraAir(entity);
        if (extra > extraMax) {
            setExtraAir(entity, extraMax);
        }
    }

    public static void addAir(LivingEntity entity, int amount) {
        if (amount <= 0) {
            return;
        }

        int baseMax = BASE_AIR_SUPPLY_TICKS;
        int baseAir = entity.getAirSupply();
        int baseRoom = Math.max(0, baseMax - baseAir);
        int toBase = Math.min(amount, baseRoom);
        entity.setAirSupply(baseAir + toBase);

        int remaining = amount - toBase;
        if (remaining > 0) {
            fillExtraAir(entity, remaining);
        }
    }

    public static int getExtraAir(LivingEntity entity) {
        return EXTRA_AIR.getOrDefault(entity.getId(), 0);
    }

    public static int getTotalAir(LivingEntity entity) {
        return Math.max(0, entity.getAirSupply()) + getExtraAir(entity);
    }

    public static void setExtraAir(LivingEntity entity, int value) {
        int maxExtra = getEffectiveExtraCapacity(entity);
        int clamped = Math.max(0, Math.min(value, maxExtra));
        if (clamped <= 0) {
            EXTRA_AIR.remove(entity.getId());
        } else {
            EXTRA_AIR.put(entity.getId(), clamped);
        }
    }

    public static int consumeExtraAir(LivingEntity entity, int amount) {
        int current = getExtraAir(entity);
        if (current <= 0) {
            return amount;
        }
        int consumed = Math.min(amount, current);
        int remaining = current - consumed;
        if (remaining <= 0) {
            EXTRA_AIR.remove(entity.getId());
        } else {
            EXTRA_AIR.put(entity.getId(), remaining);
        }
        return amount - consumed;
    }

    public static void removeAir(LivingEntity entity, int amount) {
        if (amount <= 0) {
            return;
        }

        int remainder = consumeExtraAir(entity, amount);
        if (remainder > 0) {
            entity.setAirSupply(Math.max(0, entity.getAirSupply() - remainder));
        }
    }

    public static void fillExtraAir(LivingEntity entity, int amount) {
        int maxExtra = getEffectiveExtraCapacity(entity);
        if (maxExtra <= 0 || amount <= 0) {
            return;
        }
        int current = getExtraAir(entity);
        int room = Math.max(0, maxExtra - current);
        int toFill = Math.min(amount, room);
        if (toFill > 0) {
            EXTRA_AIR.put(entity.getId(), current + toFill);
        }
    }

    public static void fillExtraAirToMax(LivingEntity entity) {
        int maxExtra = getEffectiveExtraCapacity(entity);
        if (maxExtra <= 0) {
            return;
        }

        // Top off the full air reserve, filling base air first and then storing
        // any remaining capacity as extra air.
        int baseMax = BASE_AIR_SUPPLY_TICKS;
        int baseAir = entity.getAirSupply();
        int baseRoom = Math.max(0, baseMax - baseAir);
        int toBase = Math.min(maxExtra, baseRoom);

        if (toBase > 0) {
            entity.setAirSupply(baseAir + toBase);
        }

        setExtraAir(entity, maxExtra - toBase);
    }

    public static void clearExtraAir(int entityId) {
        EXTRA_AIR.remove(entityId);
    }

    /**
     * Uniform regen that refills one base air bar in a configurable number of
     * ticks (default {@link GameRuleRegistry#DEFAULT_REGEN_FILL_TIME_TICKS}).
     * Extra air uses the same per-tick amount after base air is full, so every
     * bubble layer fills at the same speed and total refill time scales with total
     * capacity.
     *
     * <p>
     * The base fill time can be changed by the server admin via {@code /gamerule
     * aquanautRegenFillTimeTicks <value>}.
     */
    public static int getRegenPerTick(LivingEntity entity) {
        if (!usesExtraAirSupply(entity)) {
            return 0;
        }

        if (!DivingEquipmentHelper.hasMask(entity) && !DivingEquipmentHelper.hasTank(entity)) {
            return 0;
        }

        int fillTimeTicks = Math.max(1, entity.level().getGameRules().getInt(GameRuleRegistry.REGEN_FILL_TIME_TICKS));
        int base = Math.max(1, BASE_AIR_SUPPLY_TICKS / fillTimeTicks);
        int maskBonus = DivingEquipmentHelper.getMaskRegenBonus(entity);
        int maskContribution = Math.max(0, maskBonus);

        return base + maskContribution;
    }
}
