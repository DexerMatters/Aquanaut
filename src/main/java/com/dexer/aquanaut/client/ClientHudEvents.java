package com.dexer.aquanaut.client;

import com.dexer.aquanaut.Aquanaut;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Renders extra air supply as stacked bubble layers on top of the vanilla air
 * bar.
 *
 * <h3>Visual design</h3>
 * <ul>
 * <li>Each "layer" equals one full vanilla air bar (=
 * {@code player.getMaxAirSupply()} ticks, 10 bubbles).</li>
 * <li>The currently draining layer loses bubbles from the <b>left</b>; the
 * vanilla bar loses from the right,
 * so the two directions are visually distinct.</li>
 * <li>Every layer uses a baked sprite variant; lower (older) layers peek
 * through the empty slots of the layer above them.</li>
 * </ul>
 *
 * <h3>Rendering order (back → front)</h3>
 * <ol>
 * <li>Full layers below the draining one (depth N … depth 1).</li>
 * <li>The draining layer (depth 0, partially filled).</li>
 * </ol>
 */
@EventBusSubscriber(modid = Aquanaut.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientHudEvents {

    private static final ResourceLocation AIR_SPRITE = ResourceLocation.withDefaultNamespace("hud/air");

    /**
     * Baked sprites applied per absolute layer slot (index 0 =
     * topmost/first-consumed).
     */
    private static final ResourceLocation[] LAYER_SPRITES = {
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "hud/air_layer_0"),
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "hud/air_layer_1"),
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "hud/air_layer_2"),
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "hud/air_layer_3"),
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "hud/air_layer_4"),
    };

    private ClientHudEvents() {
    }

    /**
     * Y coordinate of the air bar row, captured before vanilla processes the layer.
     */
    private static int cachedAirBarY = 0;

    @SubscribeEvent
    public static void onRenderAirLevelPre(RenderGuiLayerEvent.Pre event) {
        if (!VanillaGuiLayers.AIR_LEVEL.equals(event.getName())) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        // Snapshot before vanilla increments rightHeight, so Y is stable
        // regardless of whether vanilla renders the air bar this tick.
        cachedAirBarY = mc.getWindow().getGuiScaledHeight() - mc.gui.rightHeight;
    }

    @SubscribeEvent
    public static void onRenderAirLevelPost(RenderGuiLayerEvent.Post event) {
        if (!VanillaGuiLayers.AIR_LEVEL.equals(event.getName())) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) {
            return;
        }
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }

        // Creative/spectator — no air HUD needed
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        // Only show when extra air exists and the overlay is needed.
        int extraAir = ClientAirData.getExtraAir();
        int maxExtraAir = ClientAirData.getMaxExtraAir();
        if (extraAir <= 0 || maxExtraAir <= 0) {
            return;
        }

        // Hide only when both the main air bar is hidden AND extra air is full.
        if (!player.isUnderWater()
                && player.getAirSupply() >= player.getMaxAirSupply()
                && extraAir >= maxExtraAir) {
            return;
        }

        int maxAirSupply = player.getMaxAirSupply();
        if (maxAirSupply <= 0) {
            return;
        }

        var graphics = event.getGuiGraphics();
        int guiWidth = graphics.guiWidth();

        // Use the Y snapshotted in the Pre event — rightHeight may or may not
        // have been incremented by vanilla, so we can't derive it in Post.
        int airBarY = cachedAirBarY;

        // ── Decompose extraAir into layers ─────────────────────────────────────
        // layer = index of the currently-draining layer (0 = last tank remaining).
        // totalLayers = total number of tanks based on maxExtraAir.
        int totalLayers = Mth.ceil((float) maxExtraAir / maxAirSupply);

        // When extra air is still filling but the player is on the surface with
        // a full main bar, vanilla has already hidden its bubble row. Draw a
        // white (untinted) full row ourselves so the bar never disappears.
        boolean mainBarHiddenByVanilla = !player.isUnderWater()
                && player.getAirSupply() >= player.getMaxAirSupply();
        if (mainBarHiddenByVanilla && extraAir < maxExtraAir) {
            drawBubbles(graphics, guiWidth, airBarY, 0, 10, AIR_SPRITE);
        }

        // Only draw extra-air layers when there is actually some extra air left.
        if (extraAir > 0) {
            int layer = (extraAir - 1) / maxAirSupply;
            int topLayerAir = extraAir - layer * maxAirSupply;
            int topLayerBubbles = Mth.ceil((double) topLayerAir * 10.0 / maxAirSupply); // [1..10]

            // ── Draw from back to front ─────────────────────────────────────────────
            // Full layers (depth layer … depth 1): all 10 bubbles.
            for (int depth = layer; depth >= 1; depth--) {
                int absLayer = layer - depth;
                int slot = totalLayers - 1 - absLayer;
                drawBubbles(graphics, guiWidth, airBarY, 0, 10, spriteAt(slot));
            }
            // Draining layer: leftmost (10 - topLayerBubbles) slots are empty.
            int drainingSlot = totalLayers - 1 - layer;
            int emptySlots = 10 - topLayerBubbles;
            drawBubbles(graphics, guiWidth, airBarY, emptySlots, 10, spriteAt(drainingSlot));
        }
    }

    /**
     * Draws bubble sprites for slots [startSlot, endSlot) using the given sprite.
     * Slot 0 = leftmost bubble, slot 9 = rightmost (matching vanilla's j1=9 and
     * j1=0).
     */
    private static void drawBubbles(
            net.minecraft.client.gui.GuiGraphics graphics,
            int guiWidth, int y, int startSlot, int endSlot, ResourceLocation sprite) {
        for (int i = startSlot; i < endSlot; i++) {
            // Convert slot index (left=0) to vanilla x formula (j1 counts from right):
            // vanilla: x = guiWidth/2 + 91 - 9 - j1*8, j1 = 9-i
            int x = guiWidth / 2 + 91 - 9 - (9 - i) * 8;
            graphics.blitSprite(sprite, x, y, 9, 9);
        }
    }

    private static ResourceLocation spriteAt(int depth) {
        return LAYER_SPRITES[Math.min(depth, LAYER_SPRITES.length - 1)];
    }
}
