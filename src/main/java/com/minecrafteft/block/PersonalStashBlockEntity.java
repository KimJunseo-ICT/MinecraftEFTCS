package com.minecrafteft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.minecrafteft.registry.ModContent;

import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import com.minecrafteft.stash.StashMenu;

public final class PersonalStashBlockEntity extends BlockEntity {
    private UUID owner;
    private int restAreaVersion;
    private final Set<UUID> viewers = new HashSet<>();

    public PersonalStashBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.STASH_BLOCK_ENTITY.get(), pos, state);
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void startViewing(UUID player) {
        viewers.add(player);
        updateOpenState();
    }

    public void stopViewing(UUID player) {
        viewers.remove(player);
        updateOpenState();
    }

    public void tickViewers() {
        if (!(level instanceof ServerLevel server) || server.getGameTime() % 10 != 0) return;
        if(server.getGameTime()%20==0 && getBlockState().getBlock() instanceof PersonalStashBlock block
                && block.tier()==com.minecrafteft.stash.StashTier.FOUR && restAreaVersion<12) {
            if(ContainerStructure.addRestArea(level,worldPosition,getBlockState())) {
                restAreaVersion=12;
                setChanged();
            }
        }
        viewers.removeIf(id -> {
            var player = server.getServer().getPlayerList().getPlayer(id);
            return player == null || !(player.containerMenu instanceof StashMenu menu)
                    || !menu.isViewingContainer(server.dimension(), worldPosition);
        });
        updateOpenState();
    }

    private void updateOpenState() {
        if (level == null || level.isClientSide || isRemoved() || level.getBlockEntity(worldPosition) != this
                || !(getBlockState().getBlock() instanceof PersonalStashBlock)) return;
        boolean open = !viewers.isEmpty();
        if (getBlockState().getValue(PersonalStashBlock.OPEN) == open) return;
        level.setBlock(worldPosition, getBlockState().setValue(PersonalStashBlock.OPEN, open), 3);
        level.playSound(null, worldPosition, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
                SoundSource.BLOCKS, 0.7F, 0.8F);
    }

    @Override public AABB getRenderBoundingBox() {
        AABB bounds = new AABB(worldPosition);
        if (getBlockState().getBlock() instanceof PersonalStashBlock block) {
            var facing = getBlockState().getValue(PersonalStashBlock.FACING);
            for (var cell : ContainerStructureLayout.cells(block.tier().level()))
                bounds = bounds.minmax(new AABB(ContainerStructure.position(worldPosition, facing, cell.x(), cell.y(), cell.z())));
        }
        return bounds.inflate(0.15);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("RestAreaVersion",restAreaVersion);
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        restAreaVersion=tag.getInt("RestAreaVersion");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
