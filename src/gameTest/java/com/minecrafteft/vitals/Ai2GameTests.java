package com.minecrafteft.vitals;

import com.minecrafteft.medical.*;
import com.minecrafteft.registry.ModContent;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder("minecrafteft")
@PrefixGameTestTemplate(false)
public final class Ai2GameTests {
    private static void check(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    private static FakePlayer player(GameTestHelper helper) {
        var player=new FakePlayer(helper.getLevel(),new GameProfile(UUID.randomUUID(),"Ai2Test"));
        player.setGameMode(GameType.SURVIVAL);
        player.getCapability(VitalProvider.CAPABILITY).ifPresent(state -> state.damage(50));
        return player;
    }
    private static double health(FakePlayer player) { return player.getCapability(VitalProvider.CAPABILITY).orElseThrow(IllegalStateException::new).health(); }
    private static ItemStack begin(FakePlayer player,int used) {
        var stack=new ItemStack(ModContent.AI2_MEDIKIT.get());
        stack.setDamageValue(used);
        player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        stack.getItem().use(player.level(),player,InteractionHand.MAIN_HAND);
        return stack;
    }
    @GameTest(template="empty")
    public static void ai2_cancel_before_and_during_healing(GameTestHelper helper) {
        var player=player(helper);
        var early=begin(player,0);
        early.onUseTick(player.level(),player,61);
        player.stopUsingItem();
        check(early.getDamageValue()==0 && health(player)==50,"early cancellation costs neither health nor charge");
        check(player.getCooldowns().isOnCooldown(ModContent.AI2_MEDIKIT.get()),"early cancellation also has a reuse delay");
        for(int tick=0;tick<Ai2TreatmentProgress.REUSE_DELAY_TICKS;tick++) player.getCooldowns().tick();
        var partial=begin(player,0);
        for(int elapsed=0;elapsed<80;elapsed++) partial.onUseTick(player.level(),player,120-elapsed);
        check(partial.getDamageValue()==1 && health(player)==60,"half treatment spends one charge and heals ten");
        player.getInventory().selected=1;
        player.stopUsingItem();
        partial.onUseTick(player.level(),player,30);
        check(partial.getDamageValue()==1 && health(player)==60,"switch stops healing without refund");
        check(player.getCooldowns().isOnCooldown(ModContent.AI2_MEDIKIT.get()),"partial treatment has the same reuse delay");
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void ai2_fifth_use_completes_before_kit_removal(GameTestHelper helper) {
        var player=player(helper);
        var kit=begin(player,4);
        for(int elapsed=0;elapsed<100;elapsed++) kit.onUseTick(player.level(),player,120-elapsed);
        check(!kit.isEmpty() && kit.getDamageValue()==5,"empty last-charge shell remains through treatment");
        check(health(player)==70,"fifth charge still heals full twenty");
        for(int elapsed=100;elapsed<120;elapsed++) kit.onUseTick(player.level(),player,120-elapsed);
        check(health(player)==70,"cleanup adds no extra healing");
        kit.getItem().finishUsingItem(kit,player.level(),player);
        check(kit.isEmpty(),"empty kit removed at end of sixth second");
        check(player.getCooldowns().isOnCooldown(ModContent.AI2_MEDIKIT.get()),"last charge locks the next kit");
        player.stopUsingItem();
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void ai2_reuse_delay_applies_across_hands(GameTestHelper helper) {
        var player=player(helper);
        var kit=begin(player,0);
        kit.onUseTick(player.level(),player,Ai2TreatmentProgress.USE_TICKS-79);
        // Release halfway through healing, then try a spare in the other hand.
        player.stopUsingItem();
        var spare=new ItemStack(ModContent.AI2_MEDIKIT.get());
        player.setItemInHand(InteractionHand.OFF_HAND,spare);
        for(int tick=0;tick<Ai2TreatmentProgress.REUSE_DELAY_TICKS;tick++) {
            spare.use(player.level(),player,InteractionHand.OFF_HAND);
            check(!player.isUsingItem(),"spare kit cannot bypass reuse delay at tick "+tick);
            player.getCooldowns().tick();
        }
        spare.use(player.level(),player,InteractionHand.OFF_HAND);
        check(player.isUsingItem() && player.getUsedItemHand()==InteractionHand.OFF_HAND,"ready on the first unlocked tick");
        player.stopUsingItem();
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void ai2_switch_to_another_kit_cannot_reuse_treatment(GameTestHelper helper) {
        var player=player(helper);
        var first=begin(player,0);
        first.onUseTick(player.level(),player,60);
        var second=new ItemStack(ModContent.AI2_MEDIKIT.get());
        player.setItemInHand(InteractionHand.MAIN_HAND,second);
        second.onUseTick(player.level(),player,40);
        check(!player.isUsingItem(),"switching even to same item type stops the old use");
        check(first.getDamageValue()==1 && second.getDamageValue()==0,"charge never transfers to replacement kit");
        check(health(player)==50.5,"replacement kit cannot continue old healing");
        helper.succeed();
    }
}
