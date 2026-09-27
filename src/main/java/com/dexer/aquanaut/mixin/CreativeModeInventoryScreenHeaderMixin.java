package com.dexer.aquanaut.mixin;

import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dexer.aquanaut.common.inventory.CreativeTabHeader;
import com.dexer.aquanaut.common.inventory.CreativeTabLayout;
import com.dexer.aquanaut.common.inventory.SectionedTabOutput;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;

/**
 * Renders {@link CreativeTabHeader}s inside the creative inventory.
 *
 * <p>
 * When the screen pours a tab's display items into the picker list, this inlines the tab's headers
 * as whole rows of empty slots (see {@link CreativeTabLayout#inline}), which is all it takes for
 * vanilla's own layout and scrolling to flow around them. The empty rows are then drawn over with
 * the header's banner instead of nine slots, and clicking one is ignored: a header row is a label,
 * not a place to drop items.
 */
@Mixin(value = CreativeModeInventoryScreen.class, remap = false)
public abstract class CreativeModeInventoryScreenHeaderMixin {

    /** Vanilla's picker grid: nine columns, five visible rows of 18px cells, items inset one. */
    @Unique
    private static final int AQUANAUT$CELL = 18;
    @Unique
    private static final int AQUANAUT$VISIBLE_ROWS = 5;
    @Unique
    private static final int AQUANAUT$GRID_LEFT = 8;
    @Unique
    private static final int AQUANAUT$GRID_TOP = 17;
    @Unique
    private static final int AQUANAUT$GRID_WIDTH = CreativeTabLayout.GRID_COLUMNS * AQUANAUT$CELL;

    @Shadow
    private static CreativeModeTab selectedTab;
    @Shadow
    private float scrollOffs;
    @Shadow
    static SimpleContainer CONTAINER;

    @Unique
    private CreativeTabLayout.Plan aquanaut$headerPlan = CreativeTabLayout.Plan.EMPTY;

    /**
     * Re-lays the picker list every time the screen fills it: on tab selection, on tab rebuild and
     * on search. The transform drops empty stacks first, so the layered fills (a rebuild calls the
     * search refresh which also fills) settle on the same plan.
     */
    @Inject(method = { "selectTab", "refreshCurrentTabContents", "refreshSearchResults" }, at = @At("TAIL"))
    private void aquanaut$inlineHeaderRows(CallbackInfo ci) {
        CreativeTabLayout layout = SectionedTabOutput.layoutFor(selectedTab).orElse(null);
        CreativeModeInventoryScreen.ItemPickerMenu picker = ((CreativeModeInventoryScreen) (Object) this)
                        .getMenu();
        if (layout == null) {
            this.aquanaut$headerPlan = CreativeTabLayout.Plan.EMPTY;
            return;
        }

        CreativeTabLayout.Plan plan = layout.inline(List.copyOf(picker.items));
        picker.items.clear();
        picker.items.addAll(plan.items());
        this.aquanaut$headerPlan = plan;
        picker.scrollTo(this.scrollOffs);
    }

    /**
     * Draws each visible header row over the slot cells it inlined into. Called inside the
     * container screen's translated pose -- after the slots and their hover highlight, before the
     * tooltips -- so the banner replaces the row without covering anything else.
     */
    @Inject(method = "renderLabels", at = @At("TAIL"))
    private void aquanaut$renderHeaderRows(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        Map<Integer, CreativeTabHeader> headerRows = this.aquanaut$headerPlan.headerRows();
        if (headerRows.isEmpty()) {
            return;
        }

        int firstRow = aquanaut$firstVisibleRow();
        Font font = Minecraft.getInstance().font;
        for (int gridRow = 0; gridRow < AQUANAUT$VISIBLE_ROWS; gridRow++) {
            CreativeTabHeader header = headerRows.get(firstRow + gridRow);
            if (header == null) {
                continue;
            }

            int x = AQUANAUT$GRID_LEFT;
            int y = AQUANAUT$GRID_TOP + gridRow * AQUANAUT$CELL;
            guiGraphics.blit(header.background(), x, y, 0, 0, AQUANAUT$GRID_WIDTH, AQUANAUT$CELL);
            int textY = y + (AQUANAUT$CELL - font.lineHeight) / 2;
            guiGraphics.drawString(font, header.title(), x + 5, textY, 0xFFFFFF, true);
        }
    }

    /**
     * A header row occupies slots but is not one: clicking or dragging onto it does nothing, so an
     * item can never be parked on a banner.
     */
    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void aquanaut$ignoreHeaderRows(Slot slot, int slotId, int mouseButton, ClickType type,
                    CallbackInfo ci) {
        if (slot != null && slot.container == CONTAINER
                        && this.aquanaut$headerPlan.headerRows().containsKey(
                                        aquanaut$firstVisibleRow() + slot.getContainerSlot()
                                                        / CreativeTabLayout.GRID_COLUMNS)) {
            ci.cancel();
        }
    }

    /**
     * The first item row the picker shows for the current scroll offset. This mirrors the two
     * halves of {@code ItemPickerMenu}'s scroll mapping ({@code calculateRowCount} and
     * {@code getRowIndexForScroll}); the plan holds exactly the menu's list, so its rows line up
     * with the slots the menu fills.
     */
    @Unique
    private int aquanaut$firstVisibleRow() {
        int rowCount = Mth.positiveCeilDiv(this.aquanaut$headerPlan.items().size(),
                        CreativeTabLayout.GRID_COLUMNS) - AQUANAUT$VISIBLE_ROWS;
        return Math.max((int) ((double) (this.scrollOffs * (float) rowCount) + 0.5), 0);
    }
}
