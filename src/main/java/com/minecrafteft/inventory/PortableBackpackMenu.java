package com.minecrafteft.inventory;

import com.minecrafteft.registry.ModContent;
import com.minecrafteft.stash.StashMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import java.util.ArrayList;
import java.util.List;

/** The held bag remains in its real hand slot and carries every content change in its NBT. */
public final class PortableBackpackMenu extends AbstractContainerMenu {
    public final int sourceSlot;
    public final EquipmentGrid contents;
    public final LoadoutSlots loadout;
    public final List<Slot> bagSlots=new ArrayList<>();
    private final Inventory inventory;
    private final ItemStack host;
    public PortableBackpackMenu(int id,Inventory inventory,FriendlyByteBuf data) { this(id,inventory,data.readVarInt()); }
    public PortableBackpackMenu(int id,Inventory inventory,int sourceSlot) {
        super(ModContent.PORTABLE_BACKPACK_MENU.get(),id);
        if(sourceSlot<0 || (sourceSlot>8 && sourceSlot!=40)) throw new IllegalArgumentException("Invalid held bag slot");
        this.inventory=inventory;this.sourceSlot=sourceSlot;this.host=inventory.getItem(sourceSlot);
        contents=new EquipmentGrid(()->inventory.getItem(sourceSlot),inventory::setChanged);
        for(int i=0;i<DayPackItem.WIDTH*DayPackItem.HEIGHT;i++) {
            final int cell=i;
            bagSlots.add(addSlot(new SlotItemHandler(contents,cell,-10000,-10000) {
                @Override public void setChanged() { contents.flush();super.setChanged(); }
            }));
        }
        LoadoutState.of(inventory.player).restoreMelee(inventory.player);
        loadout=new LoadoutSlots(inventory,this::addSlot,null,sourceSlot);
    }
    @Override public boolean stillValid(Player player) {
        return player.isAlive() && !player.isSpectator() && inventory.getItem(sourceSlot)==host && host.getCount()==1 && host.getItem() instanceof DayPackItem;
    }
    @Override public void clicked(int slot,int button,ClickType type,Player player) {
        if(type==ClickType.SWAP && button==sourceSlot) return;
        if(LoadoutSlots.blockedClick(this,slot,button,type,player)) return;
        super.clicked(slot,button,type,player);
        contents.flush();
    }
    @Override public boolean clickMenuButton(Player player,int button) {
        if(button==0) { var state=LoadoutState.of(player);state.hideBackpack=!state.hideBackpack;state.dirty=true;return true; }
        if(button==1 && StashMenu.isGridItem(getCarried())) { setCarried(StashMenu.rotatedCopy(getCarried()));return true; }
        return false;
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0 || index>=slots.size()) return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.mayPickup(player) || !slot.isActive() || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack moving=slot.getItem(),original=moving.copy();slot.set(ItemStack.EMPTY);
        if(bagSlots.contains(slot)) LoadoutInventory.insert(player,moving,contents);
        else { ItemStack rest=contents.insert(moving);moving.setCount(rest.getCount()); }
        slot.set(moving);slot.setChanged();contents.flush();
        return moving.getCount()==original.getCount()?ItemStack.EMPTY:original;
    }
    @Override public void broadcastChanges() { contents.flush();super.broadcastChanges(); }
    @Override public void removed(Player player) { contents.flush();super.removed(player); }
}
