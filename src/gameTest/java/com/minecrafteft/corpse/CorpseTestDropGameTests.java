package com.minecrafteft.corpse;

import com.minecrafteft.inventory.DogtagItem;
import com.minecrafteft.registry.ModContent;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("minecrafteft")
@PrefixGameTestTemplate(false)
public final class CorpseTestDropGameTests {
    private static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
    private static FakePlayer killer(GameTestHelper helper) {
        return new FakePlayer(helper.getLevel(),new GameProfile(UUID.randomUUID(),"DogtagTestKiller"));
    }
    private static LivingDropsEvent drops(GameTestHelper helper,LivingEntity victim,FakePlayer killer) {
        var items=new ArrayList<ItemEntity>();
        items.add(new ItemEntity(helper.getLevel(),0,0,0,new ItemStack(Items.ROTTEN_FLESH,2)));
        return new LivingDropsEvent(victim,killer==null?victim.damageSources().fall():victim.damageSources().playerAttack(killer),items,0,killer!=null);
    }
    @GameTest(template="empty") public static void zombie_villager_test_tag_records_the_real_victim_and_gun(GameTestHelper helper) {
        boolean enabled=CorpseTestDrops.ZOMBIE_VILLAGER_DOGTAGS.get();
        CorpseTestDrops.ZOMBIE_VILLAGER_DOGTAGS.set(true);
        try {
            var victim=EntityType.ZOMBIE_VILLAGER.create(helper.getLevel());
            victim.setCustomName(Component.literal("Test Villager"));var killer=killer(helper);
            var event=drops(helper,victim,killer);var gun=ResourceLocation.parse("tacz:m4a1");
            CorpseDeath.gunHit(new com.tacz.guns.api.event.common.EntityHurtByGunEvent.Pre(null,victim,killer,gun,gun,100,
                    org.apache.commons.lang3.tuple.Pair.of(event.getSource(),event.getSource()),false,1,net.minecraftforge.fml.LogicalSide.SERVER));
            CorpseTestDrops.dropZombieVillagerTestDogtag(event);
            var tags=event.getDrops().stream().filter(drop->drop.getItem().getItem() instanceof DogtagItem).toList();
            check(tags.size()==1 && tags.get(0).getItem().getCount()==1,"player kill drops exactly one test dogtag");
            check(event.getDrops().stream().anyMatch(drop->drop.getItem().is(Items.ROTTEN_FLESH) && drop.getItem().getCount()==2),"ordinary loot is preserved");
            var data=tags.get(0).getItem().getTag();
            check(data.getUUID("Owner").equals(victim.getUUID()) && data.getString("OwnerName").equals("Test Villager"),"tag identifies victim, including custom names");
            check(data.getInt("Level")==1 && !data.getString("DeathTime").isBlank(),"death detail fields are recorded");
            check(Component.Serializer.fromJson(data.getString("Killer")).getString().contains("DogtagTestKiller"),"actual killer is recorded");
            check(data.getString("GunId").equals(gun.toString()),"exact TACZ damage source supplies gun identity");
            check(CorpseDeath.takeGunId(event.getSource())==null,"death consumes gun-hit cache entry");
            var meleeEvent=drops(helper,victim,killer);CorpseTestDrops.dropZombieVillagerTestDogtag(meleeEvent);
            var meleeTag=meleeEvent.getDrops().stream().filter(drop->drop.getItem().is(ModContent.DOGTAG.get())).findFirst().orElseThrow().getItem().getTag();
            check(!meleeTag.contains("GunId") && !meleeTag.getString("Cause").isBlank(),"non-gun kill records its cause without a false gun ID");
            helper.succeed();
        } finally { CorpseTestDrops.ZOMBIE_VILLAGER_DOGTAGS.set(enabled); }
    }
    @GameTest(template="empty") public static void test_tag_excludes_natural_deaths_other_mobs_and_disabled_config(GameTestHelper helper) {
        boolean enabled=CorpseTestDrops.ZOMBIE_VILLAGER_DOGTAGS.get();
        try {
            CorpseTestDrops.ZOMBIE_VILLAGER_DOGTAGS.set(true);
            var victim=EntityType.ZOMBIE_VILLAGER.create(helper.getLevel());var killer=killer(helper);
            var natural=drops(helper,victim,null);CorpseTestDrops.dropZombieVillagerTestDogtag(natural);
            check(natural.getDrops().size()==1,"environmental death cannot produce a test dogtag");
            var ordinary=drops(helper,EntityType.ZOMBIE.create(helper.getLevel()),killer);
            CorpseTestDrops.dropZombieVillagerTestDogtag(ordinary);
            check(ordinary.getDrops().size()==1,"ordinary zombie cannot produce a test dogtag");
            CorpseTestDrops.ZOMBIE_VILLAGER_DOGTAGS.set(false);
            var disabled=drops(helper,victim,killer);CorpseTestDrops.dropZombieVillagerTestDogtag(disabled);
            check(disabled.getDrops().size()==1,"config disables the temporary test drop");helper.succeed();
        } finally { CorpseTestDrops.ZOMBIE_VILLAGER_DOGTAGS.set(enabled); }
    }
}
