package com.minecrafteft.stash;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public final class StashSlot extends SlotItemHandler {
    private final StashMenu menu;
    private final int viewSlot;
    private final boolean contextOnly;
    private boolean suppressItemRender;

    public StashSlot(StashMenu menu, IItemHandler itemHandler, int index, int x, int y) {
        this(menu, itemHandler, index, x, y, index);
    }

    public StashSlot(StashMenu menu, IItemHandler itemHandler, int index, int x, int y, int viewSlot) {
        super(itemHandler, index, x, y);
        this.menu = menu;
        this.viewSlot = viewSlot;
        this.contextOnly = viewSlot < 0;
    }

    @Override
    public boolean isActive() { return !contextOnly; }

    public int viewSlot() {
        return viewSlot;
    }

    public void setSuppressItemRender(boolean suppress) {
        this.suppressItemRender = suppress;
    }

    @Override
    public ItemStack getItem() {
        ItemStack stack = super.getItem();
        return suppressItemRender ? ItemStack.EMPTY : stack;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return !contextOnly && super.mayPlace(stack) && menu.canPlaceAt(menu.storageIndexForViewSlot(viewSlot), stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        // A legacy overlapping anchor must remain recoverable if a full stash cannot reflow.
        ItemStack stack = super.getItem();
        return !menu.isLockedStorageIndex(menu.storageIndexForViewSlot(viewSlot))
                && !stack.isEmpty() && (Math.floorDiv(viewSlot, 10) + StashMenu.footprintHeight(stack) > 0)
                && super.mayPickup(player);
    }
}
