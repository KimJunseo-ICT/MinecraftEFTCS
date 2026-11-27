package com.minecrafteft.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Dimensions and persisted grid contents shared by wearable storage. */
public interface StorageEquipment {
    String DATA = "MinecraftEFTStorage";
    int MAX_WIDTH = 8, MAX_HEIGHT = 20, MAX_CELLS = MAX_WIDTH * MAX_HEIGHT;
    String kind(ItemStack stack);
    int columns(ItemStack stack);
    int rows(ItemStack stack);
    record Size(int width,int height) { public int cells() { return width*height; } }
    static String type(ItemStack stack) {
        if(stack.getItem() instanceof StorageEquipment equipment) return equipment.kind(stack);
        return stack.getTag()==null?"":stack.getTag().getCompound(DATA).getString("Kind");
    }
    static Size size(ItemStack stack) {
        int width,height;
        if(stack.getItem() instanceof StorageEquipment equipment) { width=equipment.columns(stack);height=equipment.rows(stack); }
        else { CompoundTag tag=stack.getTag()==null?new CompoundTag():stack.getTag().getCompound(DATA);width=tag.getInt("Width");height=tag.getInt("Height"); }
        return width>0 && width<=MAX_WIDTH && height>0 && height<=MAX_HEIGHT?new Size(width,height):new Size(0,0);
    }
    static boolean accepts(ItemStack stack,String kind) { return !stack.isEmpty() && type(stack).equals(kind) && size(stack).cells()>0; }
}
