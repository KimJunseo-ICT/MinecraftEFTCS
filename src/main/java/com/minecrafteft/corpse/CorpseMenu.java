package com.minecrafteft.corpse;

import com.minecrafteft.inventory.*;
import com.minecrafteft.registry.ModContent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.items.*;
import java.util.*;

public final class CorpseMenu extends LoadoutMenu {
    public final CorpseEntity body;
    public final CorpseInventory loot;
    public final List<Slot> gear=new ArrayList<>(),bagCells=new ArrayList<>(),pouchCells=new ArrayList<>();
    private final int firstLoot;
    public CorpseMenu(int id,Inventory inv,FriendlyByteBuf buf) {
        this(id,inv,inv.player.level().getEntity(buf.readVarInt()) instanceof CorpseEntity body?body:null);
    }
    public CorpseMenu(int id,Inventory inv,CorpseEntity body) {
        super(ModContent.CORPSE_MENU.get(),id,inv);
        this.body=body;
        loot=inv.player.level().isClientSide || body==null?new CorpseInventory():body.loot;
        firstLoot=slots.size();
        append(gear,loot.items);append(bagCells,loot.bag);append(pouchCells,loot.pouch);
    }
    private void append(List<Slot> target,IItemHandler handler) {
        for (int i=0;i<handler.getSlots();i++) {
            final int cellIndex=i;
            target.add(addSlot(new SlotItemHandler(handler,i,-10000,-10000) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return stillValid(loadout.player) && isActive() && (handler instanceof EquipmentGrid grid
                            ? grid.canPlace(cellIndex,stack) : acceptsGear(cellIndex,stack,loadout.player));
                }
                @Override public int getMaxStackSize() {
                    return handler==loot.items && (cellIndex>=36 && cellIndex<=39 || cellIndex>=CorpseInventory.DOGTAG && cellIndex<=CorpseInventory.POUCH)
                            ? 1 : super.getMaxStackSize();
                }
                @Override public int getMaxStackSize(ItemStack stack) {
                    return handler==loot.items?Math.min(getMaxStackSize(),stack.getMaxStackSize()):super.getMaxStackSize(stack);
                }
                @Override public boolean mayPickup(Player player) { return stillValid(player) && isActive(); }
                @Override public boolean isActive() { return !(handler instanceof EquipmentGrid grid) || cellIndex<grid.size().cells(); }
                @Override public void setChanged() { if(handler instanceof EquipmentGrid grid) grid.flush(); super.setChanged(); }
            }));
        }
    }
    private static boolean acceptsGear(int index,ItemStack stack,Player player) {
        if(stack.isEmpty() || index==CorpseInventory.DOGTAG) return false;
        if(index<9) return LoadoutRules.hotbar(index,stack);
        if(index>=36 && index<=39) {
            EquipmentSlot armor=switch(index) { case 36 -> EquipmentSlot.FEET;case 37 -> EquipmentSlot.LEGS;case 38 -> EquipmentSlot.CHEST;default -> EquipmentSlot.HEAD; };
            return stack.canEquip(armor,player);
        }
        if(index==CorpseInventory.BAG) return StorageEquipment.accepts(stack,"backpack");
        if(index==CorpseInventory.POUCH) return StorageEquipment.accepts(stack,"secure");
        return LoadoutRules.offhand(stack);
    }
    @Override public boolean stillValid(Player player) { return body!=null && body.canLoot(player); }
    @Override public void clicked(int index,int button,ClickType type,Player player) {
        if (!stillValid(player)) return;
        if (index>=firstLoot && type!=ClickType.PICKUP && type!=ClickType.QUICK_MOVE) return;
        super.clicked(index,button,type,player);
    }
    @Override public boolean canTakeItemForPickAll(ItemStack stack,Slot slot) { return slot.index<firstLoot && super.canTakeItemForPickAll(stack,slot); }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if (!stillValid(player) || index<0 || index>=slots.size()) return ItemStack.EMPTY;
        Slot slot=slots.get(index);
        if (!slot.isActive() || !slot.mayPickup(player) || !slot.hasItem()) return ItemStack.EMPTY;
        var original=slot.getItem().copy();
        var remaining=original.copy();
        if(index<firstLoot) {
            var state=LoadoutState.of(player);state.backpack.flush();state.secure.flush();
            slot.set(ItemStack.EMPTY);
            insertLoot(remaining);
            slot.set(remaining);slot.setChanged();
            return remaining.getCount()==original.getCount()?ItemStack.EMPTY:original;
        }
        if (!LoadoutInventory.insert(player,remaining)) return ItemStack.EMPTY;
        slot.remove(original.getCount()-remaining.getCount());slot.setChanged();
        return original;
    }
    private void insertLoot(ItemStack stack) {
        // Match equipment roles first, then pockets/offhand and available rectangular storage.
        int[] targets={0,1,2,39,38,37,36,CorpseInventory.BAG,CorpseInventory.POUCH,3,4,5,6,7,8,40};
        for(int pass=0;pass<2 && !stack.isEmpty();pass++) for(int index:targets) {
            Slot target=gear.get(index);var existing=target.getItem();
            if(stack.isEmpty() || !target.mayPlace(stack)) continue;
            if(pass==0 && !existing.isEmpty() && ItemStack.isSameItemSameTags(existing,stack)) {
                int moved=Math.min(stack.getCount(),Math.max(0,target.getMaxStackSize(stack)-existing.getCount()));
                if(moved>0) { existing.grow(moved);stack.shrink(moved);target.setChanged(); }
            } else if(pass==1 && existing.isEmpty()) {
                int moved=Math.min(stack.getCount(),target.getMaxStackSize(stack));
                if(moved>0) { target.set(stack.copyWithCount(moved));stack.shrink(moved); }
            }
        }
        for(var grid:List.of(loot.bag,loot.pouch)) if(!stack.isEmpty()) {
            var remaining=grid.insert(stack);stack.setCount(remaining.getCount());
        }
    }
}
