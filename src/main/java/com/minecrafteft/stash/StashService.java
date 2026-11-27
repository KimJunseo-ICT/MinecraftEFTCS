package com.minecrafteft.stash;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;
import com.minecrafteft.block.PersonalStashBlockEntity;
import com.minecrafteft.registry.ModContent;

public final class StashService {
    private StashService() {}

    public static void claimOrOpen(ServerPlayer player, ServerLevel level, BlockPos pos, StashTier blockTier) {
        if(!inventoryAllowed(player)) return;
        if (!(level.getBlockEntity(pos) instanceof PersonalStashBlockEntity blockEntity)) return;
        StashSavedData data = StashSavedData.get(level);
        StashSavedData.StashRecord record = data.getRecord(player.getUUID());

        if (blockEntity.getOwner() != null) {
            if (!player.getUUID().equals(blockEntity.getOwner())) {
                player.sendSystemMessage(Component.translatable("message.minecrafteft.stash_private"));
                return;
            }
            if (record == null) {
                player.sendSystemMessage(Component.translatable("message.minecrafteft.stash_data_missing"));
                return;
            }
            if (blockTier.level() < record.tier().level()) {
                if (hasItemsOutside(record, blockTier)) {
                    player.sendSystemMessage(Component.translatable("message.minecrafteft.stash_downgrade_blocked",
                            blockTier.level(), record.tier().level(), blockTier.slots()));
                    return;
                } else {
                    data.bind(record, level, pos, blockTier);
                    player.sendSystemMessage(Component.translatable("message.minecrafteft.stash_downgrade_complete",
                            blockTier.level()));
                }
            }
            open(player, data, record);
            return;
        }

        MinecraftServer server = level.getServer();
        if (record != null && data.hasActiveAnchor(server, record)) {
            player.sendSystemMessage(Component.translatable("message.minecrafteft.stash_already_owned"));
            return;
        }
        if (record != null && hasItemsOutside(record, blockTier)) {
            player.sendSystemMessage(Component.translatable("message.minecrafteft.stash_downgrade_blocked",
                    blockTier.level(), record.tier().level(), blockTier.slots()));
            return;
        }
        if (record == null) record = data.createRecord(player.getUUID(), blockTier);
        data.bind(record, level, pos, blockTier);
        blockEntity.setOwner(player.getUUID());
        player.sendSystemMessage(Component.translatable("message.minecrafteft.stash_claimed", blockTier.level()));
        open(player, data, record);
    }

    public static void openOwned(ServerPlayer player) {
        if(!inventoryAllowed(player)) return;
        ServerLevel level = player.serverLevel();
        StashSavedData data = StashSavedData.get(level);
        StashSavedData.StashRecord record = data.getRecord(player.getUUID());
        if (record == null) {
            player.sendSystemMessage(Component.translatable("message.minecrafteft.stash_not_claimed"));
            return;
        }
        if (!data.hasActiveAnchor(player.serverLevel().getServer(), record)) {
            player.sendSystemMessage(Component.translatable("message.minecrafteft.stash_anchor_missing"));
            return;
        }
        if (!withinRange(player, record)) {
            player.sendSystemMessage(Component.translatable("message.minecrafteft.stash_out_of_range"));
            return;
        }
        open(player, data, record);
    }

    public static boolean withinRange(ServerPlayer player, StashSavedData.StashRecord record) {
        return record.anchorPos() != null && player.level().dimension().location().toString().equals(record.anchorDimension())
                && player.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(record.anchorPos())) <= 50D * 50D;
    }

    private static void open(ServerPlayer player, StashSavedData data, StashSavedData.StashRecord record) {
        if(!inventoryAllowed(player)) return;
        com.minecrafteft.inventory.LoadoutState.of(player).restoreMelee(player);
        com.minecrafteft.inventory.LoadoutEvents.sync(player);
        StashTier anchorTier = data.getActiveAnchorTier(player.server, record);
        if (anchorTier == null) {
            player.sendSystemMessage(Component.translatable("message.minecrafteft.stash_anchor_missing"));
            return;
        }
        if (anchorTier.level() < record.tier().level()) {
            player.sendSystemMessage(Component.translatable("message.minecrafteft.stash_downgrade_blocked",
                    anchorTier.level(), record.tier().level(), anchorTier.slots()));
            return;
        }
        NetworkHooks.openScreen(player,
                new SimpleMenuProvider((containerId, inventory, ignored) ->
                        new StashMenu(containerId, inventory, record),
                        Component.translatable("container.minecrafteft.personal_stash")),
                buffer -> buffer.writeInt(record.tier().slots()).writeBoolean(false));
        var anchorLevel = player.server.getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                net.minecraft.resources.ResourceLocation.parse(record.anchorDimension())));
        if (player.containerMenu instanceof StashMenu && anchorLevel != null
                && anchorLevel.getBlockEntity(record.anchorPos()) instanceof PersonalStashBlockEntity blockEntity)
            blockEntity.startViewing(player.getUUID());
    }

    public static void onAnchorDestroyed(ServerLevel level, BlockPos pos, java.util.UUID owner) {
        StashSavedData.get(level).detachIfMatches(owner, level, pos);
    }

    private static boolean inventoryAllowed(ServerPlayer player) {
        if(!player.isCreative() || com.minecrafteft.inventory.LoadoutState.of(player).creativeCustom) return true;
        player.sendSystemMessage(Component.translatable("message.minecrafteft.custom_inventory_required"));return false;
    }

    private static boolean hasItemsOutside(StashSavedData.StashRecord record, StashTier targetTier) {
        for (int index = 0; index < record.items().getSlots(); index++) {
            net.minecraft.world.item.ItemStack stack = record.items().getStackInSlot(index);
            if (!stack.isEmpty() && (index >= targetTier.slots()
                    || index / 10 + StashMenu.footprintHeight(stack) > targetTier.rows())) return true;
        }
        return false;
    }
}

