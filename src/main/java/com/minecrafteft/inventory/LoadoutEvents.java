package com.minecrafteft.inventory;

import com.minecrafteft.MinecraftEFTMod;
import com.minecrafteft.network.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid=MinecraftEFTMod.MOD_ID)
public final class LoadoutEvents {
    @Mod.EventBusSubscriber(modid=MinecraftEFTMod.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterCapabilitiesEvent event) { event.register(LoadoutState.class); }
    }
    @SubscribeEvent public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if(!(event.getObject() instanceof Player)) return;
        var provider=new LoadoutProvider();event.addCapability(ResourceLocation.fromNamespaceAndPath(MinecraftEFTMod.MOD_ID,"loadout"),provider);event.addListener(provider::invalidate);
    }
    public static void sync(ServerPlayer player) {
        var state=LoadoutState.of(player);ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new LoadoutSyncPacket(state.save()));state.dirty=false;
    }
    @SubscribeEvent public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer wearer) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer),
                    new BackpackAppearancePacket(wearer.getId(), LoadoutState.of(wearer).backpackVisible()));
        }
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getEntity() instanceof ServerPlayer player) { var state=LoadoutState.of(player);state.restoreMelee(player);
            if(state.equipment.getStackInSlot(LoadoutState.DOGTAG).isEmpty()) state.equipment.setStackInSlot(LoadoutState.DOGTAG,DogtagItem.issue(player));sync(player); }
    }
    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        var old=LoadoutState.of(event.getOriginal());var next=LoadoutState.of(event.getEntity());
        int activeHost=old.meleeHost;old.restoreMelee(event.getOriginal());
        if(!event.isWasDeath() || event.getEntity().level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            if(activeHost>=0) event.getEntity().getInventory().setItem(activeHost,event.getOriginal().getInventory().getItem(activeHost).copy());
            next.load(old.save());
        }
        else { next.equipment.setStackInSlot(LoadoutState.MELEE,old.equipment.getStackInSlot(LoadoutState.MELEE).copy());next.creativeCustom=old.creativeCustom;next.hideBackpack=old.hideBackpack;next.migrated=old.migrated;next.pocketRules=old.pocketRules; }
        event.getOriginal().invalidateCaps();
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if(event.getEntity() instanceof ServerPlayer player) { LoadoutState.of(player).equipment.setStackInSlot(LoadoutState.DOGTAG,DogtagItem.issue(player));sync(player); }
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { if(event.getEntity() instanceof ServerPlayer player) sync(player); }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { LoadoutState.of(event.getEntity()).restoreMelee(event.getEntity()); }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void beforeDeath(LivingDeathEvent event) {
        if(event.getEntity() instanceof ServerPlayer player) LoadoutState.of(player).restoreMelee(player);
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if(!(event.player instanceof ServerPlayer player) || event.phase!=TickEvent.Phase.END || !player.isAlive()) return;
        var state=LoadoutState.of(player);
        if(state.meleeHost>=0 && (player.getInventory().selected!=state.meleeHost || !LoadoutState.custom(player)
                || player.isSpectator() || player.containerMenu!=player.inventoryMenu
                || !LoadoutRules.melee(player.getInventory().getItem(state.meleeHost)))) state.restoreMelee(player);
        if(LoadoutState.custom(player) && !player.isSpectator()) LoadoutInventory.normalize(player);
        boolean visible = state.backpackVisible();
        if (visible != state.trackedBackpackVisible) {
            state.trackedBackpackVisible = visible;
            ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> player),
                    new BackpackAppearancePacket(player.getId(), visible));
        }
        if(state.dirty || player.tickCount%20==0) sync(player);
    }
    @SubscribeEvent public static void pickup(EntityItemPickupEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer player) || !LoadoutState.custom(player)) return;
        var entity=event.getItem();if(entity.hasPickUpDelay() || !entity.isAlive()) return;
        if(entity.getOwner()!=null && !entity.getOwner().equals(player.getUUID()) && entity.getAge()<5800) return;
        event.setCanceled(true);ItemStack stack=entity.getItem();int before=stack.getCount();var item=stack.getItem();
        if(!LoadoutInventory.insert(player,stack)) return;
        int count=before-stack.getCount();player.take(entity,count);player.awardStat(net.minecraft.stats.Stats.ITEM_PICKED_UP.get(item),count);
        net.minecraftforge.event.ForgeEventFactory.onItemPickup(entity,player);
        if(stack.isEmpty()) entity.discard();else entity.setItem(stack);
        sync(player);
    }
}
