package com.dexer.aquanaut.client;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.AirSupplyHelper;
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
 * <li>Each "layer" equals one base air bar
 * ({@code AirSupplyHelper.BASE_AIR_SUPPLY_TICKS} ticks, 10 bubbles).</li>
 * <li>The currently draining layer loses bubbles from the <b>left</b>; the
 * vanilla bar loses from the right,
 * so the two directions are visually distinct.</li>
 * <li>Every layer uses a baked sprite variant; lower (older) layers peek
 * through the empty slots of the layer above them.</li>
 * </ul>
 *
 * <h3>Rendering order (back → front)</h3>
 * <ol>
 * <li>Base vanilla bubbles (hud/air).</li>
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

        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        int baseAirSupply = AirSupplyHelper.BASE_AIR_SUPPLY_TICKS;
        int maxExtraAir = ClientAirData.getMaxExtraAir();
        if (maxExtraAir <= 0) {
            return;
        }

        int currentExtraAir = ClientAirData.getCurrentExtraAir();
        int totalAir = player.getAirSupply() + currentExtraAir;
        int totalMaxAir = baseAirSupply + maxExtraAir;

        // Hide only when not underwater and everything is full.
        if (!player.isUnderWater() && totalAir >= totalMaxAir) {
            return;
        }

        var graphics = event.getGuiGraphics();
        int guiWidth = graphics.guiWidth();
        int airBarY = cachedAirBarY;

        // Draw base bubbles at the surface so they remain visible when
        // vanilla hides the bar (air full, not underwater) while extra
        // layers are still filling. Underwater, vanilla handles the bar
        // with its pop animation — we only draw extra layers there.
        if (!player.isUnderWater()) {
            int baseAir = Math.min(player.getAirSupply(), baseAirSupply);
            int baseBubbles = Mth.ceil((double) baseAir * 10.0 / baseAirSupply);
            if (baseBubbles > 0) {
                drawBubbles(graphics, guiWidth, airBarY, 0, baseBubbles, AIR_SPRITE);
            }
        }

        int totalLayers = Mth.ceil((float) maxExtraAir / baseAirSupply);

        if (currentExtraAir > 0) {
            int layer = (currentExtraAir - 1) / baseAirSupply;
            int topLayerAir = currentExtraAir - layer * baseAirSupply;
            int topLayerBubbles = Mth.ceil((double) topLayerAir * 10.0 / baseAirSupply);

            for (int depth = layer; depth >= 1; depth--) {
                int absLayer = layer - depth;
                int slot = totalLayers - 1 - absLayer;
                drawBubbles(graphics, guiWidth, airBarY, 0, 10, spriteAt(slot));
            }

            int drainingSlot = totalLayers - 1 - layer;
            int emptySlots = 10 - topLayerBubbles;
            drawBubbles(graphics, guiWidth, airBarY, emptySlots, 10, spriteAt(drainingSlot));
        }
    }

    private static void drawBubbles(
            net.minecraft.client.gui.GuiGraphics graphics,
            int guiWidth, int y, int startSlot, int endSlot, ResourceLocation sprite) {
        for (int i = startSlot; i < endSlot; i++) {
            int x = guiWidth / 2 + 91 - 9 - (9 - i) * 8;
            graphics.blitSprite(sprite, x, y, 9, 9);
        }
    }

    private static ResourceLocation spriteAt(int depth) {
        return LAYER_SPRITES[Math.min(depth, LAYER_SPRITES.length - 1)];
    }
}
