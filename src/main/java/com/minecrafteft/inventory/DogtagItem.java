package com.minecrafteft.inventory;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;

public final class DogtagItem extends Item {
    public DogtagItem() { super(new Properties().stacksTo(1)); }
    public static ItemStack issue(LivingEntity owner) {
        ItemStack tag=new ItemStack(com.minecrafteft.registry.ModContent.DOGTAG.get());
        tag.getOrCreateTag().putUUID("Owner",owner.getUUID());
        tag.getOrCreateTag().putString("OwnerName",owner instanceof Player player?player.getGameProfile().getName():owner.getName().getString());
        tag.getOrCreateTag().putLong("Issued",owner.level().getGameTime());return tag;
    }
    public static void recordDeath(ItemStack stack,LivingEntity victim,net.minecraft.world.damagesource.DamageSource source,net.minecraft.resources.ResourceLocation gunId) {
        var data=stack.getOrCreateTag();
        data.remove("GunId");data.remove("Cause");data.remove("Killer");data.remove("StatusCause");
        data.putInt("Level",1);
        data.putString("DeathTime",java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss",java.util.Locale.ROOT)));
        var statusCause=com.minecrafteft.status.StatusDamage.from(source);
        if(statusCause!=null) {
            data.putString("StatusCause",statusCause.name());
            data.putString("Cause",Component.Serializer.toJson(Component.translatable(statusCause.translation())));
            return;
        }
        var killer=source.getEntity();
        if (killer!=null) data.putString("Killer",Component.Serializer.toJson(killer.getDisplayName()));
        if (gunId==null && source.getDirectEntity() instanceof com.tacz.guns.entity.EntityKineticBullet bullet) gunId=bullet.getGunId();
        if (gunId!=null) data.putString("GunId",gunId.toString());
        else {
            Component cause=source.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL) || killer==victim
                    ?Component.translatable("corpse.minecrafteft.suicide"):source.getLocalizedDeathMessage(victim);
            data.putString("Cause",Component.Serializer.toJson(cause));
        }
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        if(stack.hasTag() && stack.getTag().contains("OwnerName")) lines.add(Component.translatable("tooltip.minecrafteft.dogtag_owner",stack.getTag().getString("OwnerName")));
    }
}
