package com.minecrafteft.stash;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

/** Hidden menu slots carry authoritative case contents over the normal inventory protocol. */
final class CasePopupHandler implements IItemHandlerModifiable {
    private final StashMenu menu;
    private final ItemStackHandler mirror = new ItemStackHandler(WeaponCaseInventory.SLOTS);
    CasePopupHandler(StashMenu menu) { this.menu = menu; }
    net.minecraft.nbt.CompoundTag snapshot() { return mirror.serializeNBT(); }
    void preview(IItemHandlerModifiable inventory) {
        for (int i = 0; i < getSlots(); i++) mirror.setStackInSlot(i,
                i < inventory.getSlots() ? inventory.getStackInSlot(i).copy() : ItemStack.EMPTY);
    }
    @Override public int getSlots() { return WeaponCaseInventory.SLOTS; }
    @Override public ItemStack getStackInSlot(int slot) {
        if (menu.isClientView()) return mirror.getStackInSlot(slot);
        return menu.popupInventory() == null || slot >= menu.popupInventory().getSlots()
                ? ItemStack.EMPTY : menu.popupInventory().getStackInSlot(slot);
    }
    @Override public int getSlotLimit(int slot) { return slot < menu.openCaseSlots() ? (menu.isBackpackOpen() ? 64 : 1) : 0; }
    @Override public boolean isItemValid(int slot, ItemStack stack) {
        if (!menu.acceptsPopupItem(stack)) return false;
        int columns = menu.openColumns();
        int w = StashMenu.footprintWidth(stack), h = StashMenu.footprintHeight(stack);
        int x = slot % columns, y = slot / columns;
        if (slot < 0 || slot >= menu.openCaseSlots() || x + w > columns || y + h > menu.openCaseRows()) return false;
        if(menu.isBackpackOpen() && !getStackInSlot(slot).isEmpty()
                && !com.minecrafteft.inventory.EquipmentGrid.canMergeSupplies(getStackInSlot(slot),stack)) return false;
        for (int i = 0; i < menu.openCaseSlots(); i++) {
            if (i == slot) continue;
            ItemStack other = getStackInSlot(i);
            if (!other.isEmpty() && x < i % columns + StashMenu.footprintWidth(other) && x + w > i % columns
                    && y < i / columns + StashMenu.footprintHeight(other) && y + h > i / columns) return false;
        }
        return true;
    }
    @Override public void setStackInSlot(int slot, ItemStack stack) {
        if (menu.isClientView()) mirror.setStackInSlot(slot, stack);
        else if (menu.popupInventory() != null && slot < menu.popupInventory().getSlots()) menu.popupInventory().setStackInSlot(slot, stack);
    }
    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!isItemValid(slot, stack)) return stack;
        return menu.isClientView() ? mirror.insertItem(slot, stack, simulate)
                : menu.popupInventory().insertItem(slot, stack, simulate);
    }
    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!menu.isCaseOpen() || slot >= menu.openCaseSlots()) return ItemStack.EMPTY;
        return menu.isClientView() ? mirror.extractItem(slot, amount, simulate)
                : menu.popupInventory().extractItem(slot, amount, simulate);
    }
}
