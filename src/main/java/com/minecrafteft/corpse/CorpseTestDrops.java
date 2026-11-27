package com.minecrafteft.corpse;

import com.minecrafteft.MinecraftEFTMod;
import com.minecrafteft.inventory.DogtagItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Temporary opt-out test drop; separate from player corpse capture and ordinary mob loot. */
@Mod.EventBusSubscriber(modid=MinecraftEFTMod.MOD_ID)
public final class CorpseTestDrops {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ZOMBIE_VILLAGER_DOGTAGS;
    static {
        var builder=new ForgeConfigSpec.Builder();
        ZOMBIE_VILLAGER_DOGTAGS=builder.comment("Temporary test: player-killed zombie villagers drop one recorded dogtag. Set false after testing.")
                .define("zombieVillagerDogtags",true);
        SPEC=builder.build();
    }
    private CorpseTestDrops() { }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void dropZombieVillagerTestDogtag(LivingDropsEvent event) {
        if(!(event.getEntity() instanceof ZombieVillager victim) || victim.level().isClientSide) return;
        var gunId=CorpseDeath.takeGunId(event.getSource());
        if(!ZOMBIE_VILLAGER_DOGTAGS.get() || !(event.getSource().getEntity() instanceof ServerPlayer)) return;
        var tag=DogtagItem.issue(victim);
        DogtagItem.recordDeath(tag,victim,event.getSource(),gunId);
        event.getDrops().add(new ItemEntity(victim.level(),victim.getX(),victim.getY(),victim.getZ(),tag));
    }
}
