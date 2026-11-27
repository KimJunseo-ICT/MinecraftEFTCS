package com.minecrafteft.corpse;

import com.minecrafteft.inventory.EquipmentGrid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

/** Indices 0..40 match vanilla Inventory so legacy creative inventories retain their contents too. */
public final class CorpseInventory {
    public static final int DOGTAG=41, BAG=42, POUCH=43, RECOVERY=44, CURSOR=80, CRAFT=81, SIZE=85;
    public final ItemStackHandler items=new ItemStackHandler(SIZE);
    public final EquipmentGrid bag=new EquipmentGrid(()->items.getStackInSlot(BAG),()->{});
    public final EquipmentGrid pouch=new EquipmentGrid(()->items.getStackInSlot(POUCH),()->{});

    public CompoundTag save() {
        bag.flush();
        pouch.flush();
        return items.serializeNBT();
    }
    public void load(CompoundTag tag) {
        items.deserializeNBT(tag);
    }
    void copy(int slot, ItemStack stack) {
        if (!stack.isEmpty()) items.setStackInSlot(slot,stack.copy());
    }
}
