package com.minecrafteft.inventory;

import com.minecrafteft.stash.StashMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import java.util.Arrays;

/** Rectangular item footprints; contents travel inside the equipment item itself. */
public final class EquipmentGrid implements IItemHandlerModifiable {
    private final java.util.function.Supplier<ItemStack> equipment;
    private final Runnable changed;
    private ItemStack host=ItemStack.EMPTY;
    private final ItemStack[] cells=new ItemStack[StorageEquipment.MAX_CELLS];
    public EquipmentGrid(LoadoutState owner, int slot) {
        this(() -> owner.equipment.getStackInSlot(slot), () -> owner.dirty = true);
    }
    public EquipmentGrid(java.util.function.Supplier<ItemStack> equipment, Runnable changed) {
        this.equipment = equipment;
        this.changed = changed;
        Arrays.fill(cells, ItemStack.EMPTY);
    }
    private void refresh() {
        ItemStack next=equipment.get();
        if(next==host) return;
        host=next;Arrays.fill(cells,ItemStack.EMPTY);
        CompoundTag items=host.getTag()==null?new CompoundTag():host.getTag().getCompound(StorageEquipment.DATA).getCompound("Items");
        for(int i=0;i<cells.length;i++) if(items.contains(Integer.toString(i))) cells[i]=ItemStack.of(items.getCompound(Integer.toString(i)));
    }
    public StorageEquipment.Size size() { refresh();return StorageEquipment.size(host); }
    public void flush() {
        refresh();if(host.isEmpty()) return;
        CompoundTag data=host.hasTag()?host.getTag().getCompound(StorageEquipment.DATA):new CompoundTag(),items=new CompoundTag();
        for(int i=0;i<cells.length;i++) if(!cells[i].isEmpty()) items.put(Integer.toString(i),cells[i].save(new CompoundTag()));
        if (!data.getCompound("Items").equals(items)) {
            data.put("Items",items);
            host.getOrCreateTag().put(StorageEquipment.DATA,data);
            changed.run();
        }
    }
    @Override public int getSlots() { return cells.length; }
    @Override public ItemStack getStackInSlot(int slot) { refresh();return slot<0 || slot>=cells.length?ItemStack.EMPTY:cells[slot]; }
    public int anchorAt(int cell) {
        var size=size();if(cell<0 || cell>=size.cells()) return -1;
        int x=cell%size.width(),y=cell/size.width();
        for(int i=0;i<size.cells();i++) if(!cells[i].isEmpty()) {
            int ax=i%size.width(),ay=i/size.width();
            if(x>=ax && x<ax+StashMenu.footprintWidth(cells[i]) && y>=ay && y<ay+StashMenu.footprintHeight(cells[i])) return i;
        }
        return -1;
    }
    public boolean canPlace(int slot,ItemStack stack) {
        var size=size();if(slot<0 || slot>=size.cells() || stack.isEmpty() || !StorageEquipment.type(stack).isEmpty()) return false;
        ItemStack existing=getStackInSlot(slot);
        if(!existing.isEmpty() && !canMergeSupplies(existing,stack)) return false;
        // Only a real 1x1 supplies merge may reuse its own anchor. Weapons must land in empty cells.
        return InventoryGridGeometry.fits(size.width(),size.height(),slot,StashMenu.footprintWidth(stack),StashMenu.footprintHeight(stack),
                cell -> !existing.isEmpty() && cell==slot ? -1 : anchorAt(cell));
    }
    public static boolean canMergeSupplies(ItemStack existing,ItemStack incoming) {
        return !existing.isEmpty() && !incoming.isEmpty() && existing.isStackable()
                && !StashMenu.isGridItem(existing) && !StashMenu.isGridItem(incoming)
                && StashMenu.footprintWidth(existing)==1 && StashMenu.footprintHeight(existing)==1
                && StashMenu.footprintWidth(incoming)==1 && StashMenu.footprintHeight(incoming)==1
                && ItemStack.isSameItemSameTags(existing,incoming)
                && existing.getCount()<Math.min(64,existing.getMaxStackSize());
    }
    @Override public boolean isItemValid(int slot,ItemStack stack) { return canPlace(slot,stack); }
    @Override public int getSlotLimit(int slot) { return 64; }
    @Override public void setStackInSlot(int slot,ItemStack stack) { refresh();cells[slot]=stack;flush(); }
    @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) {
        if(!canPlace(slot,stack)) return stack;
        ItemStack existing=getStackInSlot(slot);
        if(!existing.isEmpty() && !ItemStack.isSameItemSameTags(existing,stack)) return stack;
        int moved=Math.min(stack.getCount(),Math.min(64,stack.getMaxStackSize())-existing.getCount());
        if(moved<=0) return stack;
        if(!simulate) { ItemStack result=stack.copyWithCount(existing.getCount()+moved);setStackInSlot(slot,result); }
        return stack.copyWithCount(stack.getCount()-moved);
    }
    @Override public ItemStack extractItem(int slot,int count,boolean simulate) {
        ItemStack stack=getStackInSlot(slot);int removed=Math.min(count,stack.getCount());
        if(removed<=0) return ItemStack.EMPTY;
        ItemStack result=stack.copyWithCount(removed);
        if(!simulate) setStackInSlot(slot,stack.copyWithCount(stack.getCount()-removed));
        return result;
    }
    public ItemStack insert(ItemStack stack) {
        ItemStack rest=stack;
        for(int pass=0;pass<2;pass++) for(int i=0;i<size().cells() && !rest.isEmpty();i++)
            if((pass==0)!=getStackInSlot(i).isEmpty()) rest=insertItem(i,rest,false);
        if(!rest.isEmpty() && StashMenu.isGridItem(rest)) {
            ItemStack rotated=StashMenu.rotatedCopy(rest);
            for(int i=0;i<size().cells() && !rotated.isEmpty();i++) rotated=insertItem(i,rotated,false);
            if(rotated.getCount()<rest.getCount()) rest=rotated;
        }
        return rest;
    }
}
