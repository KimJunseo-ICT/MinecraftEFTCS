package com.minecrafteft.vitals;

import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

/** Runtime integration fixtures. Compile separately; never included in the mod jar. */
@GameTestHolder("minecrafteft")
@PrefixGameTestTemplate(false)
public final class VitalGameTests {
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static FakePlayer player(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "VitalTest")) {
            @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource damage) { return false; }
        };
        net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(
                net.minecraft.server.level.ServerPlayer.class,player,0,"f_8921_"); // End the spawn protection window.
        player.setGameMode(GameType.SURVIVAL);
        VitalFoodData.install(player);
        return player;
    }
    private static VitalState state(FakePlayer player) {
        return player.getCapability(VitalProvider.CAPABILITY).orElseThrow(() -> new AssertionError("player capability attached"));
    }

    @GameTest(template = "empty")
    public static void independent_health_damage_healing_and_death(GameTestHelper helper) {
        var player = player(helper);
        var state = state(player);
        check(player.hurt(player.damageSources().generic(), 40), "hit accepted");
        check(Math.abs(state.health()-60)<.001 && player.isAlive(), "40 damage cannot kill the independent 100 HP player");
        player.heal(4);
        check(Math.abs(state.health()-60)<.001, "vanilla healing cannot regenerate independent health");
        VitalEvents.healWithTreatment(player,4);
        check(Math.abs(state.health()-64)<.001, "explicit medical treatment still restores independent health once");
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().generic(), 100);
        check(state.health()==0 && !player.isAlive(), "depleted independent HP triggers vanilla death");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void passive_and_potion_healing_are_blocked(GameTestHelper helper) {
        var player=player(helper);var state=state(player);state.damage(60);
        player.heal(1); // Same entry point used by peaceful difficulty.
        net.minecraft.world.effect.MobEffects.REGENERATION.applyEffectTick(player,0);
        net.minecraft.world.effect.MobEffects.HEAL.applyInstantenousEffect(null,null,player,0,1);
        check(state.health()==40,"peaceful and vanilla potion effects cannot regenerate mod health");
        var event=new net.minecraftforge.event.entity.living.LivingHealEvent(player,20);
        MinecraftForge.EVENT_BUS.post(event);
        check(event.isCanceled() && state.health()==40,"unclassified Forge healing is rejected");
        VitalEvents.healWithTreatment(player,20);
        check(state.health()==60,"medical recovery remains available while natural regeneration is disabled");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void food_and_potions_work_at_full_resource(GameTestHelper helper) {
        var player = player(helper);
        var state = state(player);
        check(player.canEat(false), "ordinary food usable even at full energy");
        var raw = new ItemStack(Items.BEEF);
        player.getFoodData().eat(raw.getItem(), raw, player);
        check(state.energy()==50, "raw meat removes fifty energy");
        var bread = new ItemStack(Items.BREAD);
        player.getFoodData().eat(bread.getItem(), bread, player);
        check(state.energy()==60, "normal food restores nutrition times two");
        player.getFoodData().eat(2, .1F);
        check(state.energy()==64, "cake and direct feeding restore energy too");
        var awkward = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.AWKWARD);
        MinecraftForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(player, awkward, 0, new ItemStack(Items.GLASS_BOTTLE)));
        check(state.hydration()==50, "awkward potion removes fifty water");
        var water = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);
        MinecraftForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(player, water, 0, new ItemStack(Items.GLASS_BOTTLE)));
        check(state.hydration()==75, "water bottle restores hydration");
        state.damage(50);
        double health = state.health();
        for (int i=0;i<400;i++) player.getFoodData().tick(player);
        check(state.health()==health, "vanilla hunger cannot regenerate custom health");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vitals_persist_and_totem_preserves_life(GameTestHelper helper) {
        var provider = new VitalProvider();
        provider.getCapability(VitalProvider.CAPABILITY, null).orElseThrow(IllegalStateException::new)
                .update(73, 45, 67, 8, true);
        var restored = new VitalProvider();
        restored.deserializeNBT(provider.serializeNBT());
        var saved = restored.getCapability(VitalProvider.CAPABILITY, null).orElseThrow(IllegalStateException::new);
        check(saved.health()==73 && saved.energy()==45 && saved.hydration()==67
                && saved.stamina()==8 && saved.sprintLocked(), "all resources and sprint lock roundtrip");
        var legacy = new net.minecraft.nbt.CompoundTag();
        legacy.putDouble("Health",160);
        legacy.putDouble("Energy",45);
        legacy.putDouble("Hydration",67);
        restored.deserializeNBT(legacy);
        check(saved.health()==80 && saved.energy()==45 && saved.hydration()==67,
                "legacy 200 HP save preserves percentage and resources under new 100 HP max");
        var player = player(helper);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        player.hurt(player.damageSources().generic(), 500);
        check(player.isAlive() && state(player).health()>0, "totem restores an independent HP point");
        check(player.getOffhandItem().isEmpty(), "totem consumed normally");
        helper.succeed();
    }
}
