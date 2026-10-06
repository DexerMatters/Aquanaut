package com.dexer.aquanaut.client.screen.aquarium;

import com.dexer.aquanaut.client.screen.AquariumScreen;
import com.dexer.aquanaut.core.ItemRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.client.gui.CreativeTabsScreenPage;

import java.util.ArrayList;
import java.util.List;

/**
 * One tab of the aquarium pair: the chest tab that stands for the player inventory, and the fish tab
 * that opens the aquarium.
 *
 * <p>The pair is drawn in the vanilla creative-inventory tab row geometry, hung from the bottom-right
 * corner of the panel, so the aquarium reads as a second page of the inventory rather than as a
 * button bolted next to it. Placement is recomputed on every access because the vanilla inventory
 * screen slides sideways when the recipe book opens and the creative screen reshuffles its rows per
 * page; nothing here is cached, so the tabs can never drift out of position.
 */
public final class AquariumTab extends AbstractWidget {

    /** The two halves of the pair. */
    public enum Kind {
        /** The player inventory, drawn with the vanilla chest icon. */
        INVENTORY("gui.aquanaut.inventory"),
        /** The aquarium itself. */
        AQUARIUM("gui.aquanaut.aquarium");

        private final String titleKey;

        Kind(String titleKey) {
            this.titleKey = titleKey;
        }

        public Component title() {
            return Component.translatable(this.titleKey);
        }

        public ItemStack icon() {
            return switch (this) {
                case INVENTORY -> new ItemStack(Items.CHEST);
                case AQUARIUM -> new ItemStack(ItemRegistry.SARDINE.get());
            };
        }
    }

    private final AbstractContainerScreen<?> owner;
    private final Kind kind;
    private final boolean selected;
    private final Runnable action;

    /**
     * @param owner    the container screen the tab hangs off
     * @param kind     which half of the pair this is
     * @param selected {@code true} when this tab stands for the screen it is drawn on
     * @param action   what a click does, or {@code null} for a tab that only reports the current screen
     */
    public AquariumTab(AbstractContainerScreen<?> owner, Kind kind, boolean selected, Runnable action) {
        super(0, 0, AquariumTabLayout.TAB_WIDTH, AquariumTabLayout.TAB_HEIGHT, kind.title());
        this.owner = owner;
        this.kind = kind;
        this.selected = selected;
        this.action = action;
        this.active = action != null;
        setTooltip(Tooltip.create(kind.title()));
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    public int getX() {
        return placement().x();
    }

    @Override
    public int getY() {
        Placement placement = placement();
        if (placement.stacked() || this.selected) {
            return placement.y();
        }
        // An unselected tab hides its top four rows behind the panel edge, so only the part below that
        // edge is drawn - and only that part answers to the mouse.
        return placement.y() + AquariumTabLayout.ATTACHED_INSET;
    }

    @Override
    public int getWidth() {
        return AquariumTabLayout.TAB_WIDTH;
    }

    @Override
    public int getHeight() {
        if (this.selected) {
            return AquariumTabLayout.TAB_HEIGHT;
        }
        return placement().stacked() ? AquariumTabLayout.FREE_HEIGHT : AquariumTabLayout.ATTACHED_HEIGHT;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        AquariumTabSkin.Shape shape = shape(placement());
        int x = getX();
        int y = getY();
        AquariumTabSkin.draw(graphics, x, y, this.kind == Kind.AQUARIUM, shape,
                this.active && this.isHoveredOrFocused());

        ItemStack icon = this.kind.icon();
        int iconX = AquariumTabSkin.iconX(x);
        int iconY = AquariumTabSkin.iconY(y, shape);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 100.0F);
        graphics.renderItem(icon, iconX, iconY);
        graphics.renderItemDecorations(Minecraft.getInstance().font, icon, iconX, iconY);
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || button != 0 || !this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        playDownSound(Minecraft.getInstance().getSoundManager());
        this.action.run();
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.tab", getMessage()));
    }

    private AquariumTabSkin.Shape shape(Placement placement) {
        if (placement.stacked()) {
            return AquariumTabSkin.Shape.FREE;
        }
        return this.selected ? AquariumTabSkin.Shape.SELECTED : AquariumTabSkin.Shape.ATTACHED;
    }

    private Placement placement() {
        if (this.kind == Kind.INVENTORY) {
            AquariumTabLayout.Box chest = inventoryBox();
            return new Placement(chest.x(), chest.y(), false);
        }
        return aquariumPlacement();
    }

    /**
     * Places the aquarium tab beside the inventory tab, or under it on the creative screen where every
     * column of the row is already taken by a vanilla tab.
     */
    private Placement aquariumPlacement() {
        AquariumTabLayout.Box chest = inventoryBox();
        AquariumTabLayout.Box beside = new AquariumTabLayout.Box(AquariumTabLayout.neighbourX(chest.x()), chest.y());
        if (this.owner instanceof CreativeModeInventoryScreen creative) {
            if (AquariumTabLayout.isFree(beside, visibleTabBoxes(creative))) {
                return new Placement(beside.x(), beside.y(), false);
            }
            boolean chestSelected = creative.isInventoryOpen();
            return new Placement(chest.x(), AquariumTabLayout.stackedY(chest.y(), chestSelected), true);
        }
        return new Placement(beside.x(), beside.y(), false);
    }

    /**
     * {@return the sprite box the vanilla inventory tab occupies on this screen} On the creative screen
     * that is wherever vanilla put its own chest tab; everywhere else it is the panel's bottom-right
     * corner, which is the slot the vanilla tab would use.
     */
    private AquariumTabLayout.Box inventoryBox() {
        if (this.owner instanceof CreativeModeInventoryScreen creative) {
            CreativeTabsScreenPage page = creative.getCurrentPage();
            for (CreativeModeTab tab : page.getVisibleTabs()) {
                if (tab.getType() == CreativeModeTab.Type.INVENTORY) {
                    return boxOf(creative, page, tab);
                }
            }
        }
        return new AquariumTabLayout.Box(
                this.owner.getGuiLeft() + this.owner.getXSize() - AquariumTabLayout.TAB_WIDTH,
                this.owner.getGuiTop() + this.owner.getYSize() - AquariumTabLayout.ATTACHED_INSET);
    }

    private static List<AquariumTabLayout.Box> visibleTabBoxes(CreativeModeInventoryScreen screen) {
        CreativeTabsScreenPage page = screen.getCurrentPage();
        List<AquariumTabLayout.Box> boxes = new ArrayList<>();
        for (CreativeModeTab tab : page.getVisibleTabs()) {
            boxes.add(boxOf(screen, page, tab));
        }
        return boxes;
    }

    /** Mirrors {@code CreativeModeInventoryScreen#getTabX} and {@code #getTabY} for one tab. */
    private static AquariumTabLayout.Box boxOf(CreativeModeInventoryScreen screen, CreativeTabsScreenPage page,
            CreativeModeTab tab) {
        int x = screen.getGuiLeft() + AquariumTabLayout.rowX(screen.getXSize(), page.getColumn(tab),
                tab.isAlignedRight());
        int y = screen.getGuiTop() + AquariumTabLayout.rowY(screen.getYSize(), page.isTop(tab));
        return new AquariumTabLayout.Box(x, y);
    }

    /** Where one tab's sprite goes, and whether it hangs free under another tab instead of the panel. */
    private record Placement(int x, int y, boolean stacked) {
    }

    /** {@return the tabs a screen should carry: the pair on an inventory, the fish tab on creative} */
    public static List<AquariumTab> forScreen(AbstractContainerScreen<?> screen, Runnable openAquarium,
            Runnable openInventory) {
        List<AquariumTab> tabs = new ArrayList<>(2);
        if (screen instanceof AquariumScreen) {
            tabs.add(new AquariumTab(screen, Kind.INVENTORY, false, openInventory));
            tabs.add(new AquariumTab(screen, Kind.AQUARIUM, true, null));
        } else if (screen instanceof InventoryScreen) {
            tabs.add(new AquariumTab(screen, Kind.INVENTORY, true, null));
            tabs.add(new AquariumTab(screen, Kind.AQUARIUM, false, openAquarium));
        } else if (screen instanceof CreativeModeInventoryScreen) {
            // Vanilla already draws its own chest tab here, so only the aquarium is added.
            tabs.add(new AquariumTab(screen, Kind.AQUARIUM, false, openAquarium));
        }
        return tabs;
    }
}
