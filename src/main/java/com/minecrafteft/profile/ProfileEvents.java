package com.minecrafteft.profile;

import com.minecrafteft.MinecraftEFTMod;
import com.minecrafteft.network.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import java.util.*;

@Mod.EventBusSubscriber(modid=MinecraftEFTMod.MOD_ID)
public final class ProfileEvents {
    private static final Map<ServerPlayer,Long> MOVEMENT=new WeakHashMap<>();
    private static final ResourceLocation[] DISTANCES={Stats.WALK_ONE_CM,Stats.CROUCH_ONE_CM,Stats.SPRINT_ONE_CM,
            Stats.SWIM_ONE_CM,Stats.WALK_ON_WATER_ONE_CM,Stats.WALK_UNDER_WATER_ONE_CM,Stats.CLIMB_ONE_CM,Stats.FLY_ONE_CM,Stats.AVIATE_ONE_CM};
    private ProfileEvents() { }
    private static long distance(ServerPlayer player) {
        long sum=0;for(var type:DISTANCES) sum+=Math.max(0,player.getStats().getValue(Stats.CUSTOM.get(type)));return sum;
    }
    public static void count(ServerPlayer player,String key) { ProfileData.get(player).increment(player,key,1); }
    public static void sync(ServerPlayer player) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new ProfileSyncPacket(ProfileData.get(player).player(player).copy()));
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if(e.getEntity() instanceof ServerPlayer player) { MOVEMENT.put(player,distance(player));sync(player); }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) { MOVEMENT.remove(e.getEntity()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if(e.getEntity() instanceof ServerPlayer player) { MOVEMENT.put(player,distance(player));sync(player); }
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e) {
        if(e.getEntity() instanceof ServerPlayer player) { MOVEMENT.put(player,distance(player));sync(player); }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void death(LivingDeathEvent e) {
        if(e.getEntity() instanceof ServerPlayer player) {
            var data=ProfileData.get(player);data.player(player).putLong("LifeTicks",0);data.setDirty();MOVEMENT.remove(player);sync(player);
        }
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END || !(e.player instanceof ServerPlayer player)) return;
        var data=ProfileData.get(player);data.increment(player,"OnlineTicks",1);
        if(player.isAlive()) data.increment(player,"LifeTicks",1);
        long now=distance(player);var before=MOVEMENT.put(player,now);
        // Vanilla's self-travel statistics exclude teleports and mount travel; don't import older totals.
        if(before!=null && !player.isPassenger() && now>before) data.increment(player,"TravelCm",now-before);
        if(player.tickCount%20==0) sync(player);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void consumed(LivingEntityUseItemEvent.Finish e) {
        if(!(e.getEntity() instanceof ServerPlayer player) || !player.isAlive()) return;
        var item=e.getItem();
        if(item.getUseAnimation()==UseAnim.DRINK) count(player,"Drink");
        else if(item.isEdible()) count(player,"Food");
    }
}
