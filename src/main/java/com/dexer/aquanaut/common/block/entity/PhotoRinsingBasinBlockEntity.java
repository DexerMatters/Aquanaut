package com.dexer.aquanaut.common.block.entity;

import com.dexer.aquanaut.common.inventory.PhotoRinsingMenu;
import com.dexer.aquanaut.common.item.DevelopedPhotoItem;
import com.dexer.aquanaut.core.BlockEntityRegistry;
import com.dexer.aquanaut.core.ItemRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Stores and develops one exposed film using one batch of brine developing salts. */
public final class PhotoRinsingBasinBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int SALTS_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int SLOT_COUNT = 3;
    public static final int DEVELOPMENT_TICKS = 100;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int progress;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? progress : DEVELOPMENT_TICKS;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                progress = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public PhotoRinsingBasinBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.PHOTO_RINSING_BASIN.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
            PhotoRinsingBasinBlockEntity basin) {
        if (!basin.canDevelop()) {
            if (basin.progress != 0) {
                basin.progress = 0;
                basin.setChanged();
            }
            return;
        }

        basin.progress++;
        if (basin.progress < DEVELOPMENT_TICKS) {
            if ((basin.progress & 15) == 0) {
                basin.setChanged();
            }
            return;
        }

        ItemStack developed = new ItemStack(ItemRegistry.DEVELOPED_PHOTO.get());
        if (!DevelopedPhotoItem.copyPhoto(basin.items.get(INPUT_SLOT), developed)) {
            basin.progress = 0;
            basin.setChanged();
            return;
        }

        basin.items.get(INPUT_SLOT).shrink(1);
        basin.items.get(SALTS_SLOT).shrink(1);
        basin.items.set(OUTPUT_SLOT, developed);
        basin.progress = 0;
        basin.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.7F, 1.25F);
    }

    private boolean canDevelop() {
        ItemStack input = this.items.get(INPUT_SLOT);
        ItemStack salts = this.items.get(SALTS_SLOT);
        return input.is(ItemRegistry.EXPOSED_FILM.get())
                && DevelopedPhotoItem.photo(input).isPresent()
                && salts.is(ItemRegistry.BRINE_DEVELOPING_SALTS.get())
                && this.items.get(OUTPUT_SLOT).isEmpty();
    }

    public ContainerData data() {
        return this.data;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.aquanaut.photo_rinsing_basin");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new PhotoRinsingMenu(containerId, inventory, this, this.data);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) {
            this.setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        stack.limitSize(this.getMaxStackSize(stack));
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT_SLOT -> stack.is(ItemRegistry.EXPOSED_FILM.get());
            case SALTS_SLOT -> stack.is(ItemRegistry.BRINE_DEVELOPING_SALTS.get());
            default -> false;
        };
    }

    @Override
    public void clearContent() {
        this.items.clear();
        this.progress = 0;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putInt("DevelopmentProgress", this.progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.progress = tag.getInt("DevelopmentProgress");
    }
}
