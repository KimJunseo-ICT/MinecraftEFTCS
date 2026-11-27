package com.minecrafteft.block;

import com.minecrafteft.registry.ModContent;
import com.minecrafteft.stash.StashMenu;
import com.minecrafteft.stash.WeaponCaseInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import java.util.HashSet;
import java.util.UUID;

public final class WeaponCaseBlockEntity extends BlockEntity {
    private final WeaponCaseInventory inventory;
    private final HashSet<UUID> viewers = new HashSet<>();
    private float lid, previousLid;
    private ItemStack clientDisplayGun=ItemStack.EMPTY, lastSyncedGun=ItemStack.EMPTY;

    public WeaponCaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.WEAPON_CASE_ENTITY.get(), pos, state);
        inventory = new WeaponCaseInventory(((WeaponCaseBlock)state.getBlock()).kind().rows(), this::contentsChanged);
    }
    public WeaponCaseInventory inventory() { return inventory; }
    public void loadItem(ItemStack stack) { inventory.readItem(stack); contentsChanged(); }
    public ItemStack displayGun() { return level!=null && level.isClientSide ? clientDisplayGun : inventory.firstGun(); }
    public boolean hasStoredGun() { return !displayGun().isEmpty(); }
    private void contentsChanged() {
        setChanged();
        ItemStack first=inventory.firstGun();
        if(level!=null && !level.isClientSide && !ItemStack.isSameItemSameTags(first,lastSyncedGun))
            level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
        lastSyncedGun=first.copy();
    }
    @Override public CompoundTag getUpdateTag() {
        CompoundTag tag=new CompoundTag();
        ItemStack first=inventory.firstGun();
        tag.put("DisplayGun",first.save(new CompoundTag()));
        return tag;
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public void handleUpdateTag(CompoundTag tag) {
        ItemStack first=ItemStack.of(tag.getCompound("DisplayGun"));
        clientDisplayGun=WeaponCaseInventory.accepts(first)?first:ItemStack.EMPTY;
        if(!clientDisplayGun.isEmpty()) clientDisplayGun.setCount(1);
    }
    @Override public void onDataPacket(net.minecraft.network.Connection connection,
            net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet) {
        if(packet.getTag()!=null) handleUpdateTag(packet.getTag());
    }
    public ItemStack asItem() {
        ItemStack item = new ItemStack(getBlockState().getBlock());
        item.getOrCreateTag().put(WeaponCaseInventory.CONTENTS, inventory.write());
        return item;
    }
    public void startViewing(UUID id) { viewers.add(id); updateOpen(); }
    public void stopViewing(UUID id) { viewers.remove(id); updateOpen(); }
    public void tick() {
        if (level == null) return;
        if (level.isClientSide) {
            previousLid = lid;
            lid = net.minecraft.util.Mth.approach(lid, getBlockState().getValue(WeaponCaseBlock.OPEN) ? 1 : 0, .12F);
        } else if (level instanceof ServerLevel server) {
            // Shift-click may shrink the stored stack without a handler callback.
            if(!ItemStack.isSameItemSameTags(inventory.firstGun(),lastSyncedGun)) contentsChanged();
            if(level.getGameTime()%10!=0) return;
            viewers.removeIf(id -> {
                var player = server.getServer().getPlayerList().getPlayer(id);
                return player == null || !(player.containerMenu instanceof StashMenu menu)
                        || !menu.isViewingCaseBlock(server.dimension(), worldPosition);
            });
            updateOpen();
        }
    }
    public float lid(float partial) { return net.minecraft.util.Mth.lerp(partial, previousLid, lid); }
    private void updateOpen() {
        if (level == null || level.isClientSide || isRemoved() || level.getBlockEntity(worldPosition) != this
                || !(getBlockState().getBlock() instanceof WeaponCaseBlock)) return;
        boolean open = !viewers.isEmpty();
        if (getBlockState().getValue(WeaponCaseBlock.OPEN) != open) {
            level.setBlock(worldPosition, getBlockState().setValue(WeaponCaseBlock.OPEN, open), 3);
            level.playSound(null, worldPosition, open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE,
                    SoundSource.BLOCKS, .6F, .8F);
        }
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Contents", inventory.write());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if(tag.contains("DisplayGun")) {
            handleUpdateTag(tag);
            return;
        }
        inventory.read(tag.getCompound("Contents"));
        lastSyncedGun=inventory.firstGun().copy();
    }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(3.5); }
}
