package com.minecrafteft.status;

import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;

/** Placeholder tools intentionally use ordinary item rendering and no custom arm animation. */
public final class SimpleTreatmentItem extends Item {
    public final boolean splint;
    public SimpleTreatmentItem(boolean splint) { super(new Properties().durability(splint?3:4).setNoRepair());this.splint=splint; }
    @Override public int getUseDuration(ItemStack stack) { return 60; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return splint?UseAnim.NONE:UseAnim.EAT; }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);var state=StatusProvider.of(player);
        if(stack.getDamageValue()>=stack.getMaxDamage() || player.getCooldowns().isOnCooldown(this)
                || state==null || splint && !state.arm() && !state.leg()) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);player.setSprinting(false);return InteractionResultHolder.consume(stack);
    }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity) {
        if(!level.isClientSide) {
            var state=StatusProvider.of(entity);
            if(state!=null && (!splint || state.splint())) {
                if(entity instanceof net.minecraft.server.level.ServerPlayer server) com.minecrafteft.profile.ProfileEvents.count(server,"Medicine");
                if(!splint) state.painkillers();
                stack.setDamageValue(stack.getDamageValue()+1);
                if(stack.getDamageValue()>=stack.getMaxDamage()) { stack.shrink(1);entity.broadcastBreakEvent(entity.getUsedItemHand()); }
                if(entity instanceof Player player) { player.getCooldowns().addCooldown(this,6);StatusEvents.sync(player); }
            }
        }
        return stack;
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.minecrafteft.ai2_uses",Math.max(0,stack.getMaxDamage()-stack.getDamageValue()),stack.getMaxDamage()));
        lines.add(Component.translatable("tooltip.minecrafteft."+(splint?"splint":"analgin")));
    }
}
