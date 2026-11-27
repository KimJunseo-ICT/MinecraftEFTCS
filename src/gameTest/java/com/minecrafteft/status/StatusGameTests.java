package com.minecrafteft.status;

import com.minecrafteft.medical.*;
import com.minecrafteft.registry.ModContent;
import com.minecrafteft.vitals.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("minecrafteft")
@PrefixGameTestTemplate(false)
public final class StatusGameTests {
    private static void check(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    private static FakePlayer player(GameTestHelper helper) {
        var player=new FakePlayer(helper.getLevel(),new GameProfile(UUID.randomUUID(),"StatusTest"));player.setGameMode(GameType.SURVIVAL);
        player.getCapability(VitalProvider.CAPABILITY).ifPresent(v->v.damage(50));return player;
    }
    private static ItemStack use(FakePlayer player,Item item,int damage) {
        var stack=new ItemStack(item);stack.setDamageValue(damage);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        item.use(player.level(),player,InteractionHand.MAIN_HAND);return stack;
    }
    private static void cooldown(FakePlayer p) { for(int i=0;i<6;i++) p.getCooldowns().tick(); }
    private static double hp(FakePlayer p) { return p.getCapability(VitalProvider.CAPABILITY).orElseThrow(IllegalStateException::new).health(); }
    @GameTest(template="empty") public static void bleeding_waits_for_cleanup_and_cancellation_never_cures(GameTestHelper helper) {
        var player=player(helper);var status=StatusProvider.of(player);status.addHeavy();status.addLight();
        var kit=use(player,ModContent.IFAK_MEDIKIT.get(),0);
        for(int elapsed=0;elapsed<124;elapsed++) kit.onUseTick(player.level(),player,124-elapsed);
        check(kit.getDamageValue()==0 && status.heavy()==1 && status.light()==1 && hp(player)==50,"no early charge, cure or HP during bleeding treatment");
        kit.finishUsingItem(player.level(),player);player.stopUsingItem();
        check(kit.getDamageValue()==3 && status.heavy()==0 && status.light()==1 && status.wounds().size()==1,"complete cleanup spends three charges and converts one stack");
        cooldown(player);var lightKit=use(player,ModContent.AI2_MEDIKIT.get(),0);
        lightKit.onUseTick(player.level(),player,59);player.stopUsingItem();
        check(lightKit.getDamageValue()==1 && status.light()==1 && hp(player)==50,"mid-treatment cancellation loses one charge and does not cure or heal");
        cooldown(player);lightKit=use(player,ModContent.AI2_MEDIKIT.get(),0);lightKit.onUseTick(player.level(),player,61);player.stopUsingItem();
        check(lightKit.getDamageValue()==0 && status.light()==1,"preparation cancellation costs no bleeding charges");helper.succeed();
    }
    @GameTest(template="empty") public static void insufficient_heavy_charges_fall_back_and_final_light_use_breaks_once(GameTestHelper helper) {
        var player=player(helper);var status=StatusProvider.of(player);status.addHeavy();status.addLight();
        var kit=use(player,ModContent.IFAK_MEDIKIT.get(),6);
        kit.onUseTick(player.level(),player,50);kit.finishUsingItem(player.level(),player);player.stopUsingItem();
        check(kit.getDamageValue()==7 && status.heavy()==1 && status.light()==0 && hp(player)==50,"two remaining charges skip heavy and treat light");
        status.addLight();var ai2=use(player,ModContent.AI2_MEDIKIT.get(),3);
        ai2.onUseTick(player.level(),player,50);ai2.finishUsingItem(player.level(),player);player.stopUsingItem();
        check(ai2.isEmpty() && status.heavy()==1 && status.light()==0,"two-charge AI2 completion cures light and removes shell once");helper.succeed();
    }
    @GameTest(template="empty") public static void arm_fracture_stretches_use_and_motion_without_extra_healing(GameTestHelper helper) {
        var player=player(helper);var status=StatusProvider.of(player);status.fracture(0);
        var kit=use(player,ModContent.AI2_MEDIKIT.get(),0);
        check(player.getUseItemRemainingTicks()==180,"arm fracture lengthens AI2 to nine seconds");
        for(int elapsed=0;elapsed<180;elapsed++) kit.onUseTick(player.level(),player,180-elapsed);
        check(hp(player)==70 && kit.getDamageValue()==1,"longer use retains exact twenty HP and one charge");
        check(StatusUseTiming.elapsed(player,90,0)==60,"motion and healing share the normalized timeline");
        kit.finishUsingItem(player.level(),player);player.stopUsingItem();cooldown(player);status.painkillers();
        kit=use(player,ModContent.AI2_MEDIKIT.get(),0);check(player.getUseItemRemainingTicks()==150,"painkillers reduce arm penalty to twenty-five percent");
        player.stopUsingItem();
        player.getFoodData().setFoodLevel(0);var food=use(player,Items.APPLE,0);
        check(player.isUsingItem() && player.getUseItemRemainingTicks()==40,"ordinary food use is stretched without changing the food item");
        player.stopUsingItem();helper.succeed();
    }
    @GameTest(template="empty") public static void temporary_tools_and_status_nbt_preserve_independent_wounds(GameTestHelper helper) {
        var player=player(helper);var status=StatusProvider.of(player);status.fracture(0);status.fracture(0);
        var splint=use(player,ModContent.ALU_SPLINT.get(),0);check(player.getUseItemRemainingTicks()==90,"arm fracture also stretches splint use");
        player.stopUsingItem();check(splint.getDamageValue()==0 && status.leg(),"interrupted splint consumes nothing");
        splint=use(player,ModContent.ALU_SPLINT.get(),0);splint.finishUsingItem(player.level(),player);player.stopUsingItem();
        check(splint.getDamageValue()==1 && !status.leg() && status.arm(),"splint consumes once and heals leg first");
        var analgin=use(player,ModContent.ANALGIN.get(),0);analgin.finishUsingItem(player.level(),player);player.stopUsingItem();
        check(status.painTicks()==1600 && analgin.getDamageValue()==1,"analgin grants eighty seconds after completion");
        status.addHeavy();status.cureHeavy();for(int i=0;i<120;i++) status.tick(false,()->1);status.addHeavy();status.cureHeavy();
        var provider=new StatusProvider();var tag=new CompoundTag();tag.putIntArray("Status",status.snapshot());provider.deserializeNBT(tag);
        check(java.util.Arrays.equals(provider.serializeNBT().getIntArray("Status"),status.snapshot()),"NBT retains each wound and all simulation counters");helper.succeed();
    }
    @GameTest(template="empty") public static void status_damage_uses_independent_hp_even_with_vanilla_invulnerability(GameTestHelper helper) {
        var player=player(helper);player.invulnerableTime=20;
        VitalEvents.hurtWithStatus(player,1,false);VitalEvents.hurtWithStatus(player,1.25,false);
        check(hp(player)==47.75,"gradual bleeding ignores hurt invulnerability without changing amount");
        check(player.hurtTime==0,"silent bleeding does not restart hurt animation every second");helper.succeed();
    }
}
