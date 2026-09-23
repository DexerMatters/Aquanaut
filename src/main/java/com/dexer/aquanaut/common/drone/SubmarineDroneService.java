package com.dexer.aquanaut.common.drone;

import java.util.UUID;

import javax.annotation.Nullable;

import com.dexer.aquanaut.common.entity.SubmarineDroneEntity;
import com.dexer.aquanaut.common.item.SubmarineDroneControllerItem;
import com.dexer.aquanaut.core.SoundRegistry;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The server half of the drone link: the authority that owns attaching, detaching and control input.
 *
 * <p>
 * Both network payloads land here. They are deliberately thin — the client reports what it sees and
 * what keys are down, and every decision that matters (is the drone real, is it in range, can the
 * pilot actually see it, is somebody else flying it) is re-made here. A hacked client can therefore
 * ask, but it cannot decide.
 */
public final class SubmarineDroneService {

    /**
     * How far a drone can be and still be linked, in blocks. Longer than a player's reach by an
     * order of magnitude: the whole point of the controller is that it is a remote.
     */
    public static final double LINK_RANGE = SubmarineDroneControllerItem.REMOTE_RANGE;

    /**
     * How far the radio link carries thruster input. Past this the drone keeps its last heading and
     * holds station rather than answering — the pilot has flown out of range.
     */
    public static final double CONTROL_RANGE = 128.0D;

    private SubmarineDroneService() {
    }

    // ------------------------------------------------------------------
    // lookup
    // ------------------------------------------------------------------

    /** The controller in either hand, or {@link ItemStack#EMPTY}. */
    public static ItemStack heldController(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof SubmarineDroneControllerItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Resolves the drone a payload refers to.
     *
     * <p>
     * The id is looked up in the level's entity index rather than by scanning a box: the pilot is
     * expected to be a long way from their drone, and a bounding-box query wide enough to cover the
     * radio range would walk the whole loaded world every packet.
     */
    @Nullable
    public static SubmarineDroneEntity findDrone(ServerPlayer player, UUID droneId) {
        ServerLevel level = player.serverLevel();
        Entity entity = level.getEntity(droneId);
        return entity instanceof SubmarineDroneEntity drone ? drone : null;
    }

    /** Whether the player's crosshair lands on {@code drone} with nothing solid in between. */
    public static boolean looksAt(ServerPlayer player, SubmarineDroneEntity drone, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 reach = eye.add(player.getLookAngle().scale(range));

        double limit = range;
        BlockHitResult block = player.level().clip(new ClipContext(eye, reach,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) {
            limit = eye.distanceTo(block.getLocation());
        }
        double unobstructed = limit;

        AABB target = drone.getBoundingBox().inflate(0.2D);
        return target.clip(eye, reach)
                .filter(hit -> eye.distanceTo(hit) <= unobstructed)
                .isPresent();
    }

    // ------------------------------------------------------------------
    // attach / detach
    // ------------------------------------------------------------------

    /**
     * Attaches the held controller to a drone.
     *
     * <p>
     * Attaching is not a toggle: pressing the button again on a drone that is already linked does
     * nothing, which is why letting go has its own control. That split matters because the two
     * gestures are used in completely different situations — attaching needs the drone within reach,
     * while detaching happens later, once the drone is somewhere else entirely.
     *
     * <p>
     * There are two ways for the client to name the drone, and they are checked differently.
     * <b>Aimed</b> is a gesture at something in front of the player, so the drone has to be visible
     * and unobstructed within {@link #LINK_RANGE}. <b>Listed</b> comes from the controller's own
     * picker, which is a radio list rather than an eyeline: the drone only has to be inside the same
     * range, and may be behind the pilot's back — that is the whole point of choosing from a list.
     *
     * @param aimed whether the drone was named by the crosshair rather than picked from the list
     */
    public static void attach(ServerPlayer player, UUID droneId, boolean aimed) {
        ItemStack controller = heldController(player);
        if (controller.isEmpty()) {
            return;
        }

        SubmarineDroneEntity drone = findDrone(player, droneId);
        if (drone == null) {
            return;
        }
        if (droneId.equals(SubmarineDroneControllerItem.getLinkedDrone(controller)) && drone.isOperatedBy(player)) {
            // Already attached to this drone by this pilot: nothing to do, and emphatically nothing to
            // undo. A controller that names the drone while the drone no longer answers to the pilot is
            // a stale link — the radio dropped it, or somebody else took it over — and falls through so
            // that this press repairs it rather than reporting success over a dead link.
            return;
        }
        boolean inReach = aimed
                ? looksAt(player, drone, LINK_RANGE)
                : player.distanceToSqr(drone) <= LINK_RANGE * LINK_RANGE;
        if (!inReach) {
            return;
        }
        if (!player.level().getWorldBorder().isWithinBounds(drone.blockPosition())) {
            return;
        }

        // A drone being flown by somebody who is still online is theirs; an abandoned one is fair
        // game, so a pilot who logs out does not lock their machine away forever.
        boolean flownBySomeoneElse = drone.operatorId()
                .filter(id -> !id.equals(player.getUUID()))
                .filter(id -> player.server.getPlayerList().getPlayer(id) != null)
                .isPresent();
        if (flownBySomeoneElse) {
            deny(player, "message.aquanaut.submarine_drone.occupied");
            return;
        }

        releasePrevious(player, controller, droneId);
        SubmarineDroneControllerItem.link(controller, droneId);
        drone.setOperator(player.getUUID());
        chirp(player, drone, SoundRegistry.SUBMARINE_DRONE_ACTIVATE.get());
        player.displayClientMessage(Component.translatable("message.aquanaut.submarine_drone.activated"), true);
    }

    /**
     * Lets go of whatever the held controller is attached to.
     *
     * <p>
     * No aim and no range check: this is the pilot switching off, and they may be anywhere. The
     * drone is left holding station on its own dead-man switch.
     */
    public static void detach(ServerPlayer player) {
        ItemStack controller = heldController(player);
        if (controller.isEmpty()) {
            return;
        }
        UUID droneId = SubmarineDroneControllerItem.getLinkedDrone(controller);
        if (droneId == null) {
            return;
        }

        SubmarineDroneControllerItem.unlink(controller);
        SubmarineDroneEntity drone = findDrone(player, droneId);
        if (drone != null) {
            drone.setOperator(null);
            chirp(player, drone, SoundRegistry.SUBMARINE_DRONE_DEACTIVATE.get());
        }
        player.displayClientMessage(Component.translatable("message.aquanaut.submarine_drone.deactivated"), true);
    }

    /**
     * Powers down whatever the controller was linked to before, when it is being linked elsewhere.
     *
     * <p>
     * One controller flies one machine at a time. Without this, choosing a second drone from the list
     * would leave the first one still bound and still drawn as a live hull, answering to nobody.
     */
    private static void releasePrevious(ServerPlayer player, ItemStack controller, UUID droneId) {
        UUID previousId = SubmarineDroneControllerItem.getLinkedDrone(controller);
        if (previousId == null || previousId.equals(droneId)) {
            return;
        }
        SubmarineDroneEntity previous = findDrone(player, previousId);
        if (previous != null) {
            previous.setOperator(null);
        }
    }

    /**
     * Drops a link that has run out of radio.
     *
     * <p>
     * This is the one failure the pilot cannot report: past {@link #CONTROL_RANGE} the client can
     * neither see the drone nor command it, so the release has to come from the end that is still
     * there. Called by the drone itself on every server tick it is bound.
     *
     * <p>
     * An operator who is merely offline is left alone, and so is one in another world: an abandoned
     * drone is fair game to take over, which is a different thing from a signal that has gone.
     */
    public static void enforceLinkRange(SubmarineDroneEntity drone) {
        UUID pilotId = drone.operatorId().orElse(null);
        if (pilotId == null || !(drone.level() instanceof ServerLevel level) || level.getServer() == null) {
            return;
        }
        ServerPlayer pilot = level.getServer().getPlayerList().getPlayer(pilotId);
        if (pilot == null) {
            return;
        }
        if (pilot.level() == level && pilot.distanceToSqr(drone) <= CONTROL_RANGE * CONTROL_RANGE) {
            return;
        }

        drone.setOperator(null);
        forgetDrone(pilot, drone.getUUID());
        chirp(pilot, drone, SoundRegistry.SUBMARINE_DRONE_DEACTIVATE.get());
        pilot.displayClientMessage(
                Component.translatable("message.aquanaut.submarine_drone.signal_lost"), true);
    }

    /**
     * Forgets a drone the player's controller is pointing at, because the link has ended — the drone
     * was picked back up, or its radio ran out.
     *
     * <p>
     * The whole inventory is searched, not just the hands: a controller put away is still the one that
     * will be picked up again, and a link left pointing at a drone that no longer answers would read as
     * attached with nothing on the other end.
     */
    public static void forgetDrone(ServerPlayer player, UUID droneId) {
        for (ItemStack stack : player.getInventory().items) {
            unlinkIfPointingAt(stack, droneId);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            unlinkIfPointingAt(stack, droneId);
        }
    }

    private static void unlinkIfPointingAt(ItemStack stack, UUID droneId) {
        if (stack.getItem() instanceof SubmarineDroneControllerItem
                && droneId.equals(SubmarineDroneControllerItem.getLinkedDrone(stack))) {
            SubmarineDroneControllerItem.unlink(stack);
        }
    }

    /**
     * Plays the link handshake twice: once at the operator, once at the drone.
     *
     * <p>
     * The drone may be most of a radio range away, and a sound played only at the drone would be
     * attenuated to nothing long before it reached the operator. Playing it at the operator as well
     * is not a cheat — a controller beeping to confirm the link is exactly what the hardware would
     * do — while the copy at the drone is what a bystander nearby would hear.
     */
    private static void chirp(ServerPlayer player, SubmarineDroneEntity drone, SoundEvent sound) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS,
                0.9F, 1.0F);
        drone.level().playSound(null, drone, sound, SoundSource.NEUTRAL, 0.7F, 1.0F);
    }

    // ------------------------------------------------------------------
    // thruster input
    // ------------------------------------------------------------------

    /**
     * Forwards a pilot's thruster command to their drone.
     *
     * <p>
     * Only the operating pilot may steer, and only while the drone is inside the radio range. A
     * command from anybody else, or from beyond the link, is dropped silently — the drone's own
     * dead-man switch then brings it to a halt.
     */
    public static void applyInput(ServerPlayer player, UUID droneId, int input) {
        SubmarineDroneEntity drone = findDrone(player, droneId);
        if (drone == null || !drone.isOperatedBy(player)) {
            return;
        }
        if (player.distanceToSqr(drone) > CONTROL_RANGE * CONTROL_RANGE) {
            return;
        }
        drone.setControlInput(input);
    }

    /** Toggles the headlight for the pilot's currently operated drone. */
    public static void toggleHeadlight(ServerPlayer player, UUID droneId) {
        SubmarineDroneEntity drone = findDrone(player, droneId);
        if (drone == null || !drone.isOperatedBy(player)) {
            return;
        }
        if (player.distanceToSqr(drone) > CONTROL_RANGE * CONTROL_RANGE) {
            return;
        }
        drone.toggleHeadlight();
    }

    private static void deny(ServerPlayer player, String messageKey) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundRegistry.SUBMARINE_DRONE_REJECT.get(), SoundSource.PLAYERS, 0.8F, 1.2F);
        player.displayClientMessage(Component.translatable(messageKey), true);
    }
}
