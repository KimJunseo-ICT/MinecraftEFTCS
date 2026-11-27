package com.minecrafteft.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class LoadoutInventory {
    private LoadoutInventory() { }
    /** Mutates the supplied stack, just as vanilla menu transfers do. */
    public static boolean insert(Player player,ItemStack stack) {
        return insert(player,stack,null);
    }
    static boolean insert(Player player,ItemStack stack,EquipmentGrid source) {
        int initial=stack.getCount();var inventory=player.getInventory();var state=LoadoutState.of(player);
        for(int pass=0;pass<2;pass++) for(int i=0;i<9 && !stack.isEmpty();i++) {
            if(state.meleeHost==i || !LoadoutRules.hotbar(i,stack)) continue;
            ItemStack target=inventory.getItem(i);
            if(pass==0 && !target.isEmpty() && ItemStack.isSameItemSameTags(target,stack)) {
                int count=Math.min(stack.getCount(),Math.max(0,target.getMaxStackSize()-target.getCount()));target.grow(count);stack.shrink(count);
            } else if(pass==1 && target.isEmpty()) { int count=Math.min(stack.getCount(),stack.getMaxStackSize());inventory.setItem(i,stack.copyWithCount(count));stack.shrink(count); }
        }
        if(!stack.isEmpty() && LoadoutRules.melee(stack) && state.meleeHost<0 && state.equipment.getStackInSlot(LoadoutState.MELEE).isEmpty()) {
            state.equipment.setStackInSlot(LoadoutState.MELEE,stack.copyWithCount(1));stack.shrink(1);
        }
        for(EquipmentGrid grid:new EquipmentGrid[]{state.backpack,state.secure}) if(grid!=source && !stack.isEmpty()) {
            ItemStack rest=grid.insert(stack);stack.setCount(rest.getCount());
        }
        inventory.setChanged();return stack.getCount()!=initial;
    }
    public static void normalize(ServerPlayer player) {
        var state=LoadoutState.of(player);var inv=player.getInventory();
        boolean migration=!state.migrated || state.pocketRules<LoadoutState.CURRENT_POCKET_RULES;
        if(state.meleeHost<0) {
            ItemStack displaced=state.equipment.getStackInSlot(LoadoutState.MELEE);
            if(!displaced.isEmpty() && !LoadoutRules.melee(displaced)) {
                state.equipment.setStackInSlot(LoadoutState.MELEE,ItemStack.EMPTY);insert(player,displaced);
                if(!displaced.isEmpty()) player.drop(displaced,false);
            }
        }
        for(int i=0;i<36;i++) {
            if(i<9 && (i==state.meleeHost || LoadoutRules.hotbar(i,inv.getItem(i)))) continue;
            ItemStack stack=inv.getItem(i);if(stack.isEmpty()) continue;
            inv.setItem(i,ItemStack.EMPTY);insert(player,stack);
            recoverOrDrop(player,stack,migration);
        }
        ItemStack offhand=inv.getItem(40);
        if(!LoadoutRules.offhand(offhand)) {
            inv.setItem(40,ItemStack.EMPTY);insert(player,offhand);
            recoverOrDrop(player,offhand,migration);
        }
        state.migrated=true;state.pocketRules=LoadoutState.CURRENT_POCKET_RULES;state.dirty|=migration;
    }
    static void recoverOrDrop(ServerPlayer player,ItemStack stack,boolean recover) {
        var state=LoadoutState.of(player);
        if(recover) for(int slot=0;slot<state.recovery.getSlots() && !stack.isEmpty();slot++) {
            ItemStack rest=state.recovery.insertItem(slot,stack.copy(),false);stack.setCount(rest.getCount());
        }
        if(!stack.isEmpty()) player.drop(stack,false);
    }
}
