package com.dexer.aquanaut.common;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.diving.DivingEquipmentHelper;
import com.dexer.aquanaut.common.diving.DivingEquipmentSlotType;
import com.dexer.aquanaut.core.MobEffectRegistry;
import com.dexer.aquanaut.network.DivingEquipmentSyncPayload;
import com.dexer.aquanaut.network.ExtraAirPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shared server-side air and diving-equipment behavior for all non-water-living
 * entities.
 */
@EventBusSubscriber(modid = Aquanaut.MODID, bus = EventBusSubscriber.Bus.GAME)
@SuppressWarnings("removal")
public final class LivingAirEvents {

    // Pressure thresholds for narcosis (normalized [0, 1] hydrostatic pressure).
    // Using pressure rather than raw depth makes narcosis consistent across
    // dimensions and world types with different build heights.
    // ≈0.24 ≈ 30 m below sea level in a default Overworld (range 127 blocks).
    private static final float NARCOSIS_BASIC_PRESSURE = 0.24f;
    private static final float NARCOSIS_PRESSURE_STEP = 0.05f;
    private static final int NARCOSIS_MAX_AMPLIFIER = 2;
    private static final int NARCOSIS_DURATION_TICKS = 40;

    private static final Map<Integer, Integer> PREVIOUS_BASE_AIR = new HashMap<>();
    private static final Map<UUID, Integer> LAST_SENT_MAX_EXTRA = new HashMap<>();
    private static final Map<UUID, Integer> LAST_SENT_EXTRA_AIR = new HashMap<>();
    private static final Map<UUID, String> LAST_SENT_DIVING_ITEMS = new HashMap<>();
    private static final ResourceLocation FLIPPER_SPEED_MODIFIER_ID = ResourceLocation
            .fromNamespaceAndPath(Aquanaut.MODID, "flipper_speed_bonus");

    private LivingAirEvents() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }

        if (living instanceof ServerPlayer) {
            return;
        }

        if (!AirSupplyHelper.usesExtraAirSupply(living)) {
            removeFlipperBoost(living);
            return;
        }

        AirSupplyHelper.clampAir(living);
        if (AirSupplyHelper.getExtraAir(living) <= 0) {
            AirSupplyHelper.fillExtraAirToMax(living);
        }
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }

        PREVIOUS_BASE_AIR.remove(living.getId());
        AirSupplyHelper.clearExtraAir(living.getId());
        removeFlipperBoost(living);

        if (living instanceof ServerPlayer serverPlayer) {
            clearPlayerSync(serverPlayer.getUUID());
        }
    }

    @SubscribeEvent
    public static void onEntityTickPre(EntityTickEvent.Pre event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }

        // ServerPlayer is ticked outside the normal entity-tick loop in NeoForge;
        // EntityTickEvent does not fire for players — use PlayerTickEvent instead.
        if (living instanceof ServerPlayer) {
            return;
        }

        if (!AirSupplyHelper.usesExtraAirSupply(living)) {
            return;
        }

        PREVIOUS_BASE_AIR.put(living.getId(), living.getAirSupply());
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }

        // Players handled by PlayerTickEvent (see below).
        if (living instanceof ServerPlayer) {
            return;
        }

        if (AirSupplyHelper.usesExtraAirSupply(living)) {
            updateExtraAir(living);
            applyFlipperBoost(living);
            updateNarcosis(living);
        } else {
            removeFlipperBoost(living);
            removeNarcosis(living);
        }
    }

    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
        if (event.getEntity().level().isClientSide)
            return;
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        if (!AirSupplyHelper.usesExtraAirSupply(player))
            return;
        PREVIOUS_BASE_AIR.put(player.getId(), player.getAirSupply());
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide)
            return;
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer))
            return;

        if (AirSupplyHelper.usesExtraAirSupply(serverPlayer)) {
            updateExtraAir(serverPlayer);
            applyFlipperBoost(serverPlayer);
            updateNarcosis(serverPlayer);
        } else {
            removeFlipperBoost(serverPlayer);
            removeNarcosis(serverPlayer);
        }
        syncPlayerState(serverPlayer);
    }

    public static void clearPlayerSync(UUID uuid) {
        LAST_SENT_MAX_EXTRA.remove(uuid);
        LAST_SENT_EXTRA_AIR.remove(uuid);
        LAST_SENT_DIVING_ITEMS.remove(uuid);
    }

    private static void updateExtraAir(LivingEntity living) {
        Integer previousAir = PREVIOUS_BASE_AIR.get(living.getId());
        int currentAir = living.getAirSupply();

        if (previousAir != null && currentAir < previousAir) {
            int decrease = previousAir - currentAir;
            int notConsumed = AirSupplyHelper.consumeExtraAir(living, decrease);
            if (notConsumed < decrease) {
                int saved = decrease - notConsumed;
                living.setAirSupply(currentAir + saved);
            }
        }

        if (!living.isEyeInFluidType(NeoForgeMod.WATER_TYPE.value())
                && living.getAirSupply() >= AirSupplyHelper.BASE_AIR_SUPPLY_TICKS) {
            int regenPerTick = AirSupplyHelper.getRegenPerTick(living);
            if (regenPerTick > 0) {
                AirSupplyHelper.fillExtraAir(living, regenPerTick);
            }
        }
    }

    private static void syncPlayerState(ServerPlayer serverPlayer) {
        UUID uuid = serverPlayer.getUUID();
        int maxExtra = AirSupplyHelper.getEffectiveExtraCapacity(serverPlayer);
        int extraAir = AirSupplyHelper.getExtraAir(serverPlayer);

        Integer lastMax = LAST_SENT_MAX_EXTRA.get(uuid);
        Integer lastExtra = LAST_SENT_EXTRA_AIR.get(uuid);
        if (lastMax == null || lastMax.intValue() != maxExtra
                || lastExtra == null || lastExtra.intValue() != extraAir) {
            PacketDistributor.sendToPlayer(serverPlayer, new ExtraAirPayload(maxExtra, extraAir));
            LAST_SENT_MAX_EXTRA.put(uuid, maxExtra);
            LAST_SENT_EXTRA_AIR.put(uuid, extraAir);
        }

        String syncKey = DivingEquipmentHelper.getSyncItemId(serverPlayer, DivingEquipmentSlotType.MASK)
                + "|"
                + DivingEquipmentHelper.getSyncItemId(serverPlayer, DivingEquipmentSlotType.TANK)
                + "|"
                + DivingEquipmentHelper.getSyncItemId(serverPlayer, DivingEquipmentSlotType.FLIPPERS);
        String lastSyncKey = LAST_SENT_DIVING_ITEMS.get(uuid);
        if (lastSyncKey == null || !lastSyncKey.equals(syncKey)) {
            PacketDistributor.sendToPlayer(serverPlayer, DivingEquipmentSyncPayload.fromPlayer(serverPlayer));
            LAST_SENT_DIVING_ITEMS.put(uuid, syncKey);
        }
    }

    private static void applyFlipperBoost(LivingEntity living) {
        AttributeInstance attr = living.getAttribute(NeoForgeMod.SWIM_SPEED);
        if (attr == null) {
            return;
        }

        float multiplier = DivingEquipmentHelper.getFlipperSpeedMultiplier(living);
        if (multiplier <= 1.0F || !living.isInWater()) {
            attr.removeModifier(FLIPPER_SPEED_MODIFIER_ID);
            return;
        }

        attr.addOrUpdateTransientModifier(new AttributeModifier(
                FLIPPER_SPEED_MODIFIER_ID,
                multiplier - 1.0F,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    private static void removeFlipperBoost(LivingEntity living) {
        AttributeInstance attr = living.getAttribute(NeoForgeMod.SWIM_SPEED);
        if (attr != null) {
            attr.removeModifier(FLIPPER_SPEED_MODIFIER_ID);
        }
    }

    private static void updateNarcosis(LivingEntity living) {
        if (living instanceof Player player && (player.isCreative() || player.isSpectator())) {
            removeNarcosis(living);
            return;
        }

        BlockPos eyePos = BlockPos.containing(living.getEyePosition());
        int amplifier = getNarcosisAmplifier(living, eyePos);
        if (amplifier < 0) {
            removeNarcosis(living);
            return;
        }

        MobEffectInstance current = living.getEffect(MobEffectRegistry.NARCOSIS);
        if (current != null
                && current.getAmplifier() == amplifier
                && current.getDuration() > NARCOSIS_DURATION_TICKS / 2) {
            return;
        }

        living.addEffect(new MobEffectInstance(MobEffectRegistry.NARCOSIS, NARCOSIS_DURATION_TICKS,
                amplifier, false, true, true));
    }

    private static void removeNarcosis(LivingEntity living) {
        living.removeEffect(MobEffectRegistry.NARCOSIS);
    }

    private static int getNarcosisAmplifier(LivingEntity living, BlockPos eyePos) {
        int connectedSurfaceY = PressureHelper.getConnectedSurfaceY(living.level(), eyePos);
        if (connectedSurfaceY == Integer.MIN_VALUE) {
            return -1;
        }

        int depth = connectedSurfaceY - eyePos.getY();
        if (depth <= 0) {
            return -1;
        }

        int maxDepth = connectedSurfaceY - living.level().getMinBuildHeight();
        if (maxDepth <= 0) {
            return -1;
        }

        float pressure = (float) depth / maxDepth;
        float resistance = DivingEquipmentHelper.getMaskNarcosisResistance(living);
        float threshold = NARCOSIS_BASIC_PRESSURE + resistance;

        if (pressure < threshold) {
            return -1;
        }

        if (pressure < threshold + NARCOSIS_PRESSURE_STEP) {
            return 0;
        }

        if (pressure < threshold + (NARCOSIS_PRESSURE_STEP * 2)) {
            return 1;
        }

        return NARCOSIS_MAX_AMPLIFIER;
    }
}
