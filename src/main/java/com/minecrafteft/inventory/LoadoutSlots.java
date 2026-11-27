package com.minecrafteft.inventory;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import java.util.*;
import java.util.function.Function;

/** All visual hit rectangles live in the panel; offscreen positions prevent duplicate vanilla rendering. */
public final class LoadoutSlots {
    public final Player player;
    public final List<Slot> hotbar=new ArrayList<>(),armor=new ArrayList<>(),backpack=new ArrayList<>(),secure=new ArrayList<>(),recovery=new ArrayList<>();
    public final Slot offhand,dogtag,melee,bag,pouch;
    public final List<Slot> all=new ArrayList<>();
    public static Slot hotbar(Inventory inventory,int index) {
        return hotbar(inventory,index,-1);
    }
    private static Slot hotbar(Inventory inventory,int index,int lockedSlot) {
        final int inventoryIndex=index;
        return new Slot(inventory,inventoryIndex,-10000,-10000) {
            @Override public boolean mayPlace(ItemStack stack) { return inventoryIndex!=lockedSlot && LoadoutRules.hotbar(inventoryIndex,stack); }
            @Override public boolean mayPickup(Player player) { return inventoryIndex!=lockedSlot; }
        };
    }
    public LoadoutSlots(Inventory inventory,Function<Slot,Slot> add,List<Slot> existing) {
        this(inventory,add,existing,-1);
    }
    public LoadoutSlots(Inventory inventory,Function<Slot,Slot> add,List<Slot> existing,int lockedSlot) {
        player=inventory.player;var state=LoadoutState.of(player);
        for(int i=0;i<9;i++) hotbar.add(existing==null?add.apply(hotbar(inventory,i,lockedSlot)):existing.get(i));all.addAll(hotbar);
        for(int i=0;i<4;i++) {
            final EquipmentSlot type=EquipmentSlot.values()[EquipmentSlot.HEAD.ordinal()-i];
            Slot slot=new Slot(inventory,39-i,-10000,-10000) {
                @Override public boolean mayPlace(ItemStack stack) { return stack.canEquip(type,player); }
                @Override public int getMaxStackSize() { return 1; }
                @Override public boolean mayPickup(Player who) { return who.isCreative() || !net.minecraft.world.item.enchantment.EnchantmentHelper.hasBindingCurse(getItem()); }
            };
            armor.add(add.apply(slot));all.add(slot);
        }
        offhand=add.apply(new Slot(inventory,40,-10000,-10000) {
            @Override public boolean mayPlace(ItemStack stack) { return lockedSlot!=40 && LoadoutRules.offhand(stack); }
            @Override public boolean mayPickup(Player player) { return lockedSlot!=40; }
        });
        dogtag=add.apply(new SlotItemHandler(state.equipment,LoadoutState.DOGTAG,-10000,-10000) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return false; }
        });
        melee=add.apply(new SlotItemHandler(state.equipment,LoadoutState.MELEE,-10000,-10000));
        bag=add.apply(new SlotItemHandler(state.equipment,LoadoutState.BACKPACK,-10000,-10000));
        pouch=add.apply(new SlotItemHandler(state.equipment,LoadoutState.SECURE,-10000,-10000));
        all.addAll(List.of(offhand,dogtag,melee,bag,pouch));
        for(int i=0;i<StorageEquipment.MAX_CELLS;i++) {
            Slot b=gridSlot(state.backpack,i),s=gridSlot(state.secure,i);
            backpack.add(add.apply(b));secure.add(add.apply(s));all.add(b);all.add(s);
        }
        for(int i=0;i<36;i++) { Slot slot=new SlotItemHandler(state.recovery,i,-10000,-10000) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };recovery.add(add.apply(slot));all.add(slot); }
    }
    private Slot gridSlot(EquipmentGrid grid,int index) {
        final int cellIndex=index;
        return new SlotItemHandler(grid,cellIndex,-10000,-10000) {
            @Override public boolean isActive() { return cellIndex<grid.size().cells(); }
            @Override public void setChanged() { grid.flush();LoadoutState.of(player).dirty=true;super.setChanged(); }
        };
    }
    public boolean contains(Slot slot) { return all.contains(slot); }
    public static boolean blockedClick(net.minecraft.world.inventory.AbstractContainerMenu menu,int index,int button,net.minecraft.world.inventory.ClickType type,Player player) {
        if(index>=0 && index<menu.slots.size() && !menu.slots.get(index).isActive()
                && !(menu.slots.get(index) instanceof com.minecrafteft.stash.StashSlot)
                && !(menu instanceof com.minecrafteft.stash.StashMenu stash && stash.isInteractiveCaseSlot(menu.slots.get(index)))) return true;
        // Vanilla number-key swaps bypass the destination hotbar slot's mayPlace rule.
        if(type!=net.minecraft.world.inventory.ClickType.SWAP || index<0 || index>=menu.slots.size()) return false;
        return button==40?!LoadoutRules.offhand(menu.slots.get(index).getItem()):
                button>=0 && button<9 && !LoadoutRules.hotbar(button,menu.slots.get(index).getItem());
    }
}
