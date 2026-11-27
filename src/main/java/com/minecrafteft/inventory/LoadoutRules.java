package com.minecrafteft.inventory;

import com.minecrafteft.stash.StashMenu;
import net.minecraft.world.item.*;

public final class LoadoutRules {
    private LoadoutRules() { }
    public static boolean melee(ItemStack stack) { return stack.getItem() instanceof SwordItem || stack.getItem() instanceof DiggerItem; }
    public static boolean offhand(ItemStack stack) { return stack.isEmpty() || (!StashMenu.isTaczGun(stack) && !melee(stack)); }
    public static boolean hotbar(int index,ItemStack stack) {
        if(index<0 || index>8) return false;
        if(stack.isEmpty()) return true;
        if(index>=3) return offhand(stack);
        if(!StashMenu.isTaczGun(stack)) return false;
        return index==0?"pistol".equals(StashMenu.gunCategory(stack)):!"pistol".equals(StashMenu.gunCategory(stack));
    }
}
