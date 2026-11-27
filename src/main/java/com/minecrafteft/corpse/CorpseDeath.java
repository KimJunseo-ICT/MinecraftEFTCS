package com.minecrafteft.corpse;

import com.minecrafteft.MinecraftEFTMod;
import com.minecrafteft.inventory.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid=MinecraftEFTMod.MOD_ID)
public final class CorpseDeath {
    private static final Map<ServerPlayer,CorpseEntity> pending=new WeakHashMap<>();

    // Some TACZ ammunition uses the shooter as the direct entity. Match the actual damage source.
    private static final Map<net.minecraft.world.damagesource.DamageSource,net.minecraft.resources.ResourceLocation> gunHits=new WeakHashMap<>();
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void gunHit(com.tacz.guns.api.event.common.EntityHurtByGunEvent.Pre event) {
        if ((!(event.getHurtEntity() instanceof ServerPlayer)
                && !(event.getHurtEntity() instanceof net.minecraft.world.entity.monster.ZombieVillager)) || event.getGunId()==null) return;
        for (var part:com.tacz.guns.api.event.common.GunDamageSourcePart.values()) {
            var source=event.getDamageSource(part);
            if (source!=null) gunHits.put(source,event.getGunId());
        }
    }
    static net.minecraft.resources.ResourceLocation takeGunId(net.minecraft.world.damagesource.DamageSource source) {
        return gunHits.remove(source);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void remember(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) return;
        var body=CorpseEntity.capture(player);
        var state=LoadoutState.of(player);
        state.backpack.flush();
        state.secure.flush();
        var inventory=player.getInventory();
        for (int i=0;i<inventory.getContainerSize();i++) body.loot.copy(i,inventory.getItem(i));
        var tag=state.equipment.getStackInSlot(LoadoutState.DOGTAG).copy();
        if (tag.isEmpty()) tag=DogtagItem.issue(player);
        DogtagItem.recordDeath(tag,player,event.getSource(),takeGunId(event.getSource()));
        body.loot.copy(CorpseInventory.DOGTAG,tag);
        body.loot.copy(CorpseInventory.BAG,state.equipment.getStackInSlot(LoadoutState.BACKPACK));
        body.loot.copy(CorpseInventory.POUCH,state.equipment.getStackInSlot(LoadoutState.SECURE));
        for (int i=0;i<state.recovery.getSlots();i++) body.loot.copy(CorpseInventory.RECOVERY+i,state.recovery.getStackInSlot(i));
        body.loot.copy(CorpseInventory.CURSOR,player.containerMenu.getCarried());
        for (int i=0;i<4;i++) body.loot.copy(CorpseInventory.CRAFT+i,player.inventoryMenu.getSlot(i+1).getItem());
        pending.put(player,body);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void collect(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var body=pending.remove(player);
        if (body==null) return;
        var state=LoadoutState.of(player);
        body.updateEquipment();
        if (!player.serverLevel().addFreshEntity(body)) {
            if (!player.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)) {
                for (var handler:List.of(state.equipment,state.recovery)) for (int i=0;i<handler.getSlots();i++) {
                    if (handler==state.equipment && i==LoadoutState.MELEE) continue;
                    var stack=handler.extractItem(i,Integer.MAX_VALUE,false);
                    if (!stack.isEmpty()) event.getDrops().add(new ItemEntity(player.level(),player.getX(),player.getY(),player.getZ(),stack));
                }
            }
            return;
        }
        CorpseIndex.get(player.server).replace(player.server,body);
        // Vanilla may already have removed these slots. Claim only the surviving drops or retained stacks.
        for (int i=0;i<player.getInventory().getContainerSize();i++) {
            var snapshot=body.loot.items.getStackInSlot(i);
            if (snapshot.isEmpty()) continue;
            int retained=player.getInventory().getItem(i).getCount();
            int claimed=claim(event.getDrops(),snapshot,Math.max(0,snapshot.getCount()-retained));
            if (!net.minecraft.world.item.enchantment.EnchantmentHelper.hasVanishingCurse(snapshot))
                snapshot.setCount(retained+claimed);
        }
        player.getInventory().clearContent();
        player.containerMenu.setCarried(ItemStack.EMPTY);
        for (int i=1;i<=4;i++) player.inventoryMenu.getSlot(i).set(ItemStack.EMPTY);
        for (int i=0;i<state.equipment.getSlots();i++) if (i!=LoadoutState.MELEE) state.equipment.setStackInSlot(i,ItemStack.EMPTY);
        for (int i=0;i<state.recovery.getSlots();i++) state.recovery.setStackInSlot(i,ItemStack.EMPTY);
    }

    private static int claim(Collection<ItemEntity> drops,ItemStack target,int wanted) {
        int remaining=wanted;
        for (var it=drops.iterator();remaining>0 && it.hasNext();) {
            var stack=it.next().getItem();
            if (!ItemStack.isSameItemSameTags(stack,target)) continue;
            int take=Math.min(remaining,stack.getCount());
            remaining-=take;
            stack.shrink(take);
            if (stack.isEmpty()) it.remove();
        }
        return wanted-remaining;
    }
}
