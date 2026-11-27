package com.minecrafteft.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

public final class LoadoutState {
    public static final int DOGTAG=0,MELEE=1,BACKPACK=2,SECURE=3;
    public boolean creativeCustom,hideBackpack,migrated,dirty=true;
    public int meleeHost=-1;
    public boolean trackedBackpackVisible;
    public boolean backpackVisible() {
        return !hideBackpack && equipment.getStackInSlot(BACKPACK).getItem() instanceof DayPackItem;
    }
    public static final int CURRENT_POCKET_RULES=3;
    public int pocketRules;
    public final ItemStackHandler equipment=new ItemStackHandler(4) {
        @Override protected void onContentsChanged(int slot) { dirty=true; }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot,ItemStack stack) {
            return switch(slot) {
                case DOGTAG -> stack.getItem() instanceof DogtagItem;
                case MELEE -> LoadoutRules.melee(stack);
                case BACKPACK -> StorageEquipment.accepts(stack,"backpack");
                case SECURE -> StorageEquipment.accepts(stack,"secure");
                default -> false;
            };
        }
    };
    public final ItemStackHandler recovery=new ItemStackHandler(36) {
        @Override protected void onContentsChanged(int slot) { dirty=true; }
    };
    public final EquipmentGrid backpack=new EquipmentGrid(this,BACKPACK),secure=new EquipmentGrid(this,SECURE);
    public static LoadoutState of(Player player) { return player.getCapability(LoadoutProvider.CAPABILITY).orElseThrow(IllegalStateException::new); }
    public static boolean custom(Player player) {
        // Death/respawn briefly leaves the old client player with invalid capabilities.
        return player.isAlive() && player.getCapability(LoadoutProvider.CAPABILITY)
                .map(state -> !player.isCreative() || state.creativeCustom).orElse(false);
    }
    public CompoundTag save() {
        backpack.flush();secure.flush();
        CompoundTag tag=new CompoundTag();tag.put("Equipment",equipment.serializeNBT());tag.put("Recovery",recovery.serializeNBT());
        tag.putBoolean("CreativeCustom",creativeCustom);tag.putBoolean("HideBackpack",hideBackpack);tag.putBoolean("Migrated",migrated);tag.putInt("MeleeHost",meleeHost);
        tag.putInt("PocketRules",pocketRules);
        return tag;
    }
    public void load(CompoundTag tag) {
        if(tag.contains("Equipment")) equipment.deserializeNBT(tag.getCompound("Equipment"));
        if(tag.contains("Recovery")) recovery.deserializeNBT(tag.getCompound("Recovery"));
        creativeCustom=tag.getBoolean("CreativeCustom");hideBackpack=tag.getBoolean("HideBackpack");migrated=tag.getBoolean("Migrated");
        meleeHost=tag.contains("MeleeHost")?tag.getInt("MeleeHost"):-1;
        pocketRules=tag.getInt("PocketRules");
        if(meleeHost<0 || meleeHost>8) meleeHost=-1;
        dirty=true;
    }
    public void restoreMelee(Player player) {
        if(meleeHost<0) return;
        ItemStack tool=player.getInventory().getItem(meleeHost);
        player.getInventory().setItem(meleeHost,equipment.getStackInSlot(MELEE));
        equipment.setStackInSlot(MELEE,tool);meleeHost=-1;dirty=true;
    }
    public void toggleMelee(Player player) {
        if(meleeHost>=0) { restoreMelee(player);return; }
        if(equipment.getStackInSlot(MELEE).isEmpty()) return;
        player.stopUsingItem();meleeHost=player.getInventory().selected;
        ItemStack held=player.getInventory().getItem(meleeHost);
        player.getInventory().setItem(meleeHost,equipment.getStackInSlot(MELEE));equipment.setStackInSlot(MELEE,held);dirty=true;
    }
}
