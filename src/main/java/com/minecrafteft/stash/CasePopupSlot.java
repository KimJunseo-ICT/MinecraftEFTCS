package com.minecrafteft.stash;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

final class CasePopupSlot extends SlotItemHandler {
    private final StashMenu menu;
    CasePopupSlot(StashMenu menu, CasePopupHandler handler, int index) {
        super(handler, index, -1000, -1000);
        this.menu = menu;
    }
    @Override public void setChanged() {
        if (!menu.isClientView() && menu.popupInventory() instanceof com.minecrafteft.inventory.EquipmentGrid bag) bag.flush();
        super.setChanged();
    }
    @Override public boolean isActive() { return false; }
    @Override public boolean mayPlace(ItemStack stack) { return menu.isCaseOpen() && super.mayPlace(stack); }
    @Override public boolean mayPickup(Player player) { return menu.isCaseOpen() && super.mayPickup(player); }
}
