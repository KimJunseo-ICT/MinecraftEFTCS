package com.minecrafteft.inventory;

import com.minecrafteft.stash.StashMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;

public final class CreativeLoadoutTransition {
    private CreativeLoadoutTransition() { }
    private static int kind(ItemStack stack) {
        if(StashMenu.isTaczGun(stack)) return "pistol".equals(StashMenu.gunCategory(stack))?WeaponSlotAllocation.PISTOL:WeaponSlotAllocation.PRIMARY;
        return LoadoutRules.melee(stack)?WeaponSlotAllocation.MELEE:WeaponSlotAllocation.OTHER;
    }
    public static boolean apply(ServerPlayer player) {
        var inventory=player.getInventory();var state=LoadoutState.of(player);
        ItemStack[] snapshot=new ItemStack[39];
        for(int i=0;i<36;i++) snapshot[i]=inventory.getItem(i).copy();
        snapshot[36]=state.equipment.getStackInSlot(LoadoutState.MELEE).copy();snapshot[37]=inventory.getItem(40).copy();
        snapshot[38]=player.containerMenu.getCarried().copy();
        if(state.meleeHost>=0) { ItemStack held=snapshot[state.meleeHost];snapshot[state.meleeHost]=snapshot[36];snapshot[36]=held; }
        int[] kinds=new int[snapshot.length],counts=new int[snapshot.length];
        for(int i=0;i<snapshot.length;i++) { kinds[i]=kind(snapshot[i]);counts[i]=snapshot[i].getCount(); }
        var plan=WeaponSlotAllocation.plan(kinds,counts,36);
        if(!plan.fits()) {
            player.sendSystemMessage(Component.translatable("message.minecrafteft.loadout_weapon_limit",plan.primaries(),plan.pistols(),plan.melee()));
            return false;
        }
        // Commit only after the entire loadout has passed capacity validation.
        var displaced=new ArrayList<ItemStack>();
        for(int i=0;i<36;i++) {
            if(kinds[i]!=WeaponSlotAllocation.OTHER) inventory.setItem(i,ItemStack.EMPTY);
            else if(i<3) { if(!snapshot[i].isEmpty()) displaced.add(snapshot[i]);inventory.setItem(i,ItemStack.EMPTY); }
            else inventory.setItem(i,snapshot[i]);
        }
        for(int target=0;target<3;target++) {
            int source=plan.sources()[target];if(source>=0) inventory.setItem(target,snapshot[source].copyWithCount(1));
        }
        if(!snapshot[36].isEmpty() && kinds[36]==WeaponSlotAllocation.OTHER) displaced.add(snapshot[36]);
        int tool=plan.sources()[3];state.equipment.setStackInSlot(LoadoutState.MELEE,tool<0?ItemStack.EMPTY:snapshot[tool].copyWithCount(1));
        inventory.setItem(40,kinds[37]==WeaponSlotAllocation.OTHER?snapshot[37]:ItemStack.EMPTY);
        if(!snapshot[38].isEmpty() && kinds[38]==WeaponSlotAllocation.OTHER) displaced.add(snapshot[38]);
        player.containerMenu.setCarried(ItemStack.EMPTY);
        state.meleeHost=-1;state.dirty=true;
        for(ItemStack stack:displaced) { LoadoutInventory.insert(player,stack);LoadoutInventory.recoverOrDrop(player,stack,true); }
        inventory.setChanged();return true;
    }
}
