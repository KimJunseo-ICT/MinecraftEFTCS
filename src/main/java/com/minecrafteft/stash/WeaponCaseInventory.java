package com.minecrafteft.stash;

import com.tacz.guns.api.item.IGun;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

/** Anchor-only gun storage. Item serialization never permits nested cases. */
public final class WeaponCaseInventory extends ItemStackHandler {
    public static final int ROWS = 15;
    public static final int SLOTS = 150;
    public static final String CONTENTS = "MinecraftEFTCaseContents";
    private final Runnable changed;
    private boolean loading;
    private final int rows;

    public WeaponCaseInventory(Runnable changed) {
        this(ROWS, changed);
    }
    public WeaponCaseInventory(int rows, Runnable changed) {
        super(rows * 10);
        this.rows = rows;
        this.changed = changed;
    }
    public int rows() { return rows; }
    public boolean hasGuns() {
        return !firstGun().isEmpty();
    }
    /** Row-major order: the highest occupied row, then its leftmost gun anchor. */
    public ItemStack firstGun() {
        for(int i=0;i<getSlots();i++) if(accepts(getStackInSlot(i))) return getStackInSlot(i);
        return ItemStack.EMPTY;
    }
    public static ItemStack firstGunInItem(ItemStack stack) {
        if(!stack.hasTag()) return ItemStack.EMPTY;
        int firstIndex=Integer.MAX_VALUE;
        int slots=com.minecrafteft.block.WeaponCaseKind.of(stack).slots();
        ItemStack first=ItemStack.EMPTY;
        for(Tag entry:stack.getTag().getCompound(CONTENTS).getList("Items",Tag.TAG_COMPOUND)) {
            CompoundTag itemTag=(CompoundTag)entry;
            int index=itemTag.getInt("Slot");
            if(index<0 || index>=slots || index>=firstIndex) continue;
            ItemStack gun=ItemStack.of(itemTag);
            if(!accepts(gun)) continue;
            gun.setCount(1);
            firstIndex=index;
            first=gun;
        }
        return first;
    }
    public static boolean itemHasGuns(ItemStack stack) {
        return !firstGunInItem(stack).isEmpty();
    }

    public static boolean accepts(ItemStack stack) { return !stack.isEmpty() && IGun.getIGunOrNull(stack) != null; }

    /** Insert one gun into a complete free rectangle, trying its current orientation first.
     * Neither the input stack nor this inventory changes when no rectangle fits. */
    public ItemStack insertFirstFit(ItemStack stack) {
        return insertFirstFit(stack, false);
    }

    public boolean canFit(ItemStack stack) {
        return accepts(stack) && insertFirstFit(stack, true).getCount() < stack.getCount();
    }

    private ItemStack insertFirstFit(ItemStack stack, boolean simulate) {
        if (!accepts(stack)) return stack;
        ItemStack candidate = stack.copy();
        candidate.setCount(1);
        for (int orientation = 0; orientation < 2; orientation++) {
            for (int index = 0; index < getSlots(); index++) {
                if (!getStackInSlot(index).isEmpty() || !canPlace(index, candidate)) continue;
                if (!insertItem(index, candidate, simulate).isEmpty()) continue;
                ItemStack remainder = stack.copy();
                remainder.shrink(1);
                return remainder;
            }
            candidate = StashMenu.rotatedCopy(candidate);
        }
        return stack;
    }

    public boolean canPlace(int index, ItemStack stack) {
        if (index < 0 || index >= getSlots() || !accepts(stack)) return false;
        int w = StashMenu.footprintWidth(stack), h = StashMenu.footprintHeight(stack);
        int x = index % 10, y = index / 10;
        if (x + w > 10 || y + h > rows) return false;
        for (int i = 0; i < getSlots(); i++) {
            if (i == index) continue;
            ItemStack other = getStackInSlot(i);
            if (other.isEmpty()) continue;
            int ox = i % 10, oy = i / 10;
            if (x < ox + StashMenu.footprintWidth(other) && x + w > ox
                    && y < oy + StashMenu.footprintHeight(other) && y + h > oy) return false;
        }
        return true;
    }

    @Override public int getSlotLimit(int slot) { return 1; }
    @Override public boolean isItemValid(int slot, ItemStack stack) { return canPlace(slot, stack); }
    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return canPlace(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
    }
    @Override public void setStackInSlot(int slot, ItemStack stack) {
        if (!stack.isEmpty() && !canPlace(slot, stack)) return;
        super.setStackInSlot(slot, stack);
    }
    @Override protected void onContentsChanged(int slot) { if (!loading) changed.run(); }

    public void read(CompoundTag tag) {
        loading = true;
        try {
            for (int i = 0; i < getSlots(); i++) stacks.set(i, ItemStack.EMPTY);
            for (Tag entry : tag.getList("Items", Tag.TAG_COMPOUND)) {
                CompoundTag itemTag = (CompoundTag) entry;
                int index = itemTag.getInt("Slot");
                ItemStack item = ItemStack.of(itemTag);
                // Previously saved valid guns retain their exact tag/position. Full validation
                // happens on insertion; geometry rules can change between mod versions.
                if (index >= 0 && index < getSlots() && accepts(item)) {
                    item.setCount(1);
                    stacks.set(index, item);
                }
            }
        } finally { loading = false; }
    }

    public CompoundTag write() { return serializeNBT(); }

    public void readItem(ItemStack stack) {
        read(stack.hasTag() ? stack.getTag().getCompound(CONTENTS) : new CompoundTag());
    }
}
