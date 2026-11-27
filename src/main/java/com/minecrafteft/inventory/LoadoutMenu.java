package com.minecrafteft.inventory;

import com.minecrafteft.registry.ModContent;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class LoadoutMenu extends AbstractContainerMenu {
    public final LoadoutSlots loadout;
    public LoadoutMenu(int id,Inventory inventory) {
        this(ModContent.LOADOUT_MENU.get(),id,inventory);
    }
    protected LoadoutMenu(MenuType<?> type,int id,Inventory inventory) {
        super(type,id);LoadoutState.of(inventory.player).restoreMelee(inventory.player);
        loadout=new LoadoutSlots(inventory,this::addSlot,null);
    }
    @Override public boolean stillValid(Player player) { return player.isAlive() && !player.isSpectator(); }
    @Override public void clicked(int index,int button,ClickType type,Player player) {
        if(LoadoutSlots.blockedClick(this,index,button,type,player)) return;
        super.clicked(index,button,type,player);
    }
    @Override public boolean clickMenuButton(Player player,int button) {
        if(button==0) { var state=LoadoutState.of(player);state.hideBackpack=!state.hideBackpack;state.dirty=true;return true; }
        if(button==1 && com.minecrafteft.stash.StashMenu.isGridItem(getCarried())) { setCarried(com.minecrafteft.stash.StashMenu.rotatedCopy(getCarried()));return true; }
        return false;
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0 || index>=slots.size()) return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.isActive() || !slot.mayPickup(player) || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original=slot.getItem(),copy=original.copy();
        // Remove first so an auto-transfer cannot insert back into its own source.
        slot.set(ItemStack.EMPTY);
        if(index<9) {
            var state=LoadoutState.of(player);
            if(index>=3) {
                var equipmentTargets=new java.util.ArrayList<Slot>();equipmentTargets.addAll(loadout.hotbar.subList(0,3));equipmentTargets.addAll(loadout.armor);
                equipmentTargets.addAll(java.util.List.of(loadout.melee,loadout.bag,loadout.pouch));
                for(Slot target:equipmentTargets) if(!original.isEmpty() && !target.hasItem() && target.mayPlace(original)) {
                    target.set(original.copyWithCount(1));original.shrink(1);break;
                }
            }
            for(EquipmentGrid grid:new EquipmentGrid[]{state.backpack,state.secure}) if(!original.isEmpty()) { ItemStack rest=grid.insert(original);original.setCount(rest.getCount()); }
        } else {
            var state=LoadoutState.of(player);
            EquipmentGrid source=loadout.backpack.contains(slot)?state.backpack:loadout.secure.contains(slot)?state.secure:null;
            LoadoutInventory.insert(player,original,source);
        }
        slot.set(original);slot.setChanged();return original.getCount()==copy.getCount()?ItemStack.EMPTY:copy;
    }
}
