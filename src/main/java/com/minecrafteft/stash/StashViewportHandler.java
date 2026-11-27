package com.minecrafteft.stash;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

/** A fixed-size menu view onto a scrollable section of the persistent stash inventory. */
public final class StashViewportHandler implements IItemHandlerModifiable {
    private final IItemHandler backing;
    private final StashMenu menu;
    private final int viewSlots;
    private int firstRow;

    public StashViewportHandler(IItemHandler backing, StashMenu menu, int viewRows) {
        this.backing = backing;
        this.menu = menu;
        this.viewSlots = viewRows * 10;
    }

    public void setFirstRow(int firstRow) { this.firstRow = firstRow; }
    public int toStorageIndex(int viewSlot) { return firstRow * 10 + viewSlot; }
    @Override public int getSlots() { return viewSlots; }
    private boolean valid(int index) { return index >= 0 && index < menu.rows() * 10; }
    @Override public ItemStack getStackInSlot(int slot) {
        int index = toStorageIndex(slot);
        return valid(index) ? backing.getStackInSlot(index) : ItemStack.EMPTY;
    }
    @Override public int getSlotLimit(int slot) {
        int index = toStorageIndex(slot);
        return valid(index) ? backing.getSlotLimit(index) : 0;
    }
    @Override public boolean isItemValid(int slot, ItemStack stack) {
        return valid(toStorageIndex(slot)) && menu.canPlaceAt(toStorageIndex(slot), stack) && backing.isItemValid(toStorageIndex(slot), stack);
    }
    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        int storageIndex = toStorageIndex(slot);
        if (!valid(storageIndex) || !menu.canPlaceAt(storageIndex, stack)) return stack;
        return backing.insertItem(storageIndex, stack, simulate);
    }
    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        int storageIndex = toStorageIndex(slot);
        if (!valid(storageIndex) || backing.getStackInSlot(storageIndex).isEmpty()) return ItemStack.EMPTY;
        return backing.extractItem(storageIndex, amount, simulate);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        int storageIndex = toStorageIndex(slot);
        if (!valid(storageIndex)) return;
        // Incoming client synchronization is authoritative, including anchors above the view.
        if (!menu.isClientView() && !stack.isEmpty() && !menu.canPlaceAt(storageIndex, stack)) return;
        if (backing instanceof IItemHandlerModifiable modifiable) {
            modifiable.setStackInSlot(storageIndex, stack);
            return;
        }
        ItemStack current = backing.getStackInSlot(storageIndex);
        if (!current.isEmpty()) backing.extractItem(storageIndex, current.getCount(), false);
        if (!stack.isEmpty()) backing.insertItem(storageIndex, stack, false);
    }
}
