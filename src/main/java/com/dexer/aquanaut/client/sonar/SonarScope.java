package com.dexer.aquanaut.client.sonar;

import java.util.Collection;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.sonar.SonarPlot;
import com.dexer.aquanaut.common.sonar.SonarPulse;
import com.dexer.aquanaut.common.sonar.SonarReturn;
import com.dexer.aquanaut.core.ItemRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * The scope: the instrument's own screen, in the corner of the diver's.
 *
 * <p>
 * It is a plan position indicator, the dial every sounder has drawn since the Second World War. The
 * transducer is in the middle, bearing runs around it, range runs outwards, and a contact is a dot
 * where the two meet — coloured by its voice, with a stem showing whether it lies above or below
 * the diver, because height is the one thing a plan view cannot say. A dial that faced the world
 * instead of the diver would be unreadable the moment they turned their head; this one turns with
 * them, so "it is up the screen" always means "it is in front of me".
 *
 * <p>
 * The face itself is a baked sprite — rings, ticks, bezel and all — and everything that moves is
 * laid over it: the wavefront running out as a ring of light with a phosphor trail decaying behind
 * it, a blip for every contact the wave has reached, a ring of its own for one that has only just
 * answered, and a slow arm sweeping the dial while the instrument is in hand and quiet. Drawing the
 * face a scanline at a time would be four hundred quads a frame for a circle that never changes;
 * the sprite is four.
 *
 * <p>
 * The whole panel fades in and out rather than appearing: it is on screen while a reading lasts and
 * while the instrument is in hand, and gone otherwise, because a permanent picture in the corner of
 * the eye is a worse instrument than no instrument at all.
 */
@EventBusSubscriber(modid = Aquanaut.MODID, value = Dist.CLIENT)
public final class SonarScope {

    private static final ResourceLocation DIAL = ResourceLocation.fromNamespaceAndPath(
            Aquanaut.MODID, "hud/sonar_scope");

    // ------------------------------------------------------------------
    // the panel
    // ------------------------------------------------------------------

    private static final int DIAL_SIZE = 72;

    /** Where twenty blocks plots: inside the glass, so the bezel is never drawn on. */
    private static final float PLOT_RADIUS = 30.0F;

    /** How far from the screen edge the panel sits. */
    private static final int MARGIN = 5;

    private static final int PAD = 5;
    /** Wide enough for a contact's name and its distance on one line, and no wider. */
    private static final int PANEL_WIDTH = DIAL_SIZE + 32;
    private static final int HEADER_HEIGHT = 12;

    // ------------------------------------------------------------------
    // colour
    // ------------------------------------------------------------------

    private static final int PANEL_BG = 0xE6081116;
    private static final int PANEL_EDGE = 0xFF24414C;
    private static final int PANEL_RULE = 0xFF16282F;
    private static final int HEADER_TEXT = 0xFF7C98A2;
    private static final int RANGE_TEXT = 0xFF74DCF0;
    private static final int HEADING_TEXT = 0xFF5C8E9C;
    private static final int DIAL_GLOW = 0xFF9FEDFF;
    private static final int STEM_TEXT = 0xFF2E5A66;

    /** How fast the standby arm turns, in degrees per tick. Four seconds to the revolution. */
    private static final float IDLE_SWEEP = 4.5F;

    /** How many degrees behind the arm its tail reaches. */
    private static final float IDLE_TAIL = 26.0F;

    /** How many range rings the trail behind the wavefront leaves behind it. */
    private static final int TRAIL = 4;

    /** How far apart the trail's rings are, as a fraction of the wavefront's radius. */
    private static final float TRAIL_GAP = 0.075F;

    /** Response time of the panel's fade, in ticks. */
    private static final float FADE_TICKS = 3.0F;

    /** Smoothed visibility of the whole panel. */
    private static float shown;

    private SonarScope() {
    }

    @SubscribeEvent
    public static void onRenderHud(RenderGuiLayerEvent.Post event) {
        if (!VanillaGuiLayers.HOTBAR.equals(event.getName())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (minecraft.options.hideGui || player == null || minecraft.level == null) {
            return;
        }

        // The scope is on screen while the instrument is in hand. What is *on* it is a separate
        // question: a reading stays until that instrument fires again, whether or not anyone is
        // looking at it, which is what a sounder has always done.
        float target = held(player).isEmpty() ? 0.0F : 1.0F;
        float delta = minecraft.getTimer().getRealtimeDeltaTicks();
        shown += (target - shown) * (1.0F - (float) Math.exp(-delta / FADE_TICKS));

        if (shown < 0.02F) {
            shown = target < 0.5F ? 0.0F : shown;
            return;
        }

        draw(event.getGuiGraphics(), minecraft, player,
                minecraft.getTimer().getGameTimeDeltaPartialTick(false));
    }

    private static void draw(GuiGraphics graphics, Minecraft minecraft, LocalPlayer player, float partialTick) {
        Font font = minecraft.font;
        int height = HEADER_HEIGHT + DIAL_SIZE + PAD;
        int panelX = graphics.guiWidth() - MARGIN - PANEL_WIDTH;
        int panelY = graphics.guiHeight() - MARGIN - height;

        // Everything below is drawn at full strength; one shader colour carries the fade.
        graphics.setColor(1.0F, 1.0F, 1.0F, shown);

        panel(graphics, panelX, panelY, height);
        header(graphics, font, panelX, panelY);

        int dialX = panelX + (PANEL_WIDTH - DIAL_SIZE) / 2;
        int dialY = panelY + HEADER_HEIGHT;
        float centreX = dialX + DIAL_SIZE / 2.0F;
        float centreY = dialY + DIAL_SIZE / 2.0F;

        graphics.blitSprite(DIAL, dialX, dialY, DIAL_SIZE, DIAL_SIZE);

        graphics.enableScissor(dialX, dialY, dialX + DIAL_SIZE, dialY + DIAL_SIZE);
        try {
            dial(graphics, font, player, partialTick, centreX, centreY);
        } finally {
            graphics.disableScissor();
        }

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    // ------------------------------------------------------------------
    // the housing
    // ------------------------------------------------------------------

    private static void panel(GuiGraphics graphics, int x, int y, int height) {
        graphics.fill(x, y, x + PANEL_WIDTH, y + height, PANEL_BG);
        graphics.fill(x, y, x + PANEL_WIDTH, y + 1, PANEL_EDGE);
        graphics.fill(x, y + height - 1, x + PANEL_WIDTH, y + height, PANEL_EDGE);
        graphics.fill(x, y, x + 1, y + height, PANEL_EDGE);
        graphics.fill(x + PANEL_WIDTH - 1, y, x + PANEL_WIDTH, y + height, PANEL_EDGE);
        graphics.fill(x + 1, y + HEADER_HEIGHT - 3, x + PANEL_WIDTH - 1, y + HEADER_HEIGHT - 2, PANEL_RULE);
    }

    /**
     * The plate across the top: what the instrument is on the left, what it reaches on the right.
     *
     * <p>
     * A fixed label rather than the stack's own name, because the item's name does not fit beside a
     * range in a panel this size and a name that was truncated halfway would be worse than none.
     */
    private static void header(GuiGraphics graphics, Font font, int x, int y) {
        Component title = Component.translatable("hud.aquanaut.sonar.title");
        Component range = Component.translatable("hud.aquanaut.sonar.range", (int) SonarPulse.RANGE);

        graphics.drawString(font, title, x + PAD, y + 3, HEADER_TEXT, false);
        graphics.drawString(font, range, x + PANEL_WIDTH - PAD - font.width(range), y + 3, RANGE_TEXT, false);
    }

    // ------------------------------------------------------------------
    // the dial
    // ------------------------------------------------------------------

    /**
     * The glass, and everything on it.
     *
     * <p>
     * Nothing here says what a contact <em>is</em>. A sounder reports water that answered: a
     * bearing, a range and a voice — a colour and a sound the diver learns to read the way they
     * learn the sound of their own regulator. There is no list of names, no distances in figures and
     * no legend, because the instrument has never known what it was looking at and pretending
     * otherwise would take the reading away from the diver and hand it to the screen.
     */
    private static void dial(GuiGraphics graphics, Font font, LocalPlayer player, float partialTick,
            float centreX, float centreY) {
        float yaw = player.getViewYRot(partialTick);
        Vec3 eye = player.getEyePosition(partialTick);
        Collection<ClientSonarData.Picture> pictures = ClientSonarData.pictures();

        heading(graphics, font, yaw, centreX, centreY);

        if (pictures.isEmpty()) {
            // Nothing has ever been sounded: the arm sweeps and the dial waits.
            idleSweep(graphics, player, partialTick, centreX, centreY);
            return;
        }

        for (ClientSonarData.Picture picture : pictures) {
            float age = picture.age(partialTick);

            if (picture.isSweeping()) {
                sweep(graphics, age, centreX, centreY);
            }

            for (SonarReturn contact : picture.returns()) {
                blip(graphics, picture, contact, age, yaw, eye, centreX, centreY);
            }
        }
    }

    /**
     * The heading, inside the foot of the dial. The plot turns with the diver, so without it the
     * dial says "something is thirty degrees off the bow" and nothing about which way that is.
     */
    private static void heading(GuiGraphics graphics, Font font, float yaw, float centreX, float centreY) {
        float heading = SonarPlot.heading(yaw);
        String text = SonarPlot.compassPoint(heading) + "\u00B0 " + Mth.floor(heading);
        int width = font.width(text);
        graphics.drawString(font, text, Mth.floor(centreX) - width / 2,
                Mth.floor(centreY + PLOT_RADIUS) - 10, HEADING_TEXT, false);
    }

    /**
     * The wavefront going out, with the phosphor it has already lit decaying behind it: a ring of
     * light and the ghost of every ring before it, which is what a screen does and what tells the
     * eye which way the pulse is travelling.
     */
    private static void sweep(GuiGraphics graphics, float age, float centreX, float centreY) {
        double radius = SonarPulse.wavefrontRadius(age) / SonarPulse.RANGE * PLOT_RADIUS;
        float glow = SonarPulse.waveGlow(age);

        for (int step = TRAIL; step >= 0; step--) {
            float reach = (float) radius * (1.0F - step * TRAIL_GAP);

            if (reach < 1.0F) {
                continue;
            }

            float alpha = step == 0 ? glow : glow * 0.3F * (1.0F - step / (float) (TRAIL + 1));
            circle(graphics, centreX, centreY, reach, alpha(alpha * 0.8F, DIAL_GLOW));
        }
    }

    /** The standby arm: a slow sweep with a tail, so a fresh instrument still looks powered. */
    private static void idleSweep(GuiGraphics graphics, LocalPlayer player, float partialTick,
            float centreX, float centreY) {
        float angle = (player.level().getGameTime() + partialTick) * IDLE_SWEEP;

        for (int step = 0; step <= 4; step++) {
            float behind = angle - step * (IDLE_TAIL / 4.0F);
            float strength = 0.4F * (1.0F - step / 5.0F);
            glow(graphics, centreX, centreY, behind, PLOT_RADIUS, strength);
        }
    }

    private static void glow(GuiGraphics graphics, float centreX, float centreY, float degrees, float radius,
            float strength) {
        double radians = Math.toRadians(degrees);
        line(graphics, centreX, centreY,
                centreX + (float) Math.sin(radians) * radius,
                centreY - (float) Math.cos(radians) * radius,
                alpha(strength, DIAL_GLOW));
    }

    /** One contact: where it is now, which way up it is, and how recently it answered. */
    private static void blip(GuiGraphics graphics, ClientSonarData.Picture picture, SonarReturn contact,
            float age, float yaw, Vec3 eye, float centreX, float centreY) {
        if (!SonarPulse.hasReached(age, contact.distance())) {
            return;
        }

        // The contact is anchored to the rock it came off, not to the water the diver happened to be
        // treading when they fired, so it is resolved against where they are now: swim and the whole
        // plot slides correctly, turn and the dial swings with them.
        Vec3 track = withinRange(picture.track(contact, eye));
        float x = SonarPlot.plotX(track, yaw, centreX, PLOT_RADIUS);
        float y = SonarPlot.plotY(track, yaw, centreY, PLOT_RADIUS);
        float stem = SonarPlot.stem(track, PLOT_RADIUS);
        float glow = SonarPulse.echoGlow(age, contact.distance(), contact.strength());
        int colour = contact.signal().colour;

        // The height stem: long and dim below the diver, where the fish are, and shorter above.
        if (Math.abs(stem) > 1.0F) {
            line(graphics, x, y, x, y + stem, alpha(0.55F * glow, STEM_TEXT));
        }

        // Two layers of glow around a hard centre: the wide one carries the return across the dial
        // without the dot itself having to grow, which is how a lit point actually reads.
        disc(graphics, x, y, 3.0F, alpha(0.16F * glow, colour));
        disc(graphics, x, y, 1.8F, alpha(0.34F * glow, colour));

        int core = alpha(Math.min(1.0F, 0.42F + 0.58F * glow), colour);
        graphics.fill(Mth.floor(x) - 1, Mth.floor(y) - 1, Mth.floor(x) + 2, Mth.floor(y) + 2, core);
        graphics.fill(Mth.floor(x), Mth.floor(y), Mth.floor(x) + 1, Mth.floor(y) + 1,
                alpha(Math.min(1.0F, 0.45F + 0.55F * glow), 0xFFFFFF));

        // A ring for a contact that has only just answered, so a fresh one is unmistakable.
        float run = SonarPulse.echoProgress(age, contact.distance());

        if (run < 1.0F) {
            float fresh = 1.0F - run;
            circle(graphics, x, y, 2.0F + 9.0F * run, alpha(0.75F * fresh * fresh, colour));
        }
    }

    /**
     * Pins a contact to the rim once the diver has swum out past its range. A real set does the same
     * thing: the echo is still on the books, and where it sits on the edge of the dial says "out
     * there" rather than pretending it is somewhere it is not. Only the range is pinned — the height
     * is left alone, because the stem is the one thing the rim cannot say.
     */
    private static Vec3 withinRange(Vec3 track) {
        Vec3 flat = new Vec3(track.x, 0.0D, track.z);
        double planar = flat.length();

        if (planar <= SonarPulse.RANGE) {
            return track;
        }

        Vec3 pinned = flat.scale(SonarPulse.RANGE / planar);
        return new Vec3(pinned.x, track.y, pinned.z);
    }

    // ------------------------------------------------------------------
    // drawing primitives
    // ------------------------------------------------------------------

    /**
     * A one-pixel circle outline, drawn a scanline at a time from the middle outwards: two short
     * runs per row is the whole cost, against the hundreds of quads a naive walk of the bounding
     * box would spend to fill in everything but the edge.
     */
    private static void circle(GuiGraphics graphics, float centreX, float centreY, float radius, int colour) {
        if (radius < 1.0F || (colour >>> 24) < 4) {
            return;
        }

        int middle = Mth.floor(centreX);
        int top = Mth.ceil(centreY - radius);
        int bottom = Mth.floor(centreY + radius);

        for (int y = top; y <= bottom; y++) {
            float dy = y + 0.5F - centreY;
            float span = radius * radius - dy * dy;

            if (span < 0.0F) {
                continue;
            }

            int half = Mth.floor(Mth.sqrt(span));
            graphics.fill(middle - half, y, middle - half + 1, y + 1, colour);
            graphics.fill(middle + half, y, middle + half + 1, y + 1, colour);
        }
    }

    private static void disc(GuiGraphics graphics, float centreX, float centreY, float radius, int colour) {
        if ((colour >>> 24) < 4) {
            return;
        }

        int middle = Mth.floor(centreX);
        int top = Mth.ceil(centreY - radius);
        int bottom = Mth.floor(centreY + radius);

        for (int y = top; y <= bottom; y++) {
            float dy = y + 0.5F - centreY;
            float span = radius * radius - dy * dy;

            if (span < 0.0F) {
                continue;
            }

            int half = Mth.floor(Mth.sqrt(span));
            graphics.fill(middle - half, y, middle + half + 1, y + 1, colour);
        }
    }

    private static void line(GuiGraphics graphics, float fromX, float fromY, float toX, float toY, int colour) {
        if ((colour >>> 24) < 4) {
            return;
        }

        float dx = toX - fromX;
        float dy = toY - fromY;
        int steps = Mth.ceil(Math.max(Math.abs(dx), Math.abs(dy)));

        if (steps <= 0) {
            return;
        }

        for (int step = 0; step <= steps; step++) {
            float t = step / (float) steps;
            int x = Mth.floor(fromX + dx * t);
            int y = Mth.floor(fromY + dy * t);
            graphics.fill(x, y, x + 1, y + 1, colour);
        }
    }

    /** A voice's colour at a given strength, as the packed ARGB the HUD draws with. */
    private static int alpha(float strength, int rgb) {
        int value = Mth.clamp((int) (strength * 255.0F), 0, 255);
        return value << 24 | (rgb & 0xFFFFFF);
    }

    // ------------------------------------------------------------------

    /** The sonar in the diver's hands, main hand first. */
    private static ItemStack held(LocalPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);

            if (stack.is(ItemRegistry.PORTABLE_SONAR.get())) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }
}
