package com.dexer.aquanaut.common;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.FlotationHelper.FlotationDevice;
import com.dexer.aquanaut.common.item.HandheldAirBladderItem;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Applies the air bladders' buoyancy to whoever is holding one.
 *
 * <p>
 * The small bladder clamps the sink gravity builds up back to level, so the
 * holder keeps their depth; the large one drives them up hard until their head
 * breaks the surface. Sneaking is left alone, so a diver can still go down on
 * purpose.
 *
 * <p>
 * The rule runs on both sides without a velocity packet, the way vanilla's
 * bubble columns do: it is deterministic, so the client and the authoritative
 * server derive the same motion, and the server's own simulation stays
 * consistent with the position updates it is validating. Only the local player
 * is touched on the client -- other players belong to the server.
 */
@EventBusSubscriber(modid = Aquanaut.MODID)
public final class HandheldAirBladderEvents {

    private HandheldAirBladderEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide && !player.isLocalPlayer()) {
            return;
        }
        if (player.getAbilities().flying) {
            return; // creative flight is not swimming
        }

        FlotationDevice device = HandheldAirBladderItem.deviceInHand(player);
        if (!FlotationHelper.isActive(device, player.isInWater(),
                player.isEyeInFluidType(NeoForgeMod.WATER_TYPE.value()), player.isShiftKeyDown())) {
            return;
        }

        Vec3 motion = player.getDeltaMovement();
        double vertical = FlotationHelper.verticalVelocity(device, motion.y);
        if (vertical != motion.y) {
            player.setDeltaMovement(motion.x, vertical, motion.z);
        }
    }
}
