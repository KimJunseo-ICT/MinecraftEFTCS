package com.minecrafteft.vitals;

import com.minecrafteft.MinecraftEFTMod;
import com.minecrafteft.network.ModNetwork;
import com.minecrafteft.network.VitalSyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = MinecraftEFTMod.MOD_ID)
public final class VitalEvents {
    private static final Map<Player, Vec3> LAST_POSITION = new WeakHashMap<>();
    private VitalEvents() { }

    @Mod.EventBusSubscriber(modid = MinecraftEFTMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterCapabilitiesEvent event) { event.register(VitalState.class); }
    }

    @SubscribeEvent public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof Player)) return;
        var provider = new VitalProvider();
        event.addCapability(ResourceLocation.fromNamespaceAndPath(MinecraftEFTMod.MOD_ID, "vitals"), provider);
        event.addListener(provider::invalidate);
    }

    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event) {
        if (event.isWasDeath()) return; // Respawn starts a fresh, full set of vitals.
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(VitalProvider.CAPABILITY).ifPresent(old ->
                event.getEntity().getCapability(VitalProvider.CAPABILITY).ifPresent(next -> {
                    next.update(old.health(), old.energy(), old.hydration(), old.stamina(), old.sprintLocked(),old.sprintRestartRequired());
                    next.restoreDeprivationElapsed(old.deprivationElapsed());
                    next.restoreSprintRuntime(old.staminaRecoveryDelay(),old.sprintRestartRequired());
                }));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { initialize(event.getEntity()); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { initialize(event.getEntity()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { initialize(event.getEntity()); }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { LAST_POSITION.remove(event.getEntity()); }

    private static void initialize(Player player) {
        VitalFoodData.install(player);
        LAST_POSITION.put(player, player.position());
        if (player instanceof ServerPlayer server) player.getCapability(VitalProvider.CAPABILITY).ifPresent(state -> {
            // A death screen disconnect may save zero HP before respawn completes.
            if (player.isAlive() && state.health() == 0) state.revive(VitalState.MAX_HEALTH);
            mirrorHealth(player, state);
            sync(server, state);
        });
    }

    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        VitalFoodData.install(player);
        if (!(player instanceof ServerPlayer server) || !player.isAlive()) return;
        player.getCapability(VitalProvider.CAPABILITY).ifPresent(state -> {
            if (event.phase == TickEvent.Phase.START) {
                if ((state.sprintLocked() || state.sprintRestartRequired()) && !player.isCreative()) player.setSprinting(false);
                return;
            }
            Vec3 previous = LAST_POSITION.put(player, player.position());
            boolean moving = previous != null && previous.subtract(player.position()).horizontalDistanceSqr() > .00001;
            var status=com.minecrafteft.status.StatusProvider.of(player);
            var conditions=status==null?null:status.resources();
            if (!player.isCreative() && !player.isSpectator()) {
                double before=state.stamina();
                state.tick(.05, player.isSprinting() && !player.isPassenger(), moving && !player.isPassenger(),
                        VitalConfig.ENERGY_DRAIN.get()*(conditions==null?1:conditions.energyMultiplier()), VitalConfig.WATER_DRAIN.get(),
                        VitalConfig.SPRINT_DRAIN.get()*(conditions!=null && conditions.dehydrated()?2:1),
                        VitalConfig.WALK_RECOVERY.get()*(conditions!=null && conditions.malnourished()?.5:1),
                        VitalConfig.IDLE_RECOVERY.get()*(conditions!=null && conditions.malnourished()?.5:1));
                if (state.sprintLocked() || state.sprintRestartRequired()) player.setSprinting(false);
                if(conditions!=null) {
                    var damage=conditions.tick(state.energy(),state.hydration(),before,state.stamina(),state.spentStamina());
                    if(damage.dehydration()>0) hurtWithStatus(server,damage.dehydration(),true,com.minecrafteft.status.StatusDamage.DEHYDRATION);
                    if(damage.malnutrition()>0) hurtWithStatus(server,damage.malnutrition(),true,com.minecrafteft.status.StatusDamage.MALNUTRITION);
                }
            } else if(conditions!=null) conditions.inactiveTick(state.energy(),state.hydration());
            if (player.isAlive()) mirrorHealth(player, state);
            if (player.tickCount % 5 == 0) sync(server, state);
        });
    }

    /** Capture final damage after armor, resistance and absorption, not the incoming attack. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0) return;
        player.getCapability(VitalProvider.CAPABILITY).ifPresent(state -> {
            float amount = event.getAmount();
            state.damage(Float.isFinite(amount) ? amount : VitalState.MAX_HEALTH);
            if(state.health()>0) com.minecrafteft.status.StatusEvents.injured(player,event.getSource());
            if (state.health() == 0) {
                // Let vanilla perform death attribution, drops, death-screen and totem handling.
                event.setAmount(Math.max(1, player.getHealth()));
            } else {
                event.setAmount(0);
                player.getCombatTracker().recordDamage(event.getSource(), amount);
                player.gameEvent(GameEvent.ENTITY_DAMAGE);
                mirrorHealth(player, state);
            }
            sync(player, state);
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void heal(LivingHealEvent event) {
        // Hunger, peaceful difficulty, vanilla effects and unclassified mod heals
        // cannot regenerate this mod's health pool. Future regeneration buffs
        // must explicitly authorize recovery through the independent system.
        if (event.getEntity() instanceof ServerPlayer) event.setCanceled(true);
    }

    /** Direct treatment is distinct from passive regeneration. No vanilla heal event is forwarded. */
    public static void healWithTreatment(ServerPlayer player, float amount) {
        if (!player.isAlive() || !Float.isFinite(amount) || amount <= 0) return;
        player.getCapability(VitalProvider.CAPABILITY).ifPresent(state -> {
            state.heal(amount);
            mirrorHealth(player, state);
            sync(player, state);
        });
    }

    /** Injury damage bypasses armor, vanilla i-frames and injury rolls; cues are batched separately. */
    public static void hurtWithStatus(ServerPlayer player,double amount,boolean cue) {
        var status=com.minecrafteft.status.StatusProvider.of(player);
        var cause=status!=null && status.heavy()>0?com.minecrafteft.status.StatusDamage.HEAVY_BLEEDING:
                status!=null && status.light()>0?com.minecrafteft.status.StatusDamage.LIGHT_BLEEDING:com.minecrafteft.status.StatusDamage.FRACTURE;
        hurtWithStatus(player,amount,cue,cause);
    }
    public static void hurtWithStatus(ServerPlayer player,double amount,boolean cue,com.minecrafteft.status.StatusDamage cause) {
        if(!player.isAlive() || !Double.isFinite(amount) || amount<=0) return;
        player.getCapability(VitalProvider.CAPABILITY).ifPresent(state->{
            state.damage(amount);
            if(state.health()==0) {
                player.invulnerableTime=0;
                player.hurt(cause.source(player),1000000);
            } else {
                mirrorHealth(player,state);
                if(cue) player.serverLevel().broadcastDamageEvent(player,cause.source(player));
            }
            sync(player,state);
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void totem(LivingUseTotemEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            player.getCapability(VitalProvider.CAPABILITY).ifPresent(state -> state.revive(1));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) player.getCapability(VitalProvider.CAPABILITY).ifPresent(state -> {
            state.damage(VitalState.MAX_HEALTH);
            sync(player, state);
        });
    }

    @SubscribeEvent public static void finishItem(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) return;
        var stack = event.getItem();
        if (!stack.is(Items.POTION)) return;
        player.getCapability(VitalProvider.CAPABILITY).ifPresent(state -> {
            var potion = PotionUtils.getPotion(stack);
            if (potion == Potions.WATER) state.hydrate(VitalConfig.WATER_GAIN.get());
            else if (potion == Potions.AWKWARD) state.hydrate(-50);
            sync(player, state);
        });
    }

    private static void mirrorHealth(Player player, VitalState state) {
        // This is only an API/death compatibility mirror. The independent pool above
        // owns damage, healing and persistence; changing max-health attributes does not resize it.
        if (state.health() > 0 && player.getHealth() > 0)
            player.setHealth((float) (state.health() / VitalState.MAX_HEALTH * player.getMaxHealth()));
    }
    public static void sync(ServerPlayer player, VitalState state) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new VitalSyncPacket(state));
    }
}
