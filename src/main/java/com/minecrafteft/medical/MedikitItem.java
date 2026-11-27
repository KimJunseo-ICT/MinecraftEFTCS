package com.minecrafteft.medical;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;

public class MedikitItem extends Item {
    private static final Rarity MEDICAL_RARITY=Rarity.create("MINECRAFTEFT_MEDICAL",style -> style.withColor(0xB8D9BD));
    public final MedikitType type;
    public MedikitItem(MedikitType type) {
        super(new Properties().durability(type.charges).setNoRepair().rarity(MEDICAL_RARITY));
        this.type = type;
    }
    @Override public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(style -> style.withColor(0xB8D9BD));
    }
    @Override public int getUseDuration(ItemStack stack) { return type.useTicks; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        ItemStack stack=player.getItemInHand(hand);
        if(player.getCooldowns().isOnCooldown(this) || stack.getDamageValue()>=stack.getMaxDamage())
            return InteractionResultHolder.fail(stack);
        if(Ai2Treatment.plan(player,stack,type).target()==TreatmentPlan.Target.NONE) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        player.setSprinting(false);
        Ai2Treatment.begin(player,stack,hand);
        return InteractionResultHolder.consume(stack);
    }
    @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining) {
        Ai2Treatment.tick(entity,stack,(int)com.minecrafteft.status.StatusUseTiming.elapsed(entity,remaining,0));
    }
    private void stopTreatment(LivingEntity entity) {
        // Predict cancellation locally as well, before the server's cooldown packet arrives.
        if(entity.level().isClientSide && entity instanceof Player player)
            player.getCooldowns().addCooldown(this,Ai2TreatmentProgress.REUSE_DELAY_TICKS);
        Ai2Treatment.end(entity);
    }
    @Override public void onStopUsing(ItemStack stack,LivingEntity entity,int remaining) { stopTreatment(entity); }
    @Override public void releaseUsing(ItemStack stack,Level level,LivingEntity entity,int remaining) { stopTreatment(entity); }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity) {
        // The server sends the use-finished event before its cooldown packet.
        if(level.isClientSide && entity instanceof Player player)
            player.getCooldowns().addCooldown(this,Ai2TreatmentProgress.REUSE_DELAY_TICKS);
        Ai2Treatment.finish(entity);
        return stack;
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected) {
        if(!level.isClientSide && stack.getDamageValue()>=stack.getMaxDamage()
                && (!(entity instanceof LivingEntity living) || !Ai2Treatment.activeStack(living,stack))) stack.shrink(1);
    }
    @Override public boolean onEntityItemUpdate(ItemStack stack,net.minecraft.world.entity.item.ItemEntity entity) {
        if(!entity.level().isClientSide && stack.getDamageValue()>=stack.getMaxDamage()) { entity.discard();return true; }
        return false;
    }
    @Override public boolean isEnchantable(ItemStack stack) { return false; }
    @Override public boolean isBookEnchantable(ItemStack stack,ItemStack book) { return false; }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.minecrafteft.ai2_uses",Math.max(0,stack.getMaxDamage()-stack.getDamageValue()),stack.getMaxDamage())
                .withStyle(net.minecraft.ChatFormatting.GOLD));
        lines.add(Component.translatable(type == MedikitType.IFAK ? "tooltip.minecrafteft.ifak_heal" : "tooltip.minecrafteft.ai2_heal").withStyle(net.minecraft.ChatFormatting.GRAY));
        String prefix=type==MedikitType.IFAK?"ifak":"ai2";
        lines.add(Component.translatable("tooltip.minecrafteft."+prefix+"_heavy_cost").withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.minecrafteft."+prefix+"_light_cost").withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.minecrafteft.ai2_use").withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.minecrafteft.ai2_cancel").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("tooltip.minecrafteft.health_cancel").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.minecrafteft.client.Ai2ItemRenderer.instance();
            }
        });
    }
}
