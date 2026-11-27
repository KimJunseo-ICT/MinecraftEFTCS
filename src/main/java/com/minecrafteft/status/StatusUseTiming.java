package com.minecrafteft.status;

import com.minecrafteft.MinecraftEFTMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Freeze the rate at use start; recovery/painkiller expiry mid-animation cannot jump the timeline. */
@Mod.EventBusSubscriber(modid=MinecraftEFTMod.MOD_ID)
public final class StatusUseTiming {
    private record Timing(int ticks,int base) { }
    private static final Map<LivingEntity,Timing> USES=new WeakHashMap<>();
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void statusUseStarted(LivingEntityUseItemEvent.Start event) {
        var state=StatusProvider.of(event.getEntity());
        int base=event.getDuration();
        if(base<=0 || base>=72000) { USES.remove(event.getEntity());return; }
        double factor=state==null?1:state.useMultiplier();
        int ticks=(int)Math.ceil(base*factor);event.setDuration(ticks);
        USES.put(event.getEntity(),new Timing(ticks,base));
    }
    public static float elapsed(LivingEntity entity,int remaining,float partial) {
        var timing=USES.get(entity);
        if(timing==null) return entity.getUseItem().getUseDuration()-remaining+partial;
        return Math.max(0,Math.min(timing.base,(timing.ticks-remaining+partial)*timing.base/(float)timing.ticks));
    }
    public static void clear(LivingEntity entity) { USES.remove(entity); }
}
