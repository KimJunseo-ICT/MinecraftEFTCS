package com.minecrafteft.corpse;

import com.minecrafteft.inventory.*;
import com.minecrafteft.registry.ModContent;
import com.minecrafteft.stash.StashMenu;
import com.mojang.authlib.GameProfile;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("minecrafteft")
@PrefixGameTestTemplate(false)
public final class CorpseGameTests {
    private record Fixture(FakePlayer player,CorpseEntity body,CorpseMenu menu) { }
    private static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
    private static Fixture fixture(GameTestHelper helper) {
        var player=new FakePlayer(helper.getLevel(),new GameProfile(UUID.randomUUID(),"CorpseLootTest"));
        player.setGameMode(GameType.SURVIVAL);
        var pos=helper.absolutePos(new BlockPos(1,1,1));player.setPos(pos.getX(),pos.getY(),pos.getZ());
        var body=CorpseEntity.capture(player);
        check(helper.getLevel().addFreshEntity(body),"corpse added to world");
        CorpseIndex.get(player.server).replace(player.server,body);
        return new Fixture(player,body,new CorpseMenu(61,player.getInventory(),body));
    }
    private static ItemStack rifle(String serial) {
        var stack=GunItemBuilder.create().setId(ResourceLocation.parse("tacz:m4a1")).build();
        stack.getOrCreateTag().putString("Serial",serial);return stack;
    }
    private static void place(Fixture f,Slot target,ItemStack stack) {
        f.menu.setCarried(stack.copy());f.menu.clicked(target.index,0,ClickType.PICKUP,f.player);
        check(f.menu.getCarried().isEmpty() && ItemStack.matches(target.getItem(),stack),"valid equipment returns to corpse exactly once");
    }

    @GameTest(template="empty") public static void corpse_interaction_requires_close_range_in_both_game_modes(GameTestHelper helper) {
        var f=fixture(helper);
        for(var mode:new GameType[]{GameType.SURVIVAL,GameType.CREATIVE}) {
            f.player.setGameMode(mode);
            for(double distance:new double[]{0,2.49,2.5,2.51,4,8}) {
                f.player.setPos(f.body.getX()+distance,f.body.getY(),f.body.getZ());
                boolean near=distance<=2.5;
                check(f.body.canLoot(f.player)==near,"corpse interaction has a 2.5-block limit in "+mode);
                check(f.menu.stillValid(f.player)==near,"open corpse menu uses the same distance limit");
                if(!near) {
                    check(f.body.interact(f.player,InteractionHand.MAIN_HAND)==InteractionResult.PASS,"remote right-click cannot open corpse");
                    var torch=new ItemStack(Items.TORCH,3);f.menu.setCarried(torch.copy());
                    f.menu.clicked(f.menu.gear.get(4).index,0,ClickType.PICKUP,f.player);
                    check(ItemStack.matches(f.menu.getCarried(),torch) && f.menu.gear.get(4).getItem().isEmpty(),"remote click preserves cursor and corpse contents");
                }
            }
        }
        f.body.discard();helper.succeed();
    }

    @GameTest(template="empty") public static void corpse_bag_rotation_survives_placement_pickup_and_reload(GameTestHelper helper) {
        var f=fixture(helper);
        f.body.loot.items.setStackInSlot(CorpseInventory.BAG,new ItemStack(ModContent.DAY_PACK.get()));
        var gun=rifle("rotated-corpse-gun");f.menu.setCarried(gun.copy());
        check(f.menu.clickMenuButton(f.player,1),"corpse menu accepts R rotation");
        var rotated=f.menu.getCarried().copy();
        check(StashMenu.isRotated(rotated) && StashMenu.footprintWidth(rotated)==StashMenu.footprintHeight(gun)
                && StashMenu.footprintHeight(rotated)==StashMenu.footprintWidth(gun),"rotation exchanges the footprint axes");
        var first=f.menu.bagCells.get(0);place(f,first,rotated);
        check(StashMenu.isRotated(first.getItem()),"stored weapon retains the rotation used by its preview");
        var restored=new CorpseEntity(ModContent.CORPSE.get(),helper.getLevel());
        restored.load(f.body.saveWithoutId(new CompoundTag()));
        check(ItemStack.matches(restored.loot.bag.getStackInSlot(0),rotated),"saving corpse retains rotation and gun data");
        f.menu.clicked(first.index,0,ClickType.PICKUP,f.player);
        check(ItemStack.matches(f.menu.getCarried(),rotated) && first.getItem().isEmpty(),"pickup keeps the same rotated orientation");
        check(f.menu.clickMenuButton(f.player,1) && ItemStack.matches(f.menu.getCarried(),gun),"second rotation restores original orientation and data");
        f.body.discard();helper.succeed();
    }

    @GameTest(template="empty") public static void corpse_equipment_returns_respect_roles_and_readonly_dogtag(GameTestHelper helper) {
        var f=fixture(helper);var menu=f.menu;
        place(f,menu.gear.get(1),rifle("returned-primary"));
        place(f,menu.gear.get(39),new ItemStack(Items.IRON_HELMET));
        place(f,menu.gear.get(3),new ItemStack(Items.APPLE,12));
        place(f,menu.gear.get(40),new ItemStack(Items.TORCH,8));
        place(f,menu.gear.get(CorpseInventory.BAG),new ItemStack(ModContent.DAY_PACK.get()));
        check(!menu.gear.get(0).mayPlace(rifle("wrong-sidearm")),"rifles cannot enter corpse sidearm");
        check(!menu.gear.get(4).mayPlace(rifle("wrong-pocket")),"guns cannot enter corpse pockets");
        check(!menu.gear.get(36).mayPlace(new ItemStack(Items.IRON_HELMET)),"helmet cannot enter corpse feet slot");
        var melee=new ItemStack(Items.IRON_AXE);f.body.appearance().put("Melee",melee.save(new CompoundTag()));
        check(!menu.gear.get(0).mayPlace(melee) && !menu.gear.get(40).mayPlace(melee),"melee cannot bypass equipment roles");
        var tag=DogtagItem.issue(f.player);var tagSlot=menu.gear.get(CorpseInventory.DOGTAG);tagSlot.set(tag.copy());
        menu.setCarried(ItemStack.EMPTY);menu.clicked(tagSlot.index,0,ClickType.PICKUP,f.player);
        check(ItemStack.matches(menu.getCarried(),tag) && tagSlot.getItem().isEmpty(),"dogtag can still be taken");
        menu.clicked(tagSlot.index,0,ClickType.PICKUP,f.player);
        check(ItemStack.matches(menu.getCarried(),tag) && tagSlot.getItem().isEmpty(),"dogtag area remains extraction only");
        check(ItemStack.matches(ItemStack.of(f.body.appearance().getCompound("Melee")),melee),"read-only melee display is untouched");
        f.body.discard();helper.succeed();
    }

    @GameTest(template="empty") public static void corpse_bag_returns_preserve_nbt_and_reject_overlap(GameTestHelper helper) {
        var f=fixture(helper);var menu=f.menu;var body=f.body;
        body.loot.items.setStackInSlot(CorpseInventory.BAG,new ItemStack(ModContent.DAY_PACK.get()));
        var rifle=rifle("corpse-bag-roundtrip");var first=menu.bagCells.get(0);
        check(first.isActive() && !menu.bagCells.get(20).isActive(),"corpse bag activity uses local cell index");
        place(f,first,rifle);
        var incoming=StashMenu.rotatedCopy(rifle("must-stay-on-cursor"));menu.setCarried(incoming.copy());
        menu.clicked(first.index,0,ClickType.PICKUP,f.player);
        check(ItemStack.matches(first.getItem(),rifle) && ItemStack.matches(menu.getCarried(),incoming),"overlap neither replaces nor consumes a gun");
        menu.setCarried(ItemStack.EMPTY);menu.clicked(first.index,0,ClickType.PICKUP,f.player);
        menu.clicked(menu.loadout.hotbar.get(1).index,0,ClickType.PICKUP,f.player);
        check(ItemStack.matches(f.player.getInventory().getItem(1),rifle) && first.getItem().isEmpty(),"corpse gun moves into player equipment");
        menu.clicked(menu.loadout.hotbar.get(1).index,0,ClickType.PICKUP,f.player);menu.clicked(first.index,0,ClickType.PICKUP,f.player);
        check(menu.getCarried().isEmpty() && ItemStack.matches(first.getItem(),rifle),"taken gun can be returned to the corpse bag");
        place(f,menu.bagCells.get(19),new ItemStack(Items.APPLE,7));
        var saved=body.saveWithoutId(new CompoundTag());var restored=new CorpseEntity(ModContent.CORPSE.get(),helper.getLevel());restored.load(saved);
        check(ItemStack.matches(restored.loot.bag.getStackInSlot(0),rifle) && restored.loot.bag.getStackInSlot(19).getCount()==7,"body serialization preserves all returned loot");
        f.body.discard();helper.succeed();
    }

    @GameTest(template="empty") public static void corpse_shift_transfers_both_ways_and_preserves_remainders(GameTestHelper helper) {
        var f=fixture(helper);var menu=f.menu;var gun=rifle("shift-both-directions");
        var own=menu.loadout.hotbar.get(1);own.set(gun.copy());menu.quickMoveStack(f.player,own.index);
        check(own.getItem().isEmpty() && ItemStack.matches(menu.gear.get(1).getItem(),gun),"Shift moves own equipment to the corpse");
        menu.quickMoveStack(f.player,menu.gear.get(1).index);
        check(ItemStack.matches(own.getItem(),gun) && menu.gear.get(1).getItem().isEmpty(),"Shift moves corpse equipment back to player");
        menu.gear.get(1).set(rifle("full-one"));menu.gear.get(2).set(rifle("full-two"));
        var before=menu.loot.save().copy();menu.quickMoveStack(f.player,own.index);
        check(ItemStack.matches(own.getItem(),gun) && before.equals(menu.loot.save()),"failed transfer restores the source and keeps corpse contents");
        for(int i=3;i<=8;i++) menu.gear.get(i).set(new ItemStack(Items.STONE,64));
        menu.gear.get(40).set(new ItemStack(Items.STONE,64));menu.gear.get(3).set(new ItemStack(Items.APPLE,60));
        var supplies=menu.loadout.hotbar.get(3);supplies.set(new ItemStack(Items.APPLE,10));menu.quickMoveStack(f.player,supplies.index);
        check(menu.gear.get(3).getItem().getCount()==64 && supplies.getItem().getCount()==6,"partial transfer preserves exact total count");
        f.player.setPos(f.body.getX()+20,f.body.getY(),f.body.getZ());
        menu.setCarried(new ItemStack(Items.TORCH));menu.clicked(menu.gear.get(4).index,0,ClickType.PICKUP,f.player);
        check(menu.getCarried().is(Items.TORCH) && menu.gear.get(4).getItem().is(Items.STONE),"out-of-range corpse interaction is rejected");
        f.body.discard();helper.succeed();
    }
}
