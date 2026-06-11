package com.dexer.aquanaut.client;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.client.model.GasFlowMeterReadoutHelper;
import com.dexer.aquanaut.common.AirSupplyHelper;
import com.dexer.aquanaut.common.block.AirPumpBlock;
import com.dexer.aquanaut.common.block.AbstractPipeBlock;
import com.dexer.aquanaut.common.block.entity.AbstractPipeBlockEntity;
import com.dexer.aquanaut.core.ItemRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

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
@SuppressWarnings("removal")
public final class ClientHudEvents {

    private static final ResourceLocation AIR_SPRITE = ResourceLocation.withDefaultNamespace("hud/air");
    private static final int TARGET_READOUT_BG = 0x99E8FFFF;
    private static final int TARGET_READOUT_TEXT = 0xFF12343B;
    private static final double TARGET_READOUT_MAX_DISTANCE_SQ = 25.0D;

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

    /** Air bar Y captured in Pre, used by Post for drawing. */
    private static int cachedAirBarY = 0;

    @SubscribeEvent
    public static void onRenderAirLevelPre(RenderGuiLayerEvent.Pre event) {
        if (!VanillaGuiLayers.AIR_LEVEL.equals(event.getName())) {
            return;
        }
        // Capture Y before vanilla increments rightHeight. Do NOT cancel so that
        // Post still fires (NeoForge only fires Post when Pre is not cancelled).
        Minecraft mc = Minecraft.getInstance();
        cachedAirBarY = mc.getWindow().getGuiScaledHeight() - mc.gui.rightHeight;
    }

    @SubscribeEvent
    public static void onRenderAirLevelPost(RenderGuiLayerEvent.Post event) {
        if (!VanillaGuiLayers.AIR_LEVEL.equals(event.getName()))
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui)
            return;
        LocalPlayer player = mc.player;
        if (player == null || player.isCreative() || player.isSpectator())
            return;

        int maxExtraAir = ClientAirData.getMaxExtraAir();
        if (maxExtraAir <= 0)
            return; // no extra capacity – vanilla already drew everything correctly

        int baseAirSupply = AirSupplyHelper.BASE_AIR_SUPPLY_TICKS;
        int currentExtraAir = ClientAirData.getCurrentExtraAir();
        int totalAir = player.getAirSupply() + currentExtraAir;
        int totalMaxAir = baseAirSupply + maxExtraAir;

        // Hide only when not underwater and everything is full.
        if (!player.isUnderWater() && totalAir >= totalMaxAir)
            return;

        var graphics = event.getGuiGraphics();
        int guiWidth = graphics.guiWidth();
        int airBarY = cachedAirBarY;

        // Base row ─────────────────────────────────────────────────────────────────
        // When UNDERWATER: vanilla's own AIR_LEVEL render already drew the base row.
        // Drawing it again here would double-render bubbles on the same pixels.
        // When NOT underwater: vanilla hides the bar entirely, so we draw it
        // ourselves to give visual context alongside the still-refilling extra layers.
        if (!player.isUnderWater()) {
            // While extra air exists the server keeps airSupply full each tick.
            int visualBaseAir = currentExtraAir > 0
                    ? baseAirSupply
                    : Math.min(player.getAirSupply(), baseAirSupply);
            int baseBubbles = Mth.ceil((double) visualBaseAir * 10.0 / baseAirSupply);
            int baseEmptySlots = 10 - baseBubbles;
            if (baseBubbles > 0) {
                drawBubbles(graphics, guiWidth, airBarY, baseEmptySlots, 10, AIR_SPRITE);
            }
        }

        // Extra layers ─────────────────────────────────────────────────────────────
        if (currentExtraAir > 0) {
            int totalLayers = Mth.ceil((float) maxExtraAir / baseAirSupply);
            int currentLayer = (currentExtraAir - 1) / baseAirSupply; // 0-based, 0 = bottom-most extra layer

            // Draw complete layers below the draining one (back → front).
            for (int depth = currentLayer; depth >= 1; depth--) {
                int absLayer = currentLayer - depth;
                int slot = totalLayers - 1 - absLayer;
                drawBubbles(graphics, guiWidth, airBarY, 0, 10, spriteAt(slot));
            }

            // Draw the actively draining layer on top.
            // Filled bubbles sit on the right; empty slots on the left stay transparent
            // so deeper layers (or the base row) show through.
            int drainingSlot = totalLayers - 1 - currentLayer;
            int airInLayer = currentExtraAir - currentLayer * baseAirSupply;
            int filledBubbles = Mth.ceil((double) airInLayer * 10.0 / baseAirSupply);
            if (filledBubbles > 0) {
                drawBubbles(graphics, guiWidth, airBarY, 10 - filledBubbles, 10, spriteAt(drainingSlot));
            }
        }
    }

    @SubscribeEvent
    public static void onRenderCrosshairPost(RenderGuiLayerEvent.Post event) {
        if (!VanillaGuiLayers.CROSSHAIR.equals(event.getName())) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) {
            return;
        }

        LocalPlayer player = mc.player;
        if (player == null || !player.isUsingItem()) {
            return;
        }

        ItemStack useItem = player.getUseItem();
        if (!useItem.is(ItemRegistry.GAS_FLOW_METER.get())) {
            return;
        }

        HitResult hitResult = mc.hitResult;
        if (!(hitResult instanceof BlockHitResult blockHitResult)) {
            return;
        }

        BlockPos pos = blockHitResult.getBlockPos();
        if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > TARGET_READOUT_MAX_DISTANCE_SQ) {
            return;
        }

        BlockState state = player.level().getBlockState(pos);
        if (state.getBlock() instanceof AirPumpBlock airPump) {
            GasFlowMeterReadoutHelper.Readout readout = GasFlowMeterReadoutHelper.airPump(
                    state.getValue(AirPumpBlock.ACTIVE),
                    airPump.getFlowStrength(state));
            drawTargetReadout(event.getGuiGraphics(), mc.font, readout);
            return;
        }

        if (!(state.getBlock() instanceof AbstractPipeBlock)
                || !(player.level().getBlockEntity(pos) instanceof AbstractPipeBlockEntity pipeEntity)) {
            return;
        }

        GasFlowMeterReadoutHelper.Readout readout = GasFlowMeterReadoutHelper.airPipe(pipeEntity.getFlow());
        drawTargetReadout(event.getGuiGraphics(), mc.font, readout);
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

    private static void drawTargetReadout(GuiGraphics graphics, Font font, GasFlowMeterReadoutHelper.Readout readout) {
        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;

        int titleWidth = font.width(readout.title());
        int valueWidth = font.width(readout.value());
        int boxWidth = Math.max(titleWidth, valueWidth) + 12;
        int boxHeight = font.lineHeight * 2 + 10;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - 28;
        int textX = centerX;
        int titleY = boxY + 4;
        int valueY = titleY + font.lineHeight + 1;

        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, TARGET_READOUT_BG);
        graphics.drawString(font, readout.title(), textX - titleWidth / 2, titleY, TARGET_READOUT_TEXT, false);
        graphics.drawString(font, readout.value(), textX - valueWidth / 2, valueY, TARGET_READOUT_TEXT, false);
    }
}
