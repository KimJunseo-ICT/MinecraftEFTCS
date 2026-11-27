package com.minecrafteft.stash;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.items.ItemStackHandler;
import com.minecrafteft.block.PersonalStashBlock;
import com.minecrafteft.block.PersonalStashBlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-owned player stash storage. It is saved with the server world, not a block entity. */
public final class StashSavedData extends SavedData {
    private static final String DATA_NAME = "minecrafteft_personal_stashes";
    private static final int STORED_SLOT_LIMIT = StashTier.FOUR.slots();
    private final Map<UUID, StashRecord> records = new HashMap<>();

    public static StashSavedData get(ServerLevel level) {
        MinecraftServer server = level.getServer();
        return server.overworld().getDataStorage().computeIfAbsent(
                StashSavedData::load, StashSavedData::new, DATA_NAME);
    }

    private static StashSavedData load(CompoundTag tag) {
        StashSavedData data = new StashSavedData();
        ListTag list = tag.getList("Stashes", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            try {
                UUID owner = UUID.fromString(entry.getString("Owner"));
                StashRecord record = new StashRecord(data, owner,
                        StashTier.fromLevel(entry.getInt("Tier")));
                ListTag storedItems = entry.getCompound("Items").getList("Items", Tag.TAG_COMPOUND);
                for (int itemIndex = 0; itemIndex < storedItems.size(); itemIndex++) {
                    CompoundTag itemTag = storedItems.getCompound(itemIndex);
                    int slot = itemTag.contains("Slot", Tag.TAG_INT)
                            ? itemTag.getInt("Slot") : itemTag.getByte("Slot") & 0xFF;
                    if (slot >= 0 && slot < record.items.getSlots()) {
                        record.items.setStackInSlot(slot, ItemStack.of(itemTag));
                    }
                }
                if (entry.contains("AnchorDimension", Tag.TAG_STRING)) {
                    record.anchorDimension = entry.getString("AnchorDimension");
                    record.anchorPos = BlockPos.of(entry.getLong("AnchorPos"));
                }
                data.records.put(owner, record);
            } catch (IllegalArgumentException ignored) {
                // Skip malformed records rather than preventing the world from loading.
            }
        }
        return data;
    }

    public StashRecord getRecord(UUID owner) {
        return records.get(owner);
    }

    public StashRecord createRecord(UUID owner, StashTier tier) {
        StashRecord record = new StashRecord(this, owner, tier);
        records.put(owner, record);
        setDirty();
        return record;
    }

    /** A stored anchor in an unloaded chunk is treated as active to avoid duplicate stashes. */
    public boolean hasActiveAnchor(MinecraftServer server, StashRecord record) {
        if (record.anchorDimension == null || record.anchorPos == null) return false;
        ResourceLocation location = ResourceLocation.tryParse(record.anchorDimension);
        if (location == null) {
            record.clearAnchor();
            return false;
        }
        ServerLevel anchorLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, location));
        if (anchorLevel == null) {
            record.clearAnchor();
            return false;
        }
        if (!anchorLevel.hasChunkAt(record.anchorPos)) return true;
        if (!(anchorLevel.getBlockState(record.anchorPos).getBlock() instanceof PersonalStashBlock)) {
            record.clearAnchor();
            return false;
        }
        if (!(anchorLevel.getBlockEntity(record.anchorPos) instanceof PersonalStashBlockEntity blockEntity)
                || !record.owner.equals(blockEntity.getOwner())) {
            record.clearAnchor();
            return false;
        }
        return true;
    }

    public StashTier getActiveAnchorTier(MinecraftServer server, StashRecord record) {
        if (!hasActiveAnchor(server, record) || record.anchorDimension == null || record.anchorPos == null) return null;
        ResourceLocation location = ResourceLocation.tryParse(record.anchorDimension);
        if (location == null) return null;
        ServerLevel anchorLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, location));
        if (anchorLevel == null || !anchorLevel.hasChunkAt(record.anchorPos)) return record.tier;
        if (anchorLevel.getBlockState(record.anchorPos).getBlock() instanceof PersonalStashBlock stashBlock) {
            return stashBlock.tier();
        }
        return null;
    }

    public void bind(StashRecord record, ServerLevel level, BlockPos pos, StashTier tier) {
        record.tier = tier;
        record.anchorDimension = level.dimension().location().toString();
        record.anchorPos = pos.immutable();
        setDirty();
    }

    public void detachIfMatches(UUID owner, ServerLevel level, BlockPos pos) {
        StashRecord record = records.get(owner);
        if (record != null && record.anchorPos != null
                && record.anchorPos.equals(pos)
                && level.dimension().location().toString().equals(record.anchorDimension)) {
            record.clearAnchor();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (StashRecord record : records.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Owner", record.owner.toString());
            entry.putInt("Tier", record.tier.level());
            entry.put("Items", record.serializeItems());
            if (record.anchorDimension != null && record.anchorPos != null) {
                entry.putString("AnchorDimension", record.anchorDimension);
                entry.putLong("AnchorPos", record.anchorPos.asLong());
            }
            list.add(entry);
        }
        tag.put("Stashes", list);
        return tag;
    }

    public static final class StashRecord {
        private final StashSavedData parent;
        private final UUID owner;
        private StashTier tier;
        private String anchorDimension;
        private BlockPos anchorPos;
        private final ItemStackHandler items;

        private StashRecord(StashSavedData parent, UUID owner, StashTier tier) {
            this.parent = parent;
            this.owner = owner;
            this.tier = tier;
            this.items = new ItemStackHandler(STORED_SLOT_LIMIT) {
                @Override
                protected void onContentsChanged(int slot) {
                    StashRecord.this.parent.setDirty();
                }
            };
        }

        public UUID owner() { return owner; }
        public StashTier tier() { return tier; }
        public ItemStackHandler items() { return items; }
        public BlockPos anchorPos() { return anchorPos; }
        public String anchorDimension() { return anchorDimension; }

        private CompoundTag serializeItems() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Size", items.getSlots());
            ListTag list = new ListTag();
            for (int slot = 0; slot < items.getSlots(); slot++) {
                ItemStack stack = items.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    CompoundTag itemTag = stack.save(new CompoundTag());
                    itemTag.putInt("Slot", slot);
                    list.add(itemTag);
                }
            }
            tag.put("Items", list);
            return tag;
        }

        private void clearAnchor() {
            anchorDimension = null;
            anchorPos = null;
            parent.setDirty();
        }
    }
}
