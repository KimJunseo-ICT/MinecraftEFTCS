package com.minecrafteft.inventory;

import com.minecrafteft.registry.ModContent;
import com.minecrafteft.stash.*;
import com.mojang.authlib.GameProfile;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("minecrafteft")
@PrefixGameTestTemplate(false)
public final class LoadoutGameTests {
    private static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
    private static FakePlayer player(GameTestHelper helper) {
        var player=new FakePlayer(helper.getLevel(),new GameProfile(UUID.randomUUID(),"LoadoutTest"));player.setGameMode(GameType.SURVIVAL);return player;
    }
    private static ItemStack bag() {
        var bag=new ItemStack(Items.CHEST);var data=new CompoundTag();data.putString("Kind","backpack");data.putInt("Width",4);data.putInt("Height",5);bag.getOrCreateTag().put(StorageEquipment.DATA,data);return bag;
    }
    private static ItemStack gun(String id) { return GunItemBuilder.create().setId(ResourceLocation.parse("tacz:"+id)).build(); }
    private static void rejectBagReplacement(net.minecraft.world.inventory.AbstractContainerMenu menu,
            net.minecraft.world.inventory.Slot cell,LoadoutSlots loadout,net.minecraft.world.entity.player.Player player,boolean rotate) {
        var stored=gun("m4a1");stored.getOrCreateTag().putString("Serial","already-in-bag");
        var incoming=gun("m4a1");incoming.getOrCreateTag().putString("Serial","carried-weapon");
        if(rotate) incoming=StashMenu.rotatedCopy(incoming);
        cell.set(stored.copy());menu.setCarried(incoming.copy());
        check(!cell.mayPlace(incoming),"occupied anchor must reject an incoming weapon, including rotated weapons");
        for(int button:new int[]{0,1}) {
            menu.clicked(cell.index,button,ClickType.PICKUP,player);
            check(ItemStack.matches(cell.getItem(),stored) && ItemStack.matches(menu.getCarried(),incoming),"blocked click preserves both guns and their full NBT");
        }
        loadout.hotbar.get(1).set(menu.getCarried());menu.setCarried(ItemStack.EMPTY);
        menu.clicked(cell.index,1,ClickType.SWAP,player);
        check(ItemStack.matches(cell.getItem(),stored) && ItemStack.matches(loadout.hotbar.get(1).getItem(),incoming),"number-key swap cannot replace an occupied bag weapon");
        menu.clicked(cell.index,0,ClickType.PICKUP,player);
        menu.clicked(loadout.hotbar.get(2).index,0,ClickType.PICKUP,player);
        check(cell.getItem().isEmpty() && ItemStack.matches(loadout.hotbar.get(2).getItem(),stored),"existing gun must first be removed to equipment");
        menu.clicked(loadout.hotbar.get(1).index,0,ClickType.PICKUP,player);
        check(cell.mayPlace(menu.getCarried()),"now-empty rectangle accepts the carried weapon");
        menu.clicked(cell.index,0,ClickType.PICKUP,player);
        check(menu.getCarried().isEmpty() && ItemStack.matches(cell.getItem(),incoming)
                && loadout.hotbar.get(1).getItem().isEmpty(),"explicit empty-then-fill moves each gun exactly once");
    }

    @GameTest(template="empty") public static void all_backpack_surfaces_reject_weapon_replacement(GameTestHelper helper) {
        for(boolean rotated:new boolean[]{false,true}) for(int surface=0;surface<4;surface++) {
            var player=player(helper);var host=new ItemStack(ModContent.DAY_PACK.get());
            if(surface==0 || surface==1) {
                var state=LoadoutState.of(player);state.equipment.setStackInSlot(LoadoutState.BACKPACK,host);
                if(surface==0) {
                    var menu=new LoadoutMenu(54,player.getInventory());
                    rejectBagReplacement(menu,menu.loadout.backpack.get(0),menu.loadout,player,rotated);
                } else {
                    var record=new StashSavedData().createRecord(player.getUUID(),StashTier.FOUR);
                    var menu=new StashMenu(55,player.getInventory(),record);
                    rejectBagReplacement(menu,menu.loadout.backpack.get(0),menu.loadout,player,rotated);
                }
                check(!state.backpack.canPlace(1,gun("glock_17")),"reserved cells also reject overlap");
                check(!state.backpack.insertItem(0,gun("glock_17"),true).isEmpty(),"simulated insertion cannot replace a weapon");
                state.backpack.setStackInSlot(19,new ItemStack(Items.APPLE,12));
                check(state.backpack.insertItem(19,new ItemStack(Items.APPLE,5),false).isEmpty()
                        && state.backpack.getStackInSlot(19).getCount()==17,"ordinary supplies still merge");
                check(!state.backpack.canPlace(19,new ItemStack(Items.STONE)),"different supplies cannot swap over an occupied cell");
            } else if(surface==2) {
                player.getInventory().setItem(3,host);var menu=new PortableBackpackMenu(56,player.getInventory(),3);
                rejectBagReplacement(menu,menu.bagSlots.get(0),menu.loadout,player,rotated);
            } else {
                var record=new StashSavedData().createRecord(player.getUUID(),StashTier.FOUR);record.items().setStackInSlot(0,host);
                var menu=new StashMenu(57,player.getInventory(),record);menu.clickMenuButton(player,StashMenu.OPEN_CASE_BUTTON);
                rejectBagReplacement(menu,menu.caseSlot(0),menu.loadout,player,rotated);
            }
        }
        helper.succeed();
    }
    @GameTest(template="empty") public static void equipment_weapon_and_armor_filters(GameTestHelper helper) {
        var player=player(helper);var menu=new LoadoutMenu(41,player.getInventory());
        check(menu.loadout.hotbar.get(0).mayPlace(gun("glock_17")),"pistol fits key 1");
        check(!menu.loadout.hotbar.get(1).mayPlace(gun("glock_17")),"pistol rejected from primary 1");
        check(menu.loadout.hotbar.get(1).mayPlace(gun("m4a1")),"rifle fits primary 1");
        check(!menu.loadout.hotbar.get(0).mayPlace(gun("m4a1")),"rifle rejected from sidearm");
        for(int i=3;i<9;i++) {
            check(!menu.loadout.hotbar.get(i).mayPlace(gun("m4a1")),"rifles cannot enter pockets");
            check(!menu.loadout.hotbar.get(i).mayPlace(gun("glock_17")),"pistols cannot enter pockets");
            check(!menu.loadout.hotbar.get(i).mayPlace(new ItemStack(Items.IRON_AXE)),"melee tools cannot enter pockets");
            check(menu.loadout.hotbar.get(i).mayPlace(new ItemStack(Items.APPLE)),"ordinary supplies still fit pockets");
        }
        check(menu.loadout.melee.mayPlace(new ItemStack(Items.DIAMOND_PICKAXE)),"vanilla tools fit melee");
        check(!menu.loadout.melee.mayPlace(new ItemStack(Items.APPLE)),"food rejected from melee");
        check(menu.loadout.armor.get(0).mayPlace(new ItemStack(Items.IRON_HELMET)),"helmet fits head");
        check(!menu.loadout.armor.get(0).mayPlace(new ItemStack(Items.IRON_BOOTS)),"boots rejected from head");
        check(!menu.loadout.dogtag.mayPickup(player),"live personal dogtag is reserved for future corpse looting");helper.succeed();
    }
    @GameTest(template="empty") public static void legacy_inventory_migration_preserves_all_items(GameTestHelper helper) {
        var player=player(helper);
        for(int i=0;i<36;i++) player.getInventory().setItem(i,new ItemStack(Items.STONE,64));
        LoadoutInventory.normalize(player);int count=0;
        for(int i=0;i<36;i++) { var stack=player.getInventory().getItem(i);count+=stack.getCount();if(i<3 || i>=9) check(stack.isEmpty(),"only six pockets remain active"); }
        var state=LoadoutState.of(player);for(int i=0;i<36;i++) count+=state.recovery.getStackInSlot(i).getCount();
        check(count==36*64,"migration neither deletes nor duplicates any stack");
        LoadoutInventory.normalize(player);check(state.recovery.getStackInSlot(0).getCount()==64,"repeat normalization does not repeat migration");helper.succeed();
    }
    @GameTest(template="empty") public static void grid_rotation_and_equipment_removal_preserve_nested_contents(GameTestHelper helper) {
        var state=LoadoutState.of(player(helper));ItemStack host=bag();state.equipment.setStackInSlot(LoadoutState.BACKPACK,host);
        var weaponCase=new ItemStack(ModContent.WEAPON_CASE_ITEM.get());weaponCase.getOrCreateTag().putString("Marker","same case after moving backpack");
        check(StashMenu.footprintWidth(weaponCase)==5,"fixture requires rotation");
        check(state.backpack.insert(weaponCase).isEmpty(),"5x3 case rotates into 4x5 backpack");
        check(StashMenu.footprintWidth(state.backpack.getStackInSlot(0))==3,"stored orientation is rotated");
        check(!state.backpack.canPlace(4,new ItemStack(Items.STONE)),"reserved footprint rejects overlap");
        check(!state.backpack.canPlace(3,bag()),"nested backpacks cannot create recursive storage");
        state.backpack.flush();state.equipment.setStackInSlot(LoadoutState.BACKPACK,ItemStack.EMPTY);
        check(state.backpack.size().cells()==0 && state.backpack.getStackInSlot(0).isEmpty(),"unequipped storage is inaccessible");
        var restored=new LoadoutState();restored.equipment.setStackInSlot(LoadoutState.BACKPACK,host.copy());
        check("same case after moving backpack".equals(restored.backpack.getStackInSlot(0).getTag().getString("Marker")),"host carries full nested NBT");
        CompoundTag saved=restored.save();var loaded=new LoadoutState();loaded.load(saved);
        check(loaded.backpack.getStackInSlot(0).getCount()==1,"save/load keeps exactly one stored case");helper.succeed();
    }
    @GameTest(template="empty") public static void melee_swap_round_trip_and_save_keep_both_items(GameTestHelper helper) {
        var player=player(helper);var state=LoadoutState.of(player);var rifle=gun("m4a1");var tool=new ItemStack(Items.IRON_AXE);
        player.getInventory().selected=1;player.getInventory().setItem(1,rifle);state.equipment.setStackInSlot(LoadoutState.MELEE,tool);
        state.toggleMelee(player);check(player.getMainHandItem()==tool && state.equipment.getStackInSlot(LoadoutState.MELEE)==rifle,"U draws real usable tool and reserves original weapon");
        var loaded=new LoadoutState();loaded.load(state.save());check(loaded.meleeHost==1,"active swap persists for recovery on login");
        state.restoreMelee(player);check(player.getMainHandItem()==rifle && state.equipment.getStackInSlot(LoadoutState.MELEE)==tool,"holstering restores both objects exactly once");helper.succeed();
    }
    @GameTest(template="empty") public static void number_swap_cannot_bypass_sidearm_filter(GameTestHelper helper) {
        var player=player(helper);var menu=new LoadoutMenu(42,player.getInventory());var apple=new ItemStack(Items.APPLE);menu.loadout.hotbar.get(3).set(apple);
        menu.clicked(menu.loadout.hotbar.get(3).index,0,ClickType.SWAP,player);
        check(menu.loadout.hotbar.get(0).getItem().isEmpty() && menu.loadout.hotbar.get(3).getItem()==apple,"number-key swap respects weapon slot rule");helper.succeed();
    }
    @GameTest(template="empty") public static void pocket_upgrade_recovers_weapons_and_blocks_number_swap(GameTestHelper helper) {
        var player=player(helper);var state=LoadoutState.of(player);state.migrated=true;state.pocketRules=1;
        player.getInventory().setItem(0,gun("glock_17"));player.getInventory().setItem(1,gun("m4a1"));player.getInventory().setItem(2,gun("m4a1"));
        state.equipment.setStackInSlot(LoadoutState.MELEE,new ItemStack(Items.IRON_AXE));
        player.getInventory().setItem(3,gun("m4a1"));player.getInventory().setItem(4,new ItemStack(Items.DIAMOND_SWORD));
        LoadoutInventory.normalize(player);
        check(player.getInventory().getItem(3).isEmpty() && player.getInventory().getItem(4).isEmpty(),"old pocket weapons leave quick-use slots");
        check(!state.recovery.getStackInSlot(0).isEmpty() && !state.recovery.getStackInSlot(1).isEmpty(),"full equipment sends both legacy weapons to recovery");
        var menu=new LoadoutMenu(43,player.getInventory());var original=menu.loadout.hotbar.get(1).getItem().copy();
        menu.clicked(menu.loadout.hotbar.get(1).index,3,ClickType.SWAP,player);
        check(menu.loadout.hotbar.get(3).getItem().isEmpty() && ItemStack.matches(menu.loadout.hotbar.get(1).getItem(),original),"number key 4 cannot move a primary weapon into pockets");
        var drop=gun("m4a1");check(!LoadoutInventory.insert(player,drop) && drop.getCount()==1,"pickup does not use empty pockets when weapon slots are full");
        helper.succeed();
    }

    @GameTest(template="empty") public static void offhand_rejects_drag_and_swap_and_recovers_legacy_weapon(GameTestHelper helper) {
        var player=player(helper);var menu=new LoadoutMenu(44,player.getInventory());
        for(var stack:new ItemStack[]{gun("m4a1"),gun("glock_17"),new ItemStack(Items.IRON_AXE)}) {
            menu.setCarried(stack.copy());menu.clicked(menu.loadout.offhand.index,0,ClickType.PICKUP,player);
            check(ItemStack.matches(menu.getCarried(),stack) && player.getOffhandItem().isEmpty(),"offhand drag rejects weapons without consuming them");
        }
        menu.setCarried(ItemStack.EMPTY);menu.loadout.hotbar.get(1).set(gun("m4a1"));
        menu.clicked(menu.loadout.hotbar.get(1).index,40,ClickType.SWAP,player);
        check(player.getOffhandItem().isEmpty() && menu.loadout.hotbar.get(1).hasItem(),"F cannot bypass offhand restriction");
        player.getInventory().setItem(40,gun("glock_17"));var state=LoadoutState.of(player);state.migrated=true;state.pocketRules=2;
        LoadoutInventory.normalize(player);
        check(player.getOffhandItem().isEmpty() && StashMenu.isTaczGun(player.getInventory().getItem(0)),"legacy offhand pistol moves to sidearm");
        check(menu.loadout.offhand.mayPlace(new ItemStack(Items.TORCH)),"ordinary offhand supplies remain allowed");helper.succeed();
    }

    @GameTest(template="empty") public static void stash_and_every_case_allow_extraction_and_equipping(GameTestHelper helper) {
        for(var block:new com.minecrafteft.block.WeaponCaseBlock[]{ModContent.WEAPON_CASE.get(),ModContent.SMALL_WEAPON_CASE.get(),
                ModContent.AR_TEAM_CASE.get(),ModContent.ELMO_CASE.get(),ModContent.HIDE404_CASE.get()}) {
            var player=player(helper);var record=new StashSavedData().createRecord(player.getUUID(),StashTier.FOUR);
            var host=new ItemStack(block.asItem());record.items().setStackInSlot(0,host);
            var menu=new StashMenu(45,player.getInventory(),record);menu.clickMenuButton(player,StashMenu.OPEN_CASE_BUTTON);
            var rifle=gun("m4a1");rifle.getOrCreateTag().putString("RoundTrip","preserve-gun-and-attachments");
            menu.setCarried(rifle.copy());menu.clicked(menu.caseSlot(0).index,0,ClickType.PICKUP,player);
            check(menu.getCarried().isEmpty(),"case insertion consumes cursor once");
            menu.clicked(menu.caseSlot(0).index,0,ClickType.PICKUP,player);
            check(ItemStack.matches(menu.getCarried(),rifle),"hidden popup slots allow real extraction for "+block.kind());
            var primary=menu.loadout.hotbar.get(1);check(primary.index>8 && primary.mayPlace(rifle),"menu-global index does not change primary filter");
            menu.clicked(primary.index,0,ClickType.PICKUP,player);
            check(menu.getCarried().isEmpty() && ItemStack.matches(primary.getItem(),rifle),"case gun equips directly");
            menu.clickMenuButton(player,StashMenu.CLOSE_CASE_BUTTON);
            menu.clicked(primary.index,0,ClickType.PICKUP,player);menu.clicked(50,0,ClickType.PICKUP,player);
            check(menu.getCarried().isEmpty() && primary.getItem().isEmpty(),"equipped gun stores in stash");
            menu.clicked(50,0,ClickType.PICKUP,player);menu.clicked(primary.index,0,ClickType.PICKUP,player);
            check(menu.getCarried().isEmpty() && ItemStack.matches(primary.getItem(),rifle),"stash roundtrip reequips same gun NBT");
            check(menu.visibleRows()==13,"right stash uses thirteen visible rows");menu.removed(player);

            player.getInventory().setItem(1,ItemStack.EMPTY);
            var entity=new com.minecrafteft.block.WeaponCaseBlockEntity(net.minecraft.core.BlockPos.ZERO,block.defaultBlockState());
            entity.inventory().setStackInSlot(0,rifle.copy());var physical=new StashMenu(46,player.getInventory(),entity);
            physical.clicked(0,0,ClickType.PICKUP,player);physical.clicked(physical.loadout.hotbar.get(1).index,0,ClickType.PICKUP,player);
            check(physical.getCarried().isEmpty() && ItemStack.matches(player.getInventory().getItem(1),rifle),"placed case gun equips for "+block.kind());
            check(entity.inventory().getStackInSlot(0).isEmpty(),"placed case contains no duplicate");physical.removed(player);
        }
        helper.succeed();
    }

    @GameTest(template="empty") public static void day_pack_stash_wear_and_reopen(GameTestHelper helper) {
        var player = player(helper);
        var record = new StashSavedData().createRecord(player.getUUID(), StashTier.FOUR);
        var bag = new ItemStack(ModContent.DAY_PACK.get());
        var rifle = gun("m4a1");
        rifle.getOrCreateTag().putString("Serial","pack-roundtrip");
        record.items().setStackInSlot(0,bag);
        var menu = new StashMenu(48,player.getInventory(),record);
        check(StashMenu.footprintWidth(bag)==4 && StashMenu.footprintHeight(bag)==5,"pack occupies 4x5 in stash");
        check(!menu.canPlaceAt(43,new ItemStack(Items.APPLE)),"last covered stash cell is reserved");
        check(menu.clickMenuButton(player,StashMenu.OPEN_CASE_BUTTON),"stash pack opens with existing container action");
        check(menu.isBackpackOpen() && menu.openColumns()==4 && menu.openCaseSlots()==20,"popup is 4x5");
        check(!menu.slots.get(0).mayPickup(player),"open pack is locked");
        check(!menu.canPlaceInCase(0,bag),"cannot place backpack in itself");
        menu.setCarried(rifle.copy());
        menu.clicked(menu.caseSlot(0).index,0,ClickType.PICKUP,player);
        check(menu.getCarried().isEmpty() && !menu.canPlaceInCase(5,new ItemStack(Items.APPLE)),"rifle uses its stash footprint");
        var supplies=menu.caseSlot(19);
        menu.setCarried(new ItemStack(Items.APPLE,12));menu.clicked(supplies.index,0,ClickType.PICKUP,player);
        menu.setCarried(new ItemStack(Items.APPLE,5));menu.clicked(supplies.index,0,ClickType.PICKUP,player);
        check(supplies.getItem().getCount()==17 && menu.getCarried().isEmpty(),"popup supports ordinary stack merging");
        menu.clickMenuButton(player,StashMenu.CLOSE_CASE_BUTTON);
        menu.clicked(0,0,ClickType.PICKUP,player);menu.clicked(menu.loadout.bag.index,0,ClickType.PICKUP,player);
        var state=LoadoutState.of(player);
        check(state.backpackVisible() && menu.loadout.backpack.get(19).getItem().getCount()==17,"equipping preserves contents");
        check(ItemStack.matches(state.backpack.getStackInSlot(0),rifle),"gun data preserved between views");
        menu.clickMenuButton(player,2);
        check(!state.backpackVisible() && state.backpack.size().cells()==20,"hiding exterior retains storage");
        menu.clicked(menu.loadout.bag.index,0,ClickType.PICKUP,player);
        menu.clickMenuButton(player,StashMenu.ROTATE_CARRIED_BUTTON);
        menu.clicked(10,0,ClickType.PICKUP,player);
        check(menu.clickMenuButton(player,StashMenu.OPEN_CASE_BUTTON+10) && menu.openColumns()==4,"rotated shell keeps upright inner grid");
        menu.clicked(menu.caseSlot(0).index,0,ClickType.PICKUP,player);
        check(ItemStack.matches(menu.getCarried(),rifle),"original weapon comes out of the moved pack");
        menu.removed(player);helper.succeed();
    }

    @GameTest(template="empty") public static void backpack_activity_uses_cell_index_not_menu_index(GameTestHelper helper) {
        var player=player(helper);LoadoutState.of(player).equipment.setStackInSlot(LoadoutState.BACKPACK,bag());
        var record=new StashSavedData().createRecord(player.getUUID(),StashTier.FOUR);var menu=new StashMenu(47,player.getInventory(),record);
        var cell=menu.loadout.backpack.get(19);check(cell.index>19 && cell.isActive(),"last valid backpack cell remains clickable after menu registration");
        check(!menu.loadout.backpack.get(20).isActive(),"first out-of-bounds cell stays hidden");
        menu.setCarried(new ItemStack(Items.APPLE,3));menu.clicked(cell.index,0,ClickType.PICKUP,player);
        check(menu.getCarried().isEmpty() && cell.getItem().getCount()==3,"valid backpack click persists items");helper.succeed();
    }

    @GameTest(template="empty") public static void invalidated_player_can_clone_and_resume_loadout(GameTestHelper helper) {
        var original=player(helper);var state=LoadoutState.of(original);state.creativeCustom=true;state.hideBackpack=true;
        state.equipment.setStackInSlot(LoadoutState.BACKPACK,new ItemStack(ModContent.DAY_PACK.get()));
        original.setGameMode(GameType.CREATIVE);original.invalidateCaps();
        check(!LoadoutState.custom(original),"invalid death-screen capability cannot crash custom HUD eligibility");
        original.reviveCaps();
        check(LoadoutState.of(original)==state,"Clone revives a fresh handle to the original loadout state");
        original.getCapability(com.minecrafteft.vitals.VitalProvider.CAPABILITY).orElseThrow(AssertionError::new);
        original.invalidateCaps();var next=player(helper);
        LoadoutEvents.clonePlayer(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(next,original,false));
        var restored=LoadoutState.of(next);
        check(restored.creativeCustom && restored.hideBackpack && restored.backpackVisible()==false,"clone preserves equipment visibility and selected inventory mode");
        check(restored.equipment.getStackInSlot(LoadoutState.BACKPACK).is(ModContent.DAY_PACK.get()),"non-death clone preserves backpack");
        original.reviveCaps();var afterDeath=player(helper);
        LoadoutEvents.clonePlayer(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(afterDeath,original,true));
        check(LoadoutState.of(afterDeath).creativeCustom,"death clone completes and retains inventory preference");helper.succeed();
    }

    @GameTest(template="empty") public static void held_backpack_locks_host_and_preserves_nested_contents(GameTestHelper helper) {
        for(int source:new int[]{3,40}) {
            var player=player(helper);var host=new ItemStack(ModContent.DAY_PACK.get());player.getInventory().setItem(source,host);
            var menu=new PortableBackpackMenu(51,player.getInventory(),source);
            var hostSlot=source==40?menu.loadout.offhand:menu.loadout.hotbar.get(source);
            check(menu.stillValid(player) && !hostSlot.mayPickup(player),"held bag stays attached to its real hand slot");
            var rifle=gun("m4a1");rifle.getOrCreateTag().putString("Serial","held-bag-transfer");
            menu.setCarried(rifle.copy());menu.clicked(menu.bagSlots.get(0).index,0,ClickType.PICKUP,player);
            check(menu.getCarried().isEmpty() && menu.contents.anchorAt(7)==0,"held bag reserves gun's full 4x2 rectangle");
            menu.clicked(menu.bagSlots.get(0).index,source,ClickType.SWAP,player);
            check(player.getInventory().getItem(source)==host && menu.contents.getStackInSlot(0).getCount()==1,"number/F swap cannot dislodge an opened host");
            check(!menu.bagSlots.get(8).mayPlace(host),"bag cannot contain itself or nested storage");
            menu.setCarried(new ItemStack(Items.APPLE,8));menu.clicked(menu.bagSlots.get(19).index,0,ClickType.PICKUP,player);
            check(menu.getCarried().isEmpty(),"ordinary supplies fit alongside reserved gun cells");
            menu.clicked(menu.bagSlots.get(0).index,0,ClickType.PICKUP,player);menu.clicked(menu.loadout.hotbar.get(1).index,0,ClickType.PICKUP,player);
            check(ItemStack.matches(player.getInventory().getItem(1),rifle),"held-bag gun equips with complete NBT");
            menu.removed(player);var saved=ItemStack.of(host.save(new CompoundTag()));
            var reopened=new EquipmentGrid(()->saved,()->{});
            check(reopened.getStackInSlot(0).isEmpty() && reopened.getStackInSlot(19).getCount()==8,"closing and serializing neither restores extracted gun nor loses supplies");
            player.getInventory().setItem(source,ItemStack.EMPTY);check(!menu.stillValid(player),"externally replaced host invalidates held bag menu");
        }
        helper.succeed();
    }

    @GameTest(template="empty") public static void creative_transition_is_atomic_and_assigns_misplaced_weapons(GameTestHelper helper) {
        var player=player(helper);player.setGameMode(GameType.CREATIVE);var state=LoadoutState.of(player);
        for(int i=3;i<6;i++) player.getInventory().setItem(i,gun("m4a1"));
        player.getInventory().setItem(0,new ItemStack(Items.APPLE,12));
        var before=player.getInventory().save(new net.minecraft.nbt.ListTag()).copy();var capability=state.save().copy();
        check(!CreativeLoadoutTransition.apply(player),"three primaries reject mode change");
        check(before.equals(player.getInventory().save(new net.minecraft.nbt.ListTag())) && capability.equals(state.save()),"rejected transition changes neither inventory nor capability");
        check(!state.creativeCustom,"failure preserves vanilla creative mode");
        player.getInventory().setItem(5,gun("glock_17"));player.getInventory().setItem(40,new ItemStack(Items.IRON_AXE));
        check(CreativeLoadoutTransition.apply(player),"valid misplaced equipment passes preflight");
        check("pistol".equals(StashMenu.gunCategory(player.getInventory().getItem(0))),"pistol automatically assigned to key 1");
        check(StashMenu.isTaczGun(player.getInventory().getItem(1)) && StashMenu.isTaczGun(player.getInventory().getItem(2)),"both primaries automatically assigned to keys 2 and 3");
        check(state.equipment.getStackInSlot(LoadoutState.MELEE).is(Items.IRON_AXE) && player.getOffhandItem().isEmpty(),"offhand tool moves to U");
        int apples=0;for(int i=0;i<36;i++) if(player.getInventory().getItem(i).is(Items.APPLE)) apples+=player.getInventory().getItem(i).getCount();
        check(apples==12,"supplies displaced by automatic equipment survive exactly once");helper.succeed();
    }
}
