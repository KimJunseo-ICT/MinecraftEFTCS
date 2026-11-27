package com.minecrafteft.medical;

import com.minecrafteft.MinecraftEFTMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Map;
import java.util.WeakHashMap;

/** Sessions stay server-side; only item durability persists after cancel/reconnect. */
@Mod.EventBusSubscriber(modid=MinecraftEFTMod.MOD_ID)
public final class Ai2Treatment {
    private record Session(ItemStack stack,InteractionHand hand,MedikitTreatmentProgress progress,TreatmentPlan plan) { }
    private static final Map<LivingEntity,Session> SESSIONS=new WeakHashMap<>();
    private Ai2Treatment() { }
    public static boolean using(LivingEntity entity) {
        return entity.isUsingItem() && entity.getUseItem().getItem() instanceof MedikitItem;
    }
    public static void begin(LivingEntity entity,ItemStack stack,InteractionHand hand) {
        if(entity.level().isClientSide) return;
        end(entity);
        var type=((MedikitItem)stack.getItem()).type;
        var plan=plan(entity,stack,type);
        SESSIONS.put(entity,new Session(stack,hand,new MedikitTreatmentProgress(type,plan),plan));
        entity.setSprinting(false);
    }
    public static TreatmentPlan plan(LivingEntity entity,ItemStack stack,MedikitType type) {
        boolean needsHealth=entity instanceof net.minecraft.world.entity.player.Player player
                ?player.getCapability(com.minecrafteft.vitals.VitalProvider.CAPABILITY).map(v->v.health()<com.minecrafteft.vitals.VitalState.MAX_HEALTH).orElse(false)
                :entity.getHealth()<entity.getMaxHealth();
        return TreatmentPlan.choose(type,stack.getMaxDamage()-stack.getDamageValue(),com.minecrafteft.status.StatusProvider.of(entity),needsHealth);
    }
    public static void tick(LivingEntity entity,ItemStack stack,int elapsed) {
        if(entity.level().isClientSide) return;
        Session session=SESSIONS.get(entity);
        if(session==null) return;
        if(session.stack()!=stack || !using(entity)
                || entity.getItemInHand(session.hand())!=stack || !entity.isAlive()) {
            end(entity);
            if(using(entity)) entity.stopUsingItem();
            return;
        }
        entity.setSprinting(false);
        var step=session.progress().advance(elapsed);
        if(step.spendCharge()) {
            if(stack.getDamageValue()>=stack.getMaxDamage()) { end(entity);entity.stopUsingItem();return; }
            // Defer removal of the final charge until finish/cancel, so the fifth
            // treatment still gets its full healing and disposal animation.
            stack.setDamageValue(stack.getDamageValue()+1);
            if(entity instanceof ServerPlayer player) player.getInventory().setChanged();
        }
        if(step.heal()>0) {
            if(entity instanceof ServerPlayer player) com.minecrafteft.vitals.VitalEvents.healWithTreatment(player,step.heal());
            else entity.heal(step.heal());
        }
    }
    public static void end(LivingEntity entity) {
        if(entity.level().isClientSide) return;
        Session session=SESSIONS.remove(entity);
        if(session==null) return;
        // Every ended use has the same delay, including preparation or partial healing.
        // Cooldowns are per item type, so switching hands or kits cannot bypass it.
        if(entity instanceof ServerPlayer player)
            player.getCooldowns().addCooldown(session.stack().getItem(),Ai2TreatmentProgress.REUSE_DELAY_TICKS);
        spend(session.stack(),session.progress().cancel());
        removeSpent(entity,session);
    }
    public static void finish(LivingEntity entity) {
        if(entity.level().isClientSide) return;
        Session session=SESSIONS.remove(entity);if(session==null) return;
        int cost=session.progress().finish();
        var state=com.minecrafteft.status.StatusProvider.of(entity);
        boolean cured=state!=null && switch(session.plan().target()) {
            case HEAVY -> state.cureHeavy();case LIGHT -> state.cureLight();default -> false;
        };
        if(cured) spend(session.stack(),cost);
        if(entity instanceof ServerPlayer player) {
            com.minecrafteft.profile.ProfileEvents.count(player,"Medicine");
            player.getCooldowns().addCooldown(session.stack().getItem(),Ai2TreatmentProgress.REUSE_DELAY_TICKS);
            com.minecrafteft.status.StatusEvents.sync(player);
        }
        removeSpent(entity,session);
    }
    private static void spend(ItemStack stack,int charges) { stack.setDamageValue(Math.min(stack.getMaxDamage(),stack.getDamageValue()+charges)); }
    private static void removeSpent(LivingEntity entity,Session session) {
        if(session.stack().getDamageValue()>=session.stack().getMaxDamage()) {
            if(entity.getItemInHand(session.hand())==session.stack()) entity.broadcastBreakEvent(session.hand());
            session.stack().shrink(1);
        }
        if(entity instanceof ServerPlayer player) player.getInventory().setChanged();
    }
    static boolean activeStack(LivingEntity entity,ItemStack stack) {
        Session session=SESSIONS.get(entity);
        return session!=null && session.stack()==stack;
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if(!(event.player instanceof ServerPlayer player)) return;
        if(using(player)) player.setSprinting(false);
        Session session=SESSIONS.get(player);
        if(event.phase==TickEvent.Phase.END && session!=null
                && (!using(player) || !player.isAlive() || player.getUseItem()!=session.stack()
                || player.getItemInHand(session.hand())!=session.stack())) {
            end(player);
            if(using(player)) player.stopUsingItem();
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { end(event.getEntity()); }
    @SubscribeEvent public static void jump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        if(using(event.getEntity())) {
            var velocity=event.getEntity().getDeltaMovement();
            event.getEntity().setDeltaMovement(velocity.x,Math.min(0,velocity.y),velocity.z);
        }
    }
}
