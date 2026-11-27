package com.minecrafteft.inventory;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class DayPackItem extends Item implements StorageEquipment {
    public static final int WIDTH = 4;
    public static final int HEIGHT = 5;

    public DayPackItem() { super(new Properties().stacksTo(1)); }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        ItemStack held=player.getItemInHand(hand);
        if(player instanceof ServerPlayer server && player.containerMenu==player.inventoryMenu) {
            int source=hand==InteractionHand.MAIN_HAND?player.getInventory().selected:40;
            NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inventory,who)->new PortableBackpackMenu(id,inventory,source),held.getHoverName()),
                    data->data.writeVarInt(source));
        }
        return InteractionResultHolder.sidedSuccess(held,level.isClientSide);
    }
    @Override public String kind(ItemStack stack) { return "backpack"; }
    @Override public int columns(ItemStack stack) { return WIDTH; }
    @Override public int rows(ItemStack stack) { return HEIGHT; }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.minecrafteft.day_pack").withStyle(ChatFormatting.GRAY));
    }
}
