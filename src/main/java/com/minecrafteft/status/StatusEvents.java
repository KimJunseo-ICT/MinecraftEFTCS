package com.minecrafteft.status;

import com.minecrafteft.MinecraftEFTMod;
import com.minecrafteft.network.*;
import com.minecrafteft.vitals.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import java.util.*;

@Mod.EventBusSubscriber(modid=MinecraftEFTMod.MOD_ID)
public final class StatusEvents {
    private static final Map<Player,Float> FALLS=new WeakHashMap<>();
    private static final class GunImpact { boolean processed; }
    private static final Map<DamageSource,GunImpact> GUN_HITS=new WeakHashMap<>();
    private static final Set<Player> JUMPED=Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<Player> AIRBORNE=Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<Player,net.minecraft.world.phys.Vec3> POSITIONS=new WeakHashMap<>();
    @Mod.EventBusSubscriber(modid=MinecraftEFTMod.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class StatusCapabilityRegistration {
        @SubscribeEvent public static void registerStatusCapability(RegisterCapabilitiesEvent event) { event.register(StatusState.class); }
    }
    @SubscribeEvent public static void attachStatusCapability(AttachCapabilitiesEvent<Entity> event) {
        if(!(event.getObject() instanceof Player)) return;
        var provider=new StatusProvider();event.addCapability(ResourceLocation.fromNamespaceAndPath(MinecraftEFTMod.MOD_ID,"status"),provider);
        event.addListener(provider::invalidate);
    }
    @SubscribeEvent public static void cloneStatus(PlayerEvent.Clone event) {
        if(event.isWasDeath()) return;
        event.getOriginal().reviveCaps();
        var old=StatusProvider.of(event.getOriginal());var next=StatusProvider.of(event.getEntity());
        if(old!=null && next!=null) next.restore(old.snapshot());
        event.getOriginal().invalidateCaps();
    }
    @SubscribeEvent public static void statusLogin(PlayerEvent.PlayerLoggedInEvent e) { sync(e.getEntity()); }
    @SubscribeEvent public static void statusRespawn(PlayerEvent.PlayerRespawnEvent e) { sync(e.getEntity()); }
    @SubscribeEvent public static void statusDimension(PlayerEvent.PlayerChangedDimensionEvent e) { sync(e.getEntity()); }
    @SubscribeEvent public static void statusLogout(PlayerEvent.PlayerLoggedOutEvent e) { FALLS.remove(e.getEntity());JUMPED.remove(e.getEntity());AIRBORNE.remove(e.getEntity());POSITIONS.remove(e.getEntity());StatusUseTiming.clear(e.getEntity()); }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void statusFallHeight(LivingFallEvent event) {
        if(event.getEntity() instanceof Player player) FALLS.put(player,event.getDistance());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void statusGunHit(com.tacz.guns.api.event.common.EntityHurtByGunEvent.Pre event) {
        if(!(event.getHurtEntity() instanceof ServerPlayer)) return;
        var impact=new GunImpact();
        for(var part:com.tacz.guns.api.event.common.GunDamageSourcePart.values()) {
            var source=event.getDamageSource(part);if(source!=null) GUN_HITS.put(source,impact);
        }
    }
    /** Called by the independent HP system before it zeroes vanilla event damage. */
    public static void injured(ServerPlayer player,DamageSource source) {
        if(player.isCreative() || player.isSpectator()) return;
        var state=StatusProvider.of(player);if(state==null) return;
        int fracturesBefore=(state.arm()?1:0)+(state.leg()?1:0);
        // TACZ armor-piercing and ordinary damage parts are one hit, not two injury rolls.
        var impact=GUN_HITS.get(source);
        if(impact!=null && impact.processed) return;
        if(impact!=null) impact.processed=true;
        boolean fall=source.is(DamageTypeTags.IS_FALL);
        boolean attack=source.getEntity()!=player && source.getEntity() instanceof LivingEntity
                && (impact!=null || source.is(DamageTypeTags.IS_PROJECTILE)
                || source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO));
        if(attack || fall) state.reopen(true,()->player.getRandom().nextDouble());
        if(attack) {
            if(player.getRandom().nextDouble()<.10) state.addLight();
            if(player.getRandom().nextDouble()<.03) state.addHeavy();
            if(player.getRandom().nextDouble()<.01) state.fracture(player.getRandom().nextDouble());
        }
        if(fall) {
            float height=FALLS.getOrDefault(player,player.fallDistance);FALLS.remove(player);
            if(player.getRandom().nextDouble()<StatusState.fallChance(height)) state.fractureLeg();
        }
        int fracturesAfter=(state.arm()?1:0)+(state.leg()?1:0);
        if(fracturesAfter>fracturesBefore) com.minecrafteft.profile.ProfileData.get(player).increment(player,"Fractures",fracturesAfter-fracturesBefore);
        sync(player);
    }
    public static void sync(Player player) {
        if(player instanceof ServerPlayer server) {
            var state=StatusProvider.of(player);
            if(state!=null) ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->server),new StatusSyncPacket(state.snapshot()));
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void statusJump(LivingEvent.LivingJumpEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer player) || !eligible(player)
                || com.minecrafteft.medical.Ai2Treatment.using(player)) return;
        var state=StatusProvider.of(player);if(state==null) return;
        state.jump(()->player.getRandom().nextDouble());JUMPED.add(player);sync(player);
    }
    public static boolean eligible(Player player) { return player.isAlive() && !player.isCreative() && !player.isSpectator(); }
    @SubscribeEvent(priority=EventPriority.LOW)
    public static void statusPlayerTick(TickEvent.PlayerTickEvent event) {
        var player=event.player;var state=StatusProvider.of(player);if(state==null) return;
        if(event.phase==TickEvent.Phase.START) {
            if(eligible(player) && state.sprintBlocked()) player.setSprinting(false);
            if(player.isUsingItem() && player.getUseItem().getItem() instanceof SimpleTreatmentItem) player.setSprinting(false);
            return;
        }
        if(!(player instanceof ServerPlayer server) || !player.isAlive()) return;
        if(!eligible(player)) { state.tick(false,()->1);JUMPED.remove(player);AIRBORNE.remove(player);if(player.tickCount%5==0) sync(player);return; }
        var previous=POSITIONS.put(player,player.position());
        boolean running=player.isSprinting() && !player.isPassenger() && previous!=null && previous.subtract(player.position()).horizontalDistanceSqr()>.00001;
        var pulse=state.tick(running,()->player.getRandom().nextDouble());
        double fractureDamage=pulse.fractureDamage();
        if(!player.onGround() && JUMPED.contains(player)) AIRBORNE.add(player);
        if(player.onGround() && AIRBORNE.remove(player)) {
            JUMPED.remove(player);if(state.leg()) fractureDamage+=1;
        }
        if(pulse.bleedingDamage()>0) VitalEvents.hurtWithStatus(server,pulse.bleedingDamage(),false,
                state.heavy()>0?StatusDamage.HEAVY_BLEEDING:StatusDamage.LIGHT_BLEEDING);
        if(fractureDamage>0) VitalEvents.hurtWithStatus(server,fractureDamage,true,StatusDamage.FRACTURE);
        if(pulse.hurtCue() && player.isAlive()) server.serverLevel().broadcastDamageEvent(player,
                (state.heavy()>0?StatusDamage.HEAVY_BLEEDING:StatusDamage.LIGHT_BLEEDING).source(player));
        if(pulse.energyLoss()>0) player.getCapability(VitalProvider.CAPABILITY).ifPresent(v->v.energy(-pulse.energyLoss()));
        if(pulse.blood() && player.isAlive()) BloodMarkEntity.leave(server);
        if(player.tickCount%5==0) sync(player);
    }
}
