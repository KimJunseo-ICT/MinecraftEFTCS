package com.minecrafteft.block;

import com.minecrafteft.stash.WeaponCaseInventory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import java.util.List;

public final class WeaponCaseItem extends BlockItem {
    public WeaponCaseItem(Block block) { super(block,new Item.Properties().stacksTo(1)); }
    public WeaponCaseKind kind() { return ((WeaponCaseBlock)getBlock()).kind(); }
    @Override public Component getName(ItemStack stack) {
        Component name=super.getName(stack);
        return kind().marking().isEmpty()?name:name.copy().withStyle(net.minecraft.ChatFormatting.AQUA);
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        // Forge invokes this from Item's constructor, before BlockItem assigns its block.
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.minecrafteft.client.WeaponCaseItemRenderer.instance();
            }
        });
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        super.appendHoverText(stack,level,lines,flag);
        int guns=stack.hasTag()?stack.getTag().getCompound(WeaponCaseInventory.CONTENTS).getList("Items",10).size():0;
        lines.add(Component.translatable("tooltip.minecrafteft.case_guns_only"));
        lines.add(Component.translatable("tooltip.minecrafteft.case_capacity_rows",kind().rows()));
        lines.add(Component.translatable("tooltip.minecrafteft.case_contents",guns));
        lines.add(Component.translatable("tooltip.minecrafteft.case_open"));
    }
}
