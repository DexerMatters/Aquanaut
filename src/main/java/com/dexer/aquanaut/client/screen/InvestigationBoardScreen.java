package com.dexer.aquanaut.client.screen;

import com.dexer.aquanaut.client.ClientInvestigationData;
import com.dexer.aquanaut.common.investigation.InvestigationCatalog;
import com.dexer.aquanaut.common.investigation.InvestigationLink;
import com.dexer.aquanaut.common.investigation.InvestigationNode;
import com.dexer.aquanaut.common.investigation.InvestigationProgress;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Fixed whiteboard UI whose directed graph can be panned and zoomed independently. */
public final class InvestigationBoardScreen extends Screen {
    /** Light vanilla-style screen dimming: the running world remains clearly visible behind the board. */
    /** Vanilla container chrome: the same panel, outline and bevels a chest or furnace uses. */
    private static final ResourceLocation PANEL_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");
    /** The panel field, left a little see-through so the world stays faintly visible behind it. */
    private static final int PANEL = 0xEAC6C6C6;
    private static final int OUTLINE = 0xFF000000;
    private static final int EDGE_LIGHT = 0xFFFFFFFF;
    private static final int EDGE_DARK = 0xFF555555;
    /** Recessed wells use the vanilla slot recipe. */
    private static final int WELL = 0xFF8B8B8B;
    private static final int WELL_EDGE_DARK = 0xFF373737;
    private static final int WELL_EDGE_LIGHT = 0xFFFFFFFF;
    /** Vanilla label ink, plus a light ink for anything sitting on the dark graph surface. */
    private static final int LABEL = 0xFF404040;
    private static final int BOARD_INK = 0xFFDDE5E7;
    private static final int SHADE_TOP = 0x48000000;
    private static final int SHADE_BOTTOM = 0x60000000;
    /**
     * The graph surface. Darker than the old whiteboard and more see-through, so the board reads
     * as smoked glass over the world rather than a painted panel.
     */
    private static final int BOARD = 0x8A1B2126;
    private static final int BOARD_GRID = 0x1EFFFFFF;
    private static final int BOARD_TOP = 0x14FFFFFF;
    private static final int INK = 0xFF2C241C;
    private static final int PAPER = 0xFFE4D6AE;
    private static final int PAPER_LIGHT = 0xFFF2E7C7;
    private static final int PAPER_SHADOW = 0xFF8B704B;
    private static final int BLUEPRINT = 0xFF7799A4;
    private static final int WARNING = 0xFFD2A85D;
    private static final int RED_STRING = 0xFFC3463C;
    private static final int GOLD = 0xFFFFCC48;
    private static final int GOLD_DARK = 0xFF9A6B20;
    private static final int DETAIL_INK = 0xFF263238;
    private static final int MUTED = 0xFF4E5658;
    private static final int MIN_BOARD_WIDTH = 300;
    private static final int MIN_BOARD_HEIGHT = 190;
    private static final int DEFAULT_BOARD_WIDTH = 640;
    private static final int DEFAULT_BOARD_HEIGHT = 390;
    /** Positions use a roomier virtual canvas while card sizes stay readable. */
    private static final double GRAPH_SPREAD = 1.18D;
    private static final int NODE_EDGE_GUARD = 3;

    private ResourceLocation selectedNode;
    private ResourceLocation selectedLink;
    private int detailScroll;
    private double graphZoom = 1.0D;
    private double graphPanX;
    private double graphPanY;
    private boolean draggingGraph;

    public InvestigationBoardScreen() {
        super(Component.translatable("gui.aquanaut.investigation.title"));
        selectedNode = InvestigationCatalog.nodes().stream().findFirst().map(InvestigationNode::id).orElse(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, width, height, SHADE_TOP, SHADE_BOTTOM);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        Layout layout = layout();
        InvestigationProgress progress = ClientInvestigationData.get();
        Map<ResourceLocation, Rect> nodes = nodeRects(layout);

        drawFrame(graphics, layout);
        drawHeader(graphics, layout);
        drawWhiteboard(graphics, layout);

        graphics.enableScissor(layout.graph.x, layout.graph.y, layout.graph.right(), layout.graph.bottom());
        for (InvestigationLink link : InvestigationCatalog.links()) {
            drawLink(graphics, link, nodes, progress, mouseX, mouseY);
        }
        for (InvestigationNode node : InvestigationCatalog.nodes()) {
            drawNode(graphics, node, nodes.get(node.id()), progress, mouseX, mouseY);
        }
        drawGraphScale(graphics, layout);
        graphics.disableScissor();

        drawDetails(graphics, layout, progress);
        drawFooter(graphics, layout);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        Layout layout = layout();
        if (!layout.graph.contains(mouseX, mouseY)) return super.mouseClicked(mouseX, mouseY, button);

        Map<ResourceLocation, Rect> rects = nodeRects(layout);
        // Cards win over strings where they overlap.
        for (InvestigationNode node : InvestigationCatalog.nodes()) {
            if (rects.get(node.id()).contains(mouseX, mouseY)) {
                selectedNode = node.id();
                selectedLink = null;
                detailScroll = 0;
                return true;
            }
        }
        // Thread and arrow hitboxes stop before the card edge, so a line passing under a card can
        // never steal focus from that card.
        if (isOverAnyNode(rects, mouseX, mouseY, NODE_EDGE_GUARD)) {
            return true;
        }
        InvestigationLink nearest = null;
        double nearestDistance = 8.0D;
        InvestigationProgress progress = ClientInvestigationData.get();
        for (InvestigationLink link : InvestigationCatalog.links()) {
            if (!isLinkVisible(progress, link)) continue;
            Rect from = rects.get(link.from());
            Rect to = rects.get(link.to());
            double distance = distanceToSegment(mouseX, mouseY, from.centerX(), from.centerY(), to.centerX(), to.centerY());
            if (distance < nearestDistance) {
                nearest = link;
                nearestDistance = distance;
            }
        }
        if (nearest != null) {
            selectedLink = nearest.id();
            selectedNode = null;
            detailScroll = 0;
            return true;
        }
        draggingGraph = true;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingGraph) {
            Layout layout = layout();
            graphPanX = Math.clamp(graphPanX + dragX, -layout.graph.w * 0.75D, layout.graph.w * 0.75D);
            graphPanY = Math.clamp(graphPanY + dragY, -layout.graph.h * 0.75D, layout.graph.h * 0.75D);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingGraph) {
            draggingGraph = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Layout layout = layout();
        if (layout.details.contains(mouseX, mouseY)) {
            detailScroll = Math.max(0, detailScroll - (int) Math.signum(scrollY) * 14);
            return true;
        }
        if (layout.graph.contains(mouseX, mouseY) && scrollY != 0.0D) {
            double oldZoom = graphZoom;
            graphZoom = Math.clamp(graphZoom + Math.signum(scrollY) * 0.10D, 0.65D, 1.65D);
            double centerX = layout.graph.centerX();
            double centerY = layout.graph.centerY();
            double localX = (mouseX - centerX - graphPanX) / oldZoom;
            double localY = (mouseY - centerY - graphPanY) / oldZoom;
            graphPanX = mouseX - centerX - localX * graphZoom;
            graphPanY = mouseY - centerY - localY * graphZoom;
            graphPanX = Math.clamp(graphPanX, -layout.graph.w * 0.75D, layout.graph.w * 0.75D);
            graphPanY = Math.clamp(graphPanY, -layout.graph.h * 0.75D, layout.graph.h * 0.75D);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private Layout layout() {
        int availableWidth = Math.max(1, width - 24);
        int availableHeight = Math.max(1, height - 24);
        int boardWidth = Math.min(DEFAULT_BOARD_WIDTH,
                Math.max(Math.min(MIN_BOARD_WIDTH, availableWidth), width * 3 / 4));
        int boardHeight = Math.min(DEFAULT_BOARD_HEIGHT,
                Math.max(Math.min(MIN_BOARD_HEIGHT, availableHeight), height * 4 / 5));
        Rect outer = new Rect((width - boardWidth) / 2, (height - boardHeight) / 2,
                boardWidth, boardHeight);
        int header = 31;
        int footer = 18;
        int detailsWidth = Mth.clamp(outer.w * 30 / 100, 130, 230);
        if (outer.w < 460) detailsWidth = Math.max(112, outer.w * 36 / 100);
        Rect graph = new Rect(outer.x + 11, outer.y + header + 5,
                outer.w - detailsWidth - 25, outer.h - header - footer - 10);
        Rect details = new Rect(graph.right() + 7, graph.y, detailsWidth, graph.h);
        return new Layout(outer, graph, details);
    }

    private Map<ResourceLocation, Rect> nodeRects(Layout layout) {
        Map<ResourceLocation, Rect> result = new LinkedHashMap<>();
        int baseNodeW = Mth.clamp(layout.graph.w / 6, 52, 90);
        int baseNodeH = layout.graph.h < 180 ? 32 : layout.graph.h < 260 ? 40 : 50;
        int nodeW = Math.max(46, (int) Math.round(baseNodeW * graphZoom));
        int nodeH = Math.max(30, (int) Math.round(baseNodeH * graphZoom));
        int usableW = Math.max(1, layout.graph.w - baseNodeW - 14);
        int usableH = Math.max(1, layout.graph.h - baseNodeH - 14);
        for (InvestigationNode node : InvestigationCatalog.nodes()) {
            double baseCenterX = layout.graph.x + 7 + node.x() * usableW / 1000.0D + baseNodeW / 2.0D;
            double baseCenterY = layout.graph.y + 7 + node.y() * usableH / 620.0D + baseNodeH / 2.0D;
            int x = (int) Math.round(layout.graph.centerX()
                    + (baseCenterX - layout.graph.centerX()) * graphZoom * GRAPH_SPREAD
                    + graphPanX - nodeW / 2.0D);
            int y = (int) Math.round(layout.graph.centerY()
                    + (baseCenterY - layout.graph.centerY()) * graphZoom * GRAPH_SPREAD
                    + graphPanY - nodeH / 2.0D);
            result.put(node.id(), new Rect(x, y, nodeW, nodeH));
        }
        return result;
    }

    private void drawFrame(GuiGraphics graphics, Layout layout) {
        Rect r = layout.outer;
        graphics.fill(r.x + 4, r.y + 5, r.right() + 4, r.bottom() + 5, 0x77000000);
        drawPanel(graphics, r.x, r.y, r.right(), r.bottom());
    }

    /**
     * The vanilla container panel: a 1px black outline, a 2px light bevel down the top and left
     * and a matching dark bevel down the bottom and right. The rounded corners are blitted straight
     * from the crafting-table sheet, so they are byte-identical to vanilla instead of approximated.
     */
    private static void drawPanel(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        graphics.fill(x1, y1, x2, y2, PANEL);
        graphics.fill(x1, y1, x2, y1 + 1, OUTLINE);
        graphics.fill(x1, y1, x1 + 1, y2, OUTLINE);
        graphics.fill(x2 - 1, y1, x2, y2, OUTLINE);
        graphics.fill(x1, y2 - 1, x2, y2, OUTLINE);
        graphics.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 3, EDGE_LIGHT);
        graphics.fill(x1 + 1, y1 + 1, x1 + 3, y2 - 1, EDGE_LIGHT);
        graphics.fill(x1 + 1, y2 - 3, x2 - 1, y2 - 1, EDGE_DARK);
        graphics.fill(x2 - 3, y1 + 1, x2 - 1, y2 - 1, EDGE_DARK);
        graphics.blit(PANEL_TEXTURE, x1, y1, 0, 0, 4, 4);
        graphics.blit(PANEL_TEXTURE, x2 - 4, y1, 172, 0, 4, 4);
        graphics.blit(PANEL_TEXTURE, x1, y2 - 4, 0, 162, 4, 4);
        graphics.blit(PANEL_TEXTURE, x2 - 4, y2 - 4, 172, 162, 4, 4);
    }

    /** A recessed vanilla well, using the same recipe as a slot: dark top/left, light bottom/right. */
    private static void drawWell(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        graphics.fill(x1, y1, x2, y2, WELL);
        graphics.fill(x1, y1, x2, y1 + 1, WELL_EDGE_DARK);
        graphics.fill(x1, y1, x1 + 1, y2, WELL_EDGE_DARK);
        graphics.fill(x1, y2 - 1, x2, y2, WELL_EDGE_LIGHT);
        graphics.fill(x2 - 1, y1, x2, y2, WELL_EDGE_LIGHT);
    }

    private void drawHeader(GuiGraphics graphics, Layout layout) {
        int x = layout.outer.x + 13;
        int y = layout.outer.y + 10;
        graphics.drawString(font, Component.translatable("gui.aquanaut.investigation.title"), x, y, LABEL, false);
        graphics.fill(layout.outer.x + 8, layout.outer.y + 27, layout.outer.right() - 8, layout.outer.y + 28,
                EDGE_DARK);
        graphics.fill(layout.outer.x + 8, layout.outer.y + 28, layout.outer.right() - 8, layout.outer.y + 29,
                EDGE_LIGHT);
    }

    private void drawWhiteboard(GuiGraphics graphics, Layout layout) {
        Rect r = layout.graph;
        drawWell(graphics, r.x - 2, r.y - 2, r.right() + 2, r.bottom() + 2);
        graphics.fill(r.x, r.y, r.right(), r.bottom(), BOARD);
        for (int y = r.y + 18; y < r.bottom(); y += 28) {
            for (int x = r.x + 18; x < r.right(); x += 28) {
                graphics.fill(x, y, Math.min(x + 1, r.right()), Math.min(y + 1, r.bottom()), BOARD_GRID);
            }
        }
        graphics.fill(r.x, r.y, r.right(), r.y + 2, BOARD_TOP);
        graphics.fill(r.x, r.bottom() - 2, r.right(), r.bottom(), 0x90AEB8B5);
    }

    private void drawLink(GuiGraphics graphics, InvestigationLink link, Map<ResourceLocation, Rect> nodes,
            InvestigationProgress progress, int mouseX, int mouseY) {
        if (!isLinkVisible(progress, link)) return;
        Rect from = nodes.get(link.from());
        Rect to = nodes.get(link.to());
        int x1 = from.centerX();
        int y1 = from.centerY();
        int x2 = to.centerX();
        int y2 = to.centerY();
        boolean selected = link.id().equals(selectedLink);
        boolean hovered = !isOverAnyNode(nodes, mouseX, mouseY, NODE_EDGE_GUARD)
                && distanceToSegment(mouseX, mouseY, x1, y1, x2, y2) < 7.0D;
        int color = selected || hovered ? 0xFFFF7668 : RED_STRING;
        drawPixelLine(graphics, x1, y1, x2, y2, color, selected ? 3 : 2);
        drawArrow(graphics, x1, y1, x2, y2, color);
    }

    private void drawNode(GuiGraphics graphics, InvestigationNode node, Rect r,
            InvestigationProgress progress, int mouseX, int mouseY) {
        int stars = progress.stars(node);
        boolean discovered = stars > 0;
        boolean selected = node.id().equals(selectedNode);
        boolean hovered = r.contains(mouseX, mouseY);
        int paperColor = switch (node.tone()) {
            case PAPER -> PAPER;
            case BLUEPRINT -> BLUEPRINT;
            case WARNING -> WARNING;
        };
        if (!discovered) paperColor = 0xFF625D50;

        graphics.fill(r.x + 3, r.y + 4, r.right() + 3, r.bottom() + 4, 0x66000000);
        graphics.fill(r.x - (selected ? 2 : 1), r.y - (selected ? 2 : 1),
                r.right() + (selected ? 2 : 1), r.bottom() + (selected ? 2 : 1),
                selected ? GOLD : hovered ? PAPER_LIGHT : PAPER_SHADOW);
        graphics.fill(r.x, r.y, r.right(), r.bottom(), paperColor);
        graphics.fill(r.x + 2, r.y + 2, r.right() - 2, r.y + 4,
                discovered ? 0x33FFFFFF : 0x22000000);
        // Square magnet and folded lower corner.
        graphics.fill(r.centerX() - 3, r.y - 2, r.centerX() + 4, r.y + 4, OUTLINE);
        graphics.fill(r.centerX() - 2, r.y - 1, r.centerX() + 3, r.y + 3,
                node.tone() == InvestigationNode.Tone.WARNING ? RED_STRING : 0xFF397D91);
        graphics.fill(r.right() - 7, r.bottom() - 1, r.right(), r.bottom(), PAPER_SHADOW);
        graphics.fill(r.right() - 1, r.bottom() - 7, r.right(), r.bottom(), PAPER_SHADOW);

        String title = discovered ? Component.translatable(node.titleKey()).getString()
                : Component.translatable("gui.aquanaut.investigation.unknown").getString();
        drawClamped(graphics, title, r.x + 6, r.y + 9, r.w - 12, discovered ? INK : 0xFFBBB5A6);
        int starY = r.bottom() - 12;
        int totalWidth = node.maxStars() * 9 - 2;
        int starX = r.centerX() - totalWidth / 2;
        for (int i = 0; i < node.maxStars(); i++) {
            drawStar(graphics, starX + i * 9, starY, i < stars ? GOLD : GOLD_DARK, i < stars);
        }
    }

    private void drawDetails(GuiGraphics graphics, Layout layout, InvestigationProgress progress) {
        Rect r = layout.details;
        drawWell(graphics, r.x, r.y, r.right(), r.bottom());
        graphics.enableScissor(r.x + 5, r.y + 5, r.right() - 5, r.bottom() - 5);

        int x = r.x + 11;
        int y = r.y + 10 - detailScroll;
        int contentWidth = r.w - 22;
        InvestigationNode node = InvestigationCatalog.node(selectedNode).orElse(null);
        InvestigationLink link = InvestigationCatalog.link(selectedLink).orElse(null);
        if (node != null) {
            y = drawNodeDetails(graphics, node, progress, x, y, contentWidth);
        } else if (link != null) {
            y = drawLinkDetails(graphics, link, progress, x, y, contentWidth);
        } else {
            graphics.drawString(font, Component.translatable("gui.aquanaut.investigation.select_hint"),
                    x, y, MUTED, false);
        }
        graphics.disableScissor();

        int contentBottom = y + detailScroll + 8;
        int max = Math.max(0, contentBottom - (r.bottom() - 6));
        detailScroll = Math.min(detailScroll, max);
        if (max > 0) drawScrollBar(graphics, r, max);
    }

    private int drawNodeDetails(GuiGraphics graphics, InvestigationNode node, InvestigationProgress progress,
            int x, int y, int width) {
        int stars = progress.stars(node);
        boolean known = stars > 0;
        graphics.drawString(font, Component.translatable("gui.aquanaut.investigation.node_label"), x, y,
                0xFF286675, false);
        y += 14;
        Component title = known ? Component.translatable(node.titleKey())
                : Component.translatable("gui.aquanaut.investigation.unknown");
        y = drawWrapped(graphics, title, x, y, width, DETAIL_INK, 2);
        y += 5;
        for (int i = 0; i < node.maxStars(); i++) drawStar(graphics, x + i * 11, y, i < stars ? GOLD : GOLD_DARK, i < stars);
        graphics.drawString(font, stars + " / " + node.maxStars(), x + node.maxStars() * 11 + 4, y,
                stars == node.maxStars() ? GOLD : MUTED, false);
        y += 17;
        drawDivider(graphics, x, y, width);
        y += 8;

        if (!known) {
            y = drawWrapped(graphics, Component.translatable("gui.aquanaut.investigation.locked_detail"),
                    x, y, width, MUTED, 8);
            return y;
        }
        graphics.drawString(font, Component.translatable("gui.aquanaut.investigation.findings"), x, y,
                0xFF765B24, false);
        y += 13;
        y = drawWrapped(graphics, Component.translatable(node.summaryKey()), x, y, width, 0xFF364448, 8);
        y += 5;
        graphics.drawString(font, Component.translatable("gui.aquanaut.investigation.questions"), x, y,
                0xFF9D3E38, false);
        y += 13;
        y = drawWrapped(graphics, Component.literal("? ").append(Component.translatable(node.questionKey())),
                x, y, width, 0xFF624324, 8);
        return y;
    }

    private int drawLinkDetails(GuiGraphics graphics, InvestigationLink link, InvestigationProgress progress,
            int x, int y, int width) {
        boolean known = progress.isDiscovered(link);
        graphics.drawString(font, Component.translatable("gui.aquanaut.investigation.link_label"), x, y,
                0xFF9D3E38, false);
        y += 14;
        y = drawWrapped(graphics, Component.translatable(link.titleKey()), x, y, width, DETAIL_INK, 2);
        y += 5;
        InvestigationNode from = InvestigationCatalog.node(link.from()).orElseThrow();
        InvestigationNode to = InvestigationCatalog.node(link.to()).orElseThrow();
        int routeColor = known ? 0xFF6C4A30 : MUTED;
        y = drawWrapped(graphics,
                Component.translatable(from.titleKey()).append("  >  ").append(Component.translatable(to.titleKey())),
                x, y, width, routeColor, 4);
        drawDivider(graphics, x, y, width);
        y += 8;
        graphics.drawString(font, Component.translatable(known
                        ? "gui.aquanaut.investigation.corroborated"
                        : "gui.aquanaut.investigation.unconfirmed"),
                x, y, known ? 0xFF9A681E : MUTED, false);
        y += 16;
        Component detail = known ? Component.translatable(link.detailKey())
                : Component.translatable("gui.aquanaut.investigation.locked_link");
        return drawWrapped(graphics, detail, x, y, width, known ? 0xFF364448 : MUTED, 8);
    }

    private void drawFooter(GuiGraphics graphics, Layout layout) {
        int y = layout.outer.bottom() - 15;
        Component controls = Component.translatable("gui.aquanaut.investigation.controls");
        graphics.drawString(font, controls, layout.outer.x + 13, y, LABEL, false);
        String nodes = InvestigationCatalog.nodes().size() + " "
                + Component.translatable("gui.aquanaut.investigation.clues").getString();
        int textWidth = font.width(nodes);
        if (layout.outer.w > font.width(controls) + textWidth + 42) {
            graphics.drawString(font, nodes, layout.outer.right() - textWidth - 18, y, LABEL, false);
        }
    }

    private void drawGraphScale(GuiGraphics graphics, Layout layout) {
        String scale = Math.round(graphZoom * 100.0D) + "%";
        int x = layout.graph.right() - font.width(scale) - 5;
        int y = layout.graph.bottom() - 12;
        graphics.fill(x - 3, y - 2, layout.graph.right() - 2, layout.graph.bottom() - 2, 0x99000000);
        graphics.drawString(font, scale, x, y, BOARD_INK, false);
    }

    private int drawWrapped(GuiGraphics graphics, Component text, int x, int y, int width, int color, int gapAfter) {
        List<FormattedCharSequence> lines = font.split(text, Math.max(20, width));
        for (FormattedCharSequence line : lines) {
            graphics.drawString(font, line, x, y, color, false);
            y += 10;
        }
        return y + gapAfter;
    }

    private void drawClamped(GuiGraphics graphics, String text, int x, int y, int maxWidth, int color) {
        String value = text;
        while (font.width(value) > maxWidth && value.length() > 3) {
            value = value.substring(0, value.length() - 2);
        }
        if (!value.equals(text)) value = value + "…";
        graphics.drawString(font, value, x, y, color, false);
    }

    private static void drawDivider(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, EDGE_DARK);
        graphics.fill(x, y + 1, x + Math.max(12, width / 3), y + 2, WELL_EDGE_LIGHT);
    }

    private static void drawStar(GuiGraphics graphics, int x, int y, int color, boolean bright) {
        int shadow = bright ? 0xFF6E4C18 : 0xFF3D4142;
        graphics.fill(x + 3, y + 1, x + 5, y + 8, shadow);
        graphics.fill(x + 1, y + 3, x + 7, y + 5, shadow);
        graphics.fill(x + 2, y + 2, x + 6, y + 6, color);
        graphics.fill(x + 3, y, x + 5, y + 7, color);
        graphics.fill(x, y + 3, x + 8, y + 5, color);
    }

    private static void drawPixelLine(GuiGraphics graphics, int x1, int y1, int x2, int y2,
            int color, int thickness) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) return;
        for (int i = 0; i <= steps; i++) {
            int x = x1 + dx * i / steps;
            int y = y1 + dy * i / steps;
            graphics.fill(x - thickness / 2, y - thickness / 2, x + (thickness + 1) / 2,
                    y + (thickness + 1) / 2, color);
        }
    }

    private static void drawArrow(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.max(1.0D, Math.sqrt(dx * dx + dy * dy));
        double ux = dx / length;
        double uy = dy / length;
        int tipX = (int) Math.round(x1 + dx * 0.58D);
        int tipY = (int) Math.round(y1 + dy * 0.58D);
        int backX = (int) Math.round(tipX - ux * 8.0D);
        int backY = (int) Math.round(tipY - uy * 8.0D);
        int sideX = (int) Math.round(-uy * 4.0D);
        int sideY = (int) Math.round(ux * 4.0D);
        drawPixelLine(graphics, tipX, tipY, backX + sideX, backY + sideY, color, 2);
        drawPixelLine(graphics, tipX, tipY, backX - sideX, backY - sideY, color, 2);
    }

    private static double distanceToSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared == 0.0D) return Math.hypot(px - x1, py - y1);
        double t = Math.clamp(((px - x1) * dx + (py - y1) * dy) / lengthSquared, 0.0D, 1.0D);
        return Math.hypot(px - (x1 + t * dx), py - (y1 + t * dy));
    }

    private static boolean isOverAnyNode(Map<ResourceLocation, Rect> nodes,
            double mouseX, double mouseY, int margin) {
        for (Rect node : nodes.values()) {
            if (node.containsInflated(mouseX, mouseY, margin)) return true;
        }
        return false;
    }

    private static boolean isLinkVisible(InvestigationProgress progress, InvestigationLink link) {
        if (!progress.isDiscovered(link)) return false;
        InvestigationNode from = InvestigationCatalog.node(link.from()).orElse(null);
        InvestigationNode to = InvestigationCatalog.node(link.to()).orElse(null);
        return from != null && to != null && progress.isDiscovered(from) && progress.isDiscovered(to);
    }

    private void drawScrollBar(GuiGraphics graphics, Rect r, int maxScroll) {
        int trackTop = r.y + 7;
        int trackHeight = r.h - 14;
        int thumbHeight = Math.max(15, trackHeight * trackHeight / (trackHeight + maxScroll));
        int thumbY = trackTop + detailScroll * (trackHeight - thumbHeight) / maxScroll;
        graphics.fill(r.right() - 5, trackTop, r.right() - 3, trackTop + trackHeight, WELL_EDGE_DARK);
        graphics.fill(r.right() - 5, thumbY, r.right() - 3, thumbY + thumbHeight, PANEL);
    }

    private record Layout(Rect outer, Rect graph, Rect details) {
    }

    private record Rect(int x, int y, int w, int h) {
        int right() { return x + w; }
        int bottom() { return y + h; }
        int centerX() { return x + w / 2; }
        int centerY() { return y + h / 2; }
        boolean contains(double px, double py) { return px >= x && px < right() && py >= y && py < bottom(); }
        boolean containsInflated(double px, double py, int margin) {
            return px >= x - margin && px < right() + margin
                    && py >= y - margin && py < bottom() + margin;
        }
    }
}
