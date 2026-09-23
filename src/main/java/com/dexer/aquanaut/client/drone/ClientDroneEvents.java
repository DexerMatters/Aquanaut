package com.dexer.aquanaut.client.drone;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.client.screen.TargetPickerScreen;
import com.dexer.aquanaut.common.drone.DroneControlInput;
import com.dexer.aquanaut.common.drone.SubmarineDroneService;
import com.dexer.aquanaut.common.entity.SubmarineDroneEntity;
import com.dexer.aquanaut.common.entity.TagRules;
import com.dexer.aquanaut.common.item.SubmarineDroneControllerItem;
import com.dexer.aquanaut.network.SubmarineDroneControlPayload;
import com.dexer.aquanaut.network.SubmarineDroneHeadlightPayload;
import com.dexer.aquanaut.network.SubmarineDroneLinkPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * The client half of flying a submarine drone.
 *
 * <h3>The player keeps their own eyes</h3>
 * There is no camera takeover. The operator stands at the controls, sees their own surroundings, and
 * watches the drone through the feed panel or with their own eyes if it is close enough.
 *
 * <h3>No input delay</h3>
 * Two things remove it, and both are needed.
 *
 * <p>
 * The command is read on {@link ClientTickEvent.Pre}, at the head of the client tick, so the drone
 * simulates with it in the <em>same</em> tick the key went down rather than the next one.
 *
 * <p>
 * And while this client is flying, it marks the drone as locally controlled, which makes the client
 * simulate the hull instead of replaying the server's position. The alternative is that every
 * keypress costs a round trip plus three ticks of position interpolation before it is visible — on a
 * machine steered by hand, that reads as the controls being broken. The server stays authoritative;
 * {@code SubmarineDroneEntity#lerpTo} reconciles the two whenever they genuinely disagree.
 *
 * <h3>Linking, and letting go</h3>
 * The right button does two jobs depending on how long it is held. A quick press links to the drone
 * under the crosshair, which is the gesture for a drone that is right there. A press that is held
 * opens {@link TargetPickerScreen} — the same list the compass uses, holding only drones — and the
 * release then links to whichever drone the list is on. That is the only way to reach a drone behind
 * the operator, or to move a link from one drone to another.
 *
 * <p>
 * Letting go is the attack button — the left mouse by default — which is also the only gesture that
 * could not be spent on anything else while flying, since the operator's own attacks are cancelled.
 * Neither gesture is a toggle, so a stray press cannot silently undo the other.
 */
@EventBusSubscriber(modid = Aquanaut.MODID, value = Dist.CLIENT)
public final class ClientDroneEvents {

    /** Headlight toggle; registered on the client mod event bus and read while flying. */
    public static final KeyMapping HEADLIGHT_KEY = new KeyMapping(
            "key.aquanaut.drone_headlight", GLFW.GLFW_KEY_H, "key.categories.aquanaut");

    /** The drone this client is currently flying, so it can be released when the session ends. */
    @Nullable
    private static SubmarineDroneEntity pilotedDrone;

    /**
     * Ticks the right button must be held before the controller offers its list instead of a quick
     * link. The same six ticks the compass uses, so the two gestures feel like one gesture.
     */
    private static final int LINK_HOLD_TICKS = 6;

    /** Most drones the link list will offer. */
    private static final int LINK_LIST_LIMIT = 10;

    /** Whether the right button was down last tick, and how long the current press has lasted. */
    private static boolean linkHeld;
    private static int linkHoldTicks;

    /**
     * Set once a press has passed the hold threshold and been answered — with a list, or with the news
     * that there is nothing to list. Its release is then not also a quick link.
     */
    private static boolean linkResolved;

    /**
     * The drone the press started on, if any: the one a quick press links to, and the row the list
     * opens on so that aiming at a drone and holding does not quietly choose a different one.
     */
    @Nullable
    private static SubmarineDroneEntity linkTarget;

    /** Left-button edge detection, so one press is one detach. */
    private static boolean leftButtonHeld;

    /**
     * Set for the rest of the tick in which the left button let go of a drone, so the same press is
     * not also spent swinging at the world: the attack is cancelled while the button is down and
     * this is what keeps it cancelled after the session has already ended.
     */
    private static boolean detachClickSpent;

    private ClientDroneEvents() {
    }

    // ------------------------------------------------------------------
    // per-tick session management
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        // One tick's worth of "the left button has been spent letting go", cleared before anything
        // can read it: the keybind pass later in the same tick must not see the previous tick's.
        detachClickSpent = false;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.level == null) {
            stopFlying();
            return;
        }

        ItemStack controller = heldController(player);
        UUID linkedId = controller.isEmpty() ? null : SubmarineDroneControllerItem.getLinkedDrone(controller);
        SubmarineDroneEntity drone = resolveDrone(minecraft, linkedId);
        boolean flying = drone != null && drone.isControlled() && minecraft.screen == null
                && !player.isSpectator();

        if (flying) {
            startFlying(drone, readControlInput(minecraft));
            if (linkedId != null) {
                PacketDistributor.sendToServer(new SubmarineDroneControlPayload(linkedId, ClientDroneState.input()));
            }
            if (minecraft.screen == null && HEADLIGHT_KEY.consumeClick() && linkedId != null) {
                PacketDistributor.sendToServer(new SubmarineDroneHeadlightPayload(linkedId));
            }
        } else {
            // Do not carry an L press made outside a drone session into the next one.
            HEADLIGHT_KEY.consumeClick();
            // Putting the controller away ends the session. Tell the server to stop before letting
            // go of the reference, so the drone does not coast on its last command for the length of
            // the dead-man timeout.
            if (pilotedDrone != null) {
                PacketDistributor.sendToServer(new SubmarineDroneControlPayload(pilotedDrone.getUUID(), 0));
            }
            stopFlying();
        }

        updateLinkGesture(minecraft, controller);
        updateDetachGesture(minecraft);
        closeLinkListWithoutController(minecraft, player);
    }

    /** Hands the drone its command for this tick and takes over simulating it. */
    private static void startFlying(SubmarineDroneEntity drone, int input) {
        if (pilotedDrone != drone && pilotedDrone != null) {
            pilotedDrone.setLocallyPiloted(false);
        }
        pilotedDrone = drone;
        drone.setLocallyPiloted(true);
        drone.setLocalControlInput(input);
        ClientDroneState.setSession(drone, input);
    }

    /** Stops simulating, tells the drone to stop, and lets the server own it again. */
    private static void stopFlying() {
        if (pilotedDrone != null) {
            pilotedDrone.setLocallyPiloted(false);
            pilotedDrone = null;
        }
        ClientDroneState.stop();
    }

    /**
     * Resolves the controller's drone in the client level.
     *
     * <p>
     * The link is a UUID, and there is no public lookup by UUID on a client world, so this walks the
     * entities the client already knows about. That list is small — only entities inside their
     * tracking range — and the result is remembered, so the walk stops as soon as a session is
     * established. A drone that has been picked up or unloaded resolves to nothing.
     */
    @Nullable
    private static SubmarineDroneEntity resolveDrone(Minecraft minecraft, @Nullable UUID droneId) {
        if (droneId == null || minecraft.level == null) {
            return null;
        }
        SubmarineDroneEntity cached = ClientDroneState.drone();
        if (cached != null && !cached.isRemoved() && droneId.equals(cached.getUUID())) {
            return cached;
        }
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof SubmarineDroneEntity drone && droneId.equals(drone.getUUID())) {
                return drone;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        stopFlying();
        linkHeld = false;
        linkHoldTicks = 0;
        linkResolved = false;
        linkTarget = null;
        leftButtonHeld = false;
        detachClickSpent = false;
        DroneFeedRenderer.reset();
    }

    // ------------------------------------------------------------------
    // controls
    // ------------------------------------------------------------------

    /**
     * Reads the drone's controls off the player's own movement bindings.
     *
     * <p>
     * Reading {@code Options} rather than registering a parallel set of keys means the controls
     * follow the player's own rebinds, and cannot fight over a physical key with anything else.
     *
     * <ul>
     * <li>forward / back — the dive planes, nose up and nose down</li>
     * <li>left / right — the rudder</li>
     * <li>jump — the jet, ahead</li>
     * <li>sneak — the jet, astern</li>
     * </ul>
     */
    private static int readControlInput(Minecraft minecraft) {
        int input = 0;
        if (minecraft.options.keyUp.isDown()) {
            input |= DroneControlInput.PITCH_UP;
        }
        if (minecraft.options.keyDown.isDown()) {
            input |= DroneControlInput.PITCH_DOWN;
        }
        if (minecraft.options.keyLeft.isDown()) {
            input |= DroneControlInput.YAW_LEFT;
        }
        if (minecraft.options.keyRight.isDown()) {
            input |= DroneControlInput.YAW_RIGHT;
        }
        if (minecraft.options.keyJump.isDown()) {
            input |= DroneControlInput.THRUST_FORWARD;
        }
        if (minecraft.options.keyShift.isDown()) {
            input |= DroneControlInput.THRUST_BACKWARD;
        }
        return input;
    }

    /**
     * Lets go on the left-button press edge.
     *
     * <p>
     * Read from the raw button rather than from an item use state, for the same reason attaching is:
     * the gesture has to behave the same however the operator is aiming. The press edge, not the
     * held state, is what matters — a latched button would drop the link and immediately try again
     * on the next tick.
     */
    private static void updateDetachGesture(Minecraft minecraft) {
        boolean held = minecraft.screen == null
                && minecraft.mouseHandler.isMouseGrabbed()
                && minecraft.options.keyAttack.isDown();
        boolean pressed = held && !leftButtonHeld;
        leftButtonHeld = held;

        if (pressed && ClientDroneState.isFlying()) {
            detachClickSpent = true;
            detach(minecraft);
        }
    }

    /** Lets go of whatever the held controller is attached to. */
    private static void detach(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (minecraft.screen != null || player == null) {
            return;
        }
        ItemStack controller = heldController(player);
        if (controller.isEmpty()) {
            return;
        }
        UUID linked = SubmarineDroneControllerItem.getLinkedDrone(controller);
        if (linked == null) {
            return;
        }
        // Stop flying before the link goes, so the drone is not left coasting on its last command
        // and this client is not left simulating something it no longer controls.
        stopFlying();
        PacketDistributor.sendToServer(SubmarineDroneLinkPayload.detaching(linked));
    }

    /**
     * The controller's right-button gesture, in two halves.
     *
     * <p>
     * A press that starts on a drone links to it, which is the gesture for a drone that is right there
     * and is exactly what this control has always done. A press that is <em>held</em> opens
     * {@link TargetPickerScreen} instead — the same list the compass uses, holding only drones — and the
     * release then links to whichever row it is on. That is the only way to reach a drone that is behind
     * the operator, or to move the link from one drone to another while flying: holding is a deliberate
     * act, so it is allowed to change the answer a press already gave.
     *
     * <p>
     * The button is read raw rather than from the item's use state, because aiming at a drone within
     * arm's reach never starts an item use — the game routes that press to the entity instead. Reading
     * the button directly means both halves behave identically whether the drone is under the player's
     * nose or out of sight behind them.
     */
    private static void updateLinkGesture(Minecraft minecraft, ItemStack controller) {
        boolean down = minecraft.options.keyUse.isDown();

        if (!down) {
            // The button is up. Nothing is sent here: the quick link happened on the press, and the list
            // sends its own choice when it is released on a row.
            linkHeld = false;
            linkHoldTicks = 0;
            linkResolved = false;
            linkTarget = null;
            return;
        }

        if (minecraft.screen != null) {
            // The list this press opened is up, and it owns the gesture until it closes.
            return;
        }

        if (!linkHeld) {
            linkHeld = true;
            linkHoldTicks = 0;
            linkResolved = false;
            linkTarget = controller.isEmpty() ? null : findAimedDrone(minecraft);
            // While a drone is already being flown a press is not a re-target but the beginning of a
            // possible switch, and that is the list's business.
            if (linkTarget != null && !ClientDroneState.isFlying()) {
                PacketDistributor.sendToServer(SubmarineDroneLinkPayload.attaching(linkTarget.getUUID()));
            }
            return;
        }

        linkHoldTicks++;
        if (linkHoldTicks >= LINK_HOLD_TICKS && !linkResolved && !controller.isEmpty()) {
            linkResolved = true;
            openLinkList(minecraft, controller, linkTarget);
        }
    }

    /**
     * Offers the drones the controller could link to, and says so when there are none.
     *
     * <p>
     * The list starts on the drone the press was aimed at, then on the one already linked, then on the
     * nearest, so aiming at a drone and holding simply re-affirms it rather than quietly choosing
     * something else.
     */
    private static void openLinkList(Minecraft minecraft, ItemStack controller,
            @Nullable SubmarineDroneEntity startedOn) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }

        UUID linked = SubmarineDroneControllerItem.getLinkedDrone(controller);
        List<SubmarineDroneEntity> drones = nearbyDrones(player);
        if (drones.isEmpty()) {
            player.displayClientMessage(Component.translatable("gui.aquanaut.drone.link.none"), true);
            return;
        }

        List<TargetPickerScreen.Option> options = new ArrayList<>(drones.size());
        for (SubmarineDroneEntity drone : drones) {
            String name = drone.getTagName().isEmpty() ? "?" : drone.getTagName();
            // A hull somebody else is flying is still offered — its pilot may have logged out, and the
            // server is the one that decides — but the list says so before the release rather than after.
            boolean occupied = drone.isControlled() && !drone.getUUID().equals(linked);
            options.add(new TargetPickerScreen.Option(drone.getUUID(),
                    occupied
                            ? Component.translatable("gui.aquanaut.drone.link.busy", name)
                            : Component.literal(name),
                    TagRules.toArgb(drone.getTagColor()),
                    null));
        }

        minecraft.setScreen(new TargetPickerScreen(TargetPickerScreen.Source.CONTROLLER,
                Component.translatable("gui.aquanaut.drone.link.title"),
                Component.translatable("gui.aquanaut.drone.link.hint"),
                options, highlightFor(drones, startedOn, linked),
                id -> {
                    if (id != null) {
                        PacketDistributor.sendToServer(SubmarineDroneLinkPayload.selecting(id));
                    }
                }));
    }

    /** Where the link list starts: what the press was aimed at, then what is linked, then the nearest. */
    private static int highlightFor(List<SubmarineDroneEntity> drones,
            @Nullable SubmarineDroneEntity startedOn, @Nullable UUID linked) {
        for (int i = 0; i < drones.size(); i++) {
            if (drones.get(i) == startedOn) {
                return i;
            }
        }
        if (linked != null) {
            for (int i = 0; i < drones.size(); i++) {
                if (linked.equals(drones.get(i).getUUID())) {
                    return i;
                }
            }
        }
        return 0;
    }

    /**
     * The drones a controller could link to, nearest first.
     *
     * <p>
     * {@link SubmarineDroneService#LINK_RANGE} is the radio's reach, and it is deliberately the number
     * the aimed gesture uses too: choosing from a list changes how a drone is named, not how far the
     * controller reaches. Drones only become visible to this client within its tracking range, so the
     * list can never offer more than the client actually knows about.
     */
    private static List<SubmarineDroneEntity> nearbyDrones(LocalPlayer player) {
        List<SubmarineDroneEntity> drones = new ArrayList<>(player.level().getEntitiesOfClass(
                SubmarineDroneEntity.class,
                player.getBoundingBox().inflate(SubmarineDroneService.LINK_RANGE)));
        drones.sort(Comparator.comparingDouble(drone -> drone.distanceToSqr(player)));
        return drones.size() > LINK_LIST_LIMIT ? drones.subList(0, LINK_LIST_LIMIT) : drones;
    }

    /** A list opened from the controller means nothing once the controller is out of the player's hands. */
    private static void closeLinkListWithoutController(Minecraft minecraft, LocalPlayer player) {
        if (minecraft.screen instanceof TargetPickerScreen picker
                && picker.source() == TargetPickerScreen.Source.CONTROLLER
                && heldController(player).isEmpty()) {
            picker.onClose();
        }
    }

    /** The nearest drone under the crosshair, with nothing solid in the way. */
    @Nullable
    private static SubmarineDroneEntity findAimedDrone(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return null;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 reach = eye.add(player.getLookAngle().scale(SubmarineDroneService.LINK_RANGE));

        double limit = SubmarineDroneService.LINK_RANGE;
        BlockHitResult block = minecraft.level.clip(new ClipContext(eye, reach,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) {
            limit = eye.distanceTo(block.getLocation());
        }
        double unobstructed = limit;

        SubmarineDroneEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        AABB search = player.getBoundingBox().inflate(SubmarineDroneService.LINK_RANGE);
        for (SubmarineDroneEntity drone : minecraft.level.getEntitiesOfClass(SubmarineDroneEntity.class, search)) {
            Optional<Vec3> hit = drone.getBoundingBox().inflate(0.2D).clip(eye, reach);
            if (hit.isEmpty()) {
                continue;
            }
            double distance = eye.distanceTo(hit.get());
            if (distance <= unobstructed && distance < bestDistance) {
                bestDistance = distance;
                best = drone;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------
    // input suppression while flying
    // ------------------------------------------------------------------

    /**
     * The operator is standing at the controls; their legs must not wander off with the same keys
     * that are flying the drone.
     */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!ClientDroneState.isFlying()) {
            return;
        }
        Input input = event.getInput();
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    /**
     * The operator cannot punch or place while both hands are on the controller. The attack button
     * is the one exception, and only as a gesture: it lets go, and the press is consumed doing so.
     */
    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isAttack()) {
            if (!ClientDroneState.isFlying() && !detachClickSpent) {
                return;
            }
            // While the link is live the button is the detach control, and for the rest of the tick
            // it let go it is nothing at all: releasing it back to the world would start mining the
            // block the operator happened to be looking at.
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }
        if (!ClientDroneState.isFlying()) {
            return;
        }
        if (event.isUseItem()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    /** The instrument panel, drawn on top of the vanilla HUD so its figures stay crisp. */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ClientDroneState.isFlying() || minecraft.options.hideGui) {
            return;
        }
        DroneHudRenderer.render(event.getGuiGraphics(), minecraft);
    }

    /** The controller in either hand, or {@link ItemStack#EMPTY}. */
    private static ItemStack heldController(LocalPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof SubmarineDroneControllerItem) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        return off.getItem() instanceof SubmarineDroneControllerItem ? off : ItemStack.EMPTY;
    }
}
