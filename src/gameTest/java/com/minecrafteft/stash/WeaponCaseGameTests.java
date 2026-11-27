package com.minecrafteft.stash;

import com.minecrafteft.block.*;
import com.minecrafteft.registry.ModContent;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder("minecrafteft")
@PrefixGameTestTemplate(false)
public final class WeaponCaseGameTests {
    private static ItemStack gun() {
        ItemStack stack = GunItemBuilder.create().setId(ResourceLocation.parse("tacz:glock_17")).build();
        stack.getOrCreateTag().putString("CaseTest", "attachments-and-appearance-survive");
        return stack;
    }
    private static void check(boolean ok, String reason) { if (!ok) throw new AssertionError(reason); }

    @GameTest(template = "empty")
    public static void ver12_display_clears_and_selects_next_gun(GameTestHelper helper) {
        var entity=new WeaponCaseBlockEntity(BlockPos.ZERO,ModContent.WEAPON_CASE.get().defaultBlockState());
        entity.inventory().setStackInSlot(0,gun());
        entity.inventory().setStackInSlot(30,gun());
        var client=new WeaponCaseBlockEntity(BlockPos.ZERO,ModContent.WEAPON_CASE.get().defaultBlockState());
        client.handleUpdateTag(entity.getUpdateTag());
        entity.inventory().extractItem(0,1,false);
        check(!entity.inventory().firstGun().isEmpty(),"next occupied anchor becomes display");
        entity.inventory().extractItem(30,1,false);
        check(entity.getUpdateTag().contains("DisplayGun"),"empty display explicitly synchronized");
        client.handleUpdateTag(entity.getUpdateTag());
        try {
            var field=WeaponCaseBlockEntity.class.getDeclaredField("clientDisplayGun");
            field.setAccessible(true);
            check(((ItemStack)field.get(client)).isEmpty(),"client clears previous gun after last extraction");
            client.load(new CompoundTag());
            client.load(entity.getUpdateTag());
            check(((ItemStack)field.get(client)).isEmpty(),"chunk update also clears display");
        } catch(ReflectiveOperationException error) { throw new AssertionError(error); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ver12_direct_stack_mutation_persists(GameTestHelper helper) {
        FakePlayer player=new FakePlayer(helper.getLevel(),new GameProfile(UUID.randomUUID(),"MutationTest"));
        var data=new StashSavedData();
        var record=data.createRecord(player.getUUID(),StashTier.FOUR);
        var host=new ItemStack(ModContent.WEAPON_CASE_ITEM.get());
        record.items().setStackInSlot(0,host);
        StashMenu menu=new StashMenu(21,player.getInventory(),record);
        menu.clickMenuButton(player,StashMenu.OPEN_CASE_BUTTON);
        menu.setCarried(gun());
        menu.clicked(menu.caseSlot(0).index,0,ClickType.PICKUP,player);
        check(!WeaponCaseInventory.firstGunInItem(host).isEmpty(),"host initially contains gun");
        // Vanilla quick transfer shrinks the live stack, which need not call setStackInSlot.
        menu.caseStack(0).shrink(1);
        menu.broadcastChanges();
        check(WeaponCaseInventory.firstGunInItem(host).isEmpty(),"broadcast persists direct extraction");
        menu.removed(player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ver12_deck_and_seating(GameTestHelper helper) {
        var cells=ContainerStructureLayout.restAreaCells();
        check(cells.stream().filter(c->c.y()==0).count()==27,"rear deck covers full 9x3 area");
        check(cells.stream().allMatch(c->c.x()<=8 && c.z()<=7),"compact existing footprint retained");
        for(int x=0;x<9;x++) for(int z=5;z<8;z++)
            check(ContainerStructureLayout.decorationAt(x,0,z).deck(),"no holes in rear deck");
        var level=helper.getLevel();
        var pos=helper.absolutePos(new BlockPos(1,1,1));
        var state=ModContent.STASH_PART.get().defaultBlockState().setValue(ContainerPartBlock.X,6)
                .setValue(ContainerPartBlock.Y,0).setValue(ContainerPartBlock.Z,7);
        level.setBlock(pos,state,3);
        FakePlayer player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"SeatTest"));
        StashSeatEntity.sit(level,pos,player);
        check(player.getVehicle() instanceof StashSeatEntity,"bench creates sitting mount");
        var seat=player.getVehicle();
        seat.positionRider(player);
        check(Math.abs(player.getY()+.55-(pos.getY()+.12+.54))<.00001,
                "seated hips touch the deck bench seat instead of hovering");
        FakePlayer other=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"OccupiedTest"));
        StashSeatEntity.sit(level,pos,other);
        check(!other.isPassenger(),"occupied seat rejects second player");
        player.stopRiding();seat.tick();
        check(seat.isRemoved(),"seat removed on dismount");
        level.removeBlock(pos,false);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void inventory_and_portable_roundtrip(GameTestHelper helper) {
        int[] writes = {0};
        WeaponCaseInventory inventory = new WeaponCaseInventory(() -> writes[0]++);
        ItemStack gun = gun();
        check(inventory.insertItem(0, gun.copy(), false).isEmpty(), "gun accepted");
        check(!inventory.insertItem(1, gun.copy(), false).isEmpty(), "reserved cell rejected");
        check(!inventory.insertItem(9, gun.copy(), false).isEmpty(), "right overflow rejected");
        check(!inventory.insertItem(30, new ItemStack(Items.DIAMOND), false).isEmpty(), "non-gun rejected");
        check(!inventory.insertItem(30, new ItemStack(ModContent.WEAPON_CASE_ITEM.get()), false).isEmpty(), "nested case rejected");
        ItemStack portable = new ItemStack(ModContent.WEAPON_CASE_ITEM.get());
        portable.getOrCreateTag().put(WeaponCaseInventory.CONTENTS, inventory.write());
        portable = ItemStack.of(portable.save(new CompoundTag()));
        WeaponCaseBlockEntity entity = new WeaponCaseBlockEntity(BlockPos.ZERO, ModContent.WEAPON_CASE.get().defaultBlockState());
        entity.loadItem(portable);
        CompoundTag saved = entity.saveWithFullMetadata();
        WeaponCaseBlockEntity restored = new WeaponCaseBlockEntity(BlockPos.ZERO, ModContent.WEAPON_CASE.get().defaultBlockState());
        restored.load(saved);
        WeaponCaseInventory result = new WeaponCaseInventory(() -> {});
        result.readItem(ItemStack.of(restored.asItem().save(new CompoundTag())));
        check(ItemStack.isSameItemSameTags(gun, result.getStackInSlot(0)), "full gun NBT roundtrip");
        check(writes[0] == 1, "invalid insert never changes storage");
        check(StashMenu.footprintWidth(portable) == 5 && StashMenu.footprintHeight(portable) == 3, "5x3 case footprint");
        check(StashTier.ONE.slots() == 200 && StashTier.TWO.slots() == 400
                && StashTier.THREE.slots() == 600 && StashTier.FOUR.slots() == 800, "doubled capacities");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void direct_case_insertion_rotates_and_rejects_fragmented_space(GameTestHelper helper) {
        ItemStack rifle=GunItemBuilder.create().setId(ResourceLocation.parse("tacz:m4a1"))
                .putAttachment(com.tacz.guns.api.item.attachment.AttachmentType.EXTENDED_MAG,
                        ResourceLocation.parse("tacz:extended_mag_1")).build();
        rifle.getOrCreateTag().putString("TransferTest","preserve-parts");
        check(StashMenu.footprintWidth(rifle)==4 && StashMenu.footprintHeight(rifle)==3,"attached rifle is 4x3");
        ItemStack original=rifle.copy();
        ItemStack blocker=GunItemBuilder.create().setId(ResourceLocation.parse("tacz:taurus943")).build();
        for(int rows:new int[]{7,15}) {
            var inventory=new WeaponCaseInventory(rows,()->{});
            for(int i=0;i<inventory.getSlots();i++) {
                int x=i%10,y=i/10;
                if(x>=2 && x<5 && y>=1 && y<5) continue; // The only hole is 3x4.
                inventory.setStackInSlot(i,blocker.copy());
            }
            var previewBefore=inventory.write();
            check(inventory.canFit(rifle),"blue preview accepts rotation-only free rectangle");
            check(previewBefore.equals(inventory.write()),"preview does not insert or modify contents");
            check(inventory.insertFirstFit(rifle).isEmpty(),"auto rotate into 3x4 in both case sizes");
            ItemStack placed=inventory.getStackInSlot(12);
            check(StashMenu.isRotated(placed),"stored orientation is rotated");
            check(ItemStack.isSameItemSameTags(StashMenu.rotatedCopy(placed),original),"attachment and custom NBT preserved");
            check(ItemStack.isSameItemSameTags(rifle,original),"input is not mutated");
            var fragmented=new WeaponCaseInventory(rows,()->{});
            for(int i=0;i<fragmented.getSlots();i++) if((i%10+i/10)%2==0)
                fragmented.setStackInSlot(i,blocker.copy());
            var before=fragmented.write();
            check(!fragmented.canFit(rifle),"preview rejects fragmented free cells");
            check(fragmented.insertFirstFit(rifle)==rifle,"scattered free cells cannot fit a rifle");
            check(before.equals(fragmented.write()),"failed transfer leaves contents unchanged");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void click_carried_gun_into_closed_and_open_case(GameTestHelper helper) {
        FakePlayer player=new FakePlayer(helper.getLevel(),new GameProfile(UUID.randomUUID(),"DirectCaseTest"));
        for(var kind:WeaponCaseKind.values()) {
            var record=new StashSavedData().createRecord(player.getUUID(),StashTier.FOUR);
            var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                    ResourceLocation.fromNamespaceAndPath("minecrafteft",kind.id()));
            var host=new ItemStack(item);
            record.items().setStackInSlot(0,host);
            var menu=new StashMenu(31,player.getInventory(),record);
            ItemStack first=gun();
            check(menu.canStoreInCase(0,first),"closed case insertion preview fits");
            menu.setCarried(first);
            menu.clicked(0,0,ClickType.PICKUP,player);
            check(menu.getCarried().isEmpty(),"closed case consumes carried gun");
            check(ItemStack.isSameItemSameTags(first,WeaponCaseInventory.firstGunInItem(host)),"closed host NBT persists gun");
            menu.clickMenuButton(player,StashMenu.OPEN_CASE_BUTTON);
            check(menu.canStoreInCase(0,gun()),"open case insertion preview fits");
            menu.setCarried(gun());
            menu.clicked(0,0,ClickType.PICKUP,player);
            check(menu.getCarried().isEmpty() && !menu.caseStack(2).isEmpty(),"open popup uses same inventory");
            menu.broadcastChanges();
            var saved=new WeaponCaseInventory(kind.rows(),()->{});
            saved.readItem(host);
            check(!saved.getStackInSlot(2).isEmpty(),"later popup persistence retains insertion");
            // Fill every remaining cell; a failed click must never swap out the case.
            ItemStack blocker=GunItemBuilder.create().setId(ResourceLocation.parse("tacz:taurus943")).build();
            for(int i=0;i<menu.openCaseSlots();i++)
                if(menu.openInventory().canPlace(i,blocker) && menu.caseStack(i).isEmpty())
                    menu.openInventory().setStackInSlot(i,blocker.copy());
            var before=host.copy();
            ItemStack held=gun();
            check(!menu.canStoreInCase(0,held),"full case insertion preview rejects gun");
            menu.setCarried(held);
            menu.clicked(0,0,ClickType.PICKUP,player);
            check(menu.getCarried()==held && ItemStack.isSameItemSameTags(host,before),"full case keeps gun on cursor and host in place");
            menu.setCarried(ItemStack.EMPTY);
            menu.removed(player);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void popup_moves_lock_and_save(GameTestHelper helper) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "CaseTest"));
        StashSavedData data = new StashSavedData();
        var record = data.createRecord(player.getUUID(), StashTier.FOUR);
        ItemStack host = new ItemStack(ModContent.WEAPON_CASE_ITEM.get());
        record.items().setStackInSlot(0, host);
        record.items().setStackInSlot(40, gun());
        StashMenu menu = new StashMenu(7, player.getInventory(), record);
        check(menu.clickMenuButton(player, StashMenu.OPEN_CASE_BUTTON), "case opens");
        check(!menu.slots.get(0).mayPickup(player), "open case host locked");
        menu.clicked(40, 0, ClickType.PICKUP, player);
        check(StashMenu.isTaczGun(menu.getCarried()), "stash pickup");
        menu.clicked(menu.caseSlot(0).index, 0, ClickType.PICKUP, player);
        check(menu.getCarried().isEmpty() && StashMenu.isTaczGun(menu.caseStack(0)), "case insertion");
        menu.clickMenuButton(player, StashMenu.CLOSE_CASE_BUTTON);
        check(menu.slots.get(0).mayPickup(player), "host unlocks");
        WeaponCaseInventory saved = new WeaponCaseInventory(() -> {});
        saved.readItem(record.items().getStackInSlot(0));
        check(StashMenu.isTaczGun(saved.getStackInSlot(0)), "popup changes persisted in case item");
        menu.clickMenuButton(player, StashMenu.OPEN_CASE_BUTTON);
        ItemStack diamond = new ItemStack(Items.DIAMOND);
        menu.setCarried(diamond);
        menu.clicked(menu.caseSlot(30).index, 0, ClickType.PICKUP, player);
        check(menu.getCarried() == diamond && menu.caseStack(30).isEmpty(), "popup refuses non-gun");
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(menu.caseSlot(0).index, 0, ClickType.PICKUP, player);
        menu.clicked(40, 0, ClickType.PICKUP, player);
        check(menu.getCarried().isEmpty() && StashMenu.isTaczGun(record.items().getStackInSlot(40)), "case to stash");
        saved.readItem(record.items().getStackInSlot(0));
        check(saved.getStackInSlot(0).isEmpty(), "extracted gun removed from serialized case");
        menu.setScrollRow(30);
        check(StashMenu.isTaczGun(record.items().getStackInSlot(40)), "server scrolling preserves storage");
        check(record.items().getStackInSlot(0) == host, "scrolling preserves open host");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void world_case_rejects_other_items_and_tracks_lid(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(pos, ModContent.WEAPON_CASE.get().defaultBlockState(), 3);
        WeaponCaseBlockEntity entity = (WeaponCaseBlockEntity) level.getBlockEntity(pos);
        FakePlayer player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "WorldCaseTest"));
        player.setPos(Vec3.atCenterOf(pos));
        StashMenu menu = new StashMenu(8, player.getInventory(), entity);
        player.containerMenu = menu;
        entity.startViewing(player.getUUID());
        check(level.getBlockState(pos).getValue(WeaponCaseBlock.OPEN), "world lid opens");
        check(menu.isCaseOnly() && menu.rows() == 15, "10x15 world menu");
        check(!menu.canPlaceAt(0, new ItemStack(Items.DIAMOND)), "world menu refuses non-gun");
        menu.setCarried(gun());
        menu.clicked(0,0,ClickType.PICKUP,player);
        check(menu.getCarried().isEmpty() && StashMenu.isTaczGun(entity.inventory().getStackInSlot(0)), "world gun insertion");
        menu.setScrollRow(5);
        check(StashMenu.isTaczGun(entity.inventory().getStackInSlot(0)), "world scroll never clears guns");
        check(menu.stillValid(player), "nearby world menu valid");
        player.setPos(pos.getX()+20, pos.getY(), pos.getZ());
        check(!menu.stillValid(player), "distant world menu invalid");
        menu.removed(player);
        check(!level.getBlockState(pos).getValue(WeaponCaseBlock.OPEN), "world lid closes");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void range_and_stash_save_restore(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        FakePlayer player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "RangeTest"));
        StashSavedData data = new StashSavedData();
        var record = data.createRecord(player.getUUID(), StashTier.FOUR);
        BlockPos root = helper.absolutePos(new BlockPos(1,1,1));
        data.bind(record, level, root, StashTier.FOUR);
        Vec3 center = Vec3.atCenterOf(root);
        player.setPos(center.add(50,0,0));
        check(StashService.withinRange(player,record), "50-block boundary included");
        player.setPos(center.add(50.01,0,0));
        check(!StashService.withinRange(player,record), "beyond 50 blocks rejected");
        player.setPos(center.add(30,40,0));
        check(StashService.withinRange(player,record), "3D distance at boundary included");
        WeaponCaseInventory contents = new WeaponCaseInventory(() -> {});
        contents.insertItem(0,gun(),false);
        ItemStack host = new ItemStack(ModContent.WEAPON_CASE_ITEM.get());
        host.getOrCreateTag().put(WeaponCaseInventory.CONTENTS,contents.write());
        record.items().setStackInSlot(770,host);
        record.items().setStackInSlot(798,gun());
        CompoundTag serialized = data.save(new CompoundTag());
        var load = StashSavedData.class.getDeclaredMethod("load",CompoundTag.class);
        load.setAccessible(true);
        StashSavedData restored = (StashSavedData) load.invoke(null,serialized);
        var restoredRecord = restored.getRecord(player.getUUID());
        check(restoredRecord.items().getSlots() == 800, "800 backing slots restored");
        check(ItemStack.isSameItemSameTags(host,restoredRecord.items().getStackInSlot(770)), "nested case saved with contents");
        check(StashMenu.isTaczGun(restoredRecord.items().getStackInSlot(798)), "high slot index not truncated to byte");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void world_placement_break_preserves_contents(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos root = helper.absolutePos(new BlockPos(1,1,1));
        var block = ModContent.WEAPON_CASE.get();
        var state = block.defaultBlockState();
        WeaponCaseInventory contents = new WeaponCaseInventory(() -> {});
        contents.insertItem(0,gun(),false);
        ItemStack host = new ItemStack(ModContent.WEAPON_CASE_ITEM.get());
        host.getOrCreateTag().put(WeaponCaseInventory.CONTENTS,contents.write());
        level.setBlock(root,state,3);
        block.setPlacedBy(level,root,state,null,host);
        for(int x=0;x<3;x++) for(int y=0;y<2;y++) for(int z=0;z<2;z++) {
            if(x+y+z==0) continue;
            BlockPos pos=WeaponCaseBlock.at(root,Direction.NORTH,x,y,z);
            check(level.getBlockState(pos).is(ModContent.WEAPON_CASE_PART.get()), "all linked cells placed");
            check(WeaponCasePartBlock.anchor(pos,level.getBlockState(pos)).equals(root), "part maps to root");
        }
        var drops=net.minecraft.world.level.block.Block.getDrops(state,level,root,level.getBlockEntity(root));
        check(drops.size()==1 && ItemStack.isSameItemSameTags(host,drops.get(0)), "single drop preserves gun NBT");
        level.destroyBlock(root,false);
        for(int x=0;x<3;x++) for(int y=0;y<2;y++) for(int z=0;z<2;z++)
            check(level.getBlockState(WeaponCaseBlock.at(root,Direction.NORTH,x,y,z)).isAir(), "linked cells removed");
        level.setBlock(root,state,3);
        block.setPlacedBy(level,root,state,null,host);
        level.destroyBlock(WeaponCaseBlock.at(root,Direction.NORTH,2,1,1),true);
        check(level.getBlockState(root).isAir(), "breaking linked part removes root");
        var dropped = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(root).inflate(1));
        check(dropped.size()==1 && ItemStack.isSameItemSameTags(host,dropped.get(0).getItem()),
                "breaking a part drops exactly one filled case");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void centered_case_parts_and_legacy_layout(GameTestHelper helper) {
        var level=helper.getLevel();
        var block=ModContent.WEAPON_CASE.get();
        BlockPos root=helper.absolutePos(new BlockPos(3,1,3));
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            var state=block.defaultBlockState().setValue(WeaponCaseBlock.FACING,facing).setValue(WeaponCaseBlock.CENTERED,true);
            level.setBlock(root,state,3);
            block.setPlacedBy(level,root,state,null,new ItemStack(ModContent.WEAPON_CASE_ITEM.get()));
            check(WeaponCaseBlock.at(root,state,1,0,0).equals(root), "center cell is clicked block: " + facing);
            net.minecraft.world.phys.AABB bounds=null;
            for(int x=0;x<3;x++) for(int y=0;y<2;y++) for(int z=0;z<2;z++) {
                BlockPos pos=WeaponCaseBlock.at(root,state,x,y,z);
                var cell=level.getBlockState(pos);
                if(!pos.equals(root)) {
                    check(cell.is(ModContent.WEAPON_CASE_PART.get()), "centered linked cell exists");
                    check(WeaponCasePartBlock.anchor(pos,cell).equals(root), "rotated centered part maps to root");
                }
                var shape=cell.getShape(level,pos).bounds().move(pos);
                bounds=bounds==null?shape:bounds.minmax(shape);
            }
            double lateralCenter=(facing.getAxis()==Direction.Axis.Z)?(bounds.minX+bounds.maxX)/2:(bounds.minZ+bounds.maxZ)/2;
            check(Math.abs(lateralCenter-(facing.getAxis()==Direction.Axis.Z?root.getX():root.getZ())-.5)<.00001,
                    "case shape centered on clicked block in " + facing);
            level.destroyBlock(WeaponCaseBlock.at(root,state,0,1,1),false);
            check(level.getBlockState(root).isAir(), "centered part destruction removes root");
            for(int x=0;x<3;x++) for(int y=0;y<2;y++) for(int z=0;z<2;z++)
                check(level.getBlockState(WeaponCaseBlock.at(root,state,x,y,z)).isAir(), "all centered parts removed");
        }
        check(!block.defaultBlockState().getValue(WeaponCaseBlock.CENTERED), "legacy default retains corner-root layout");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void scrollbar_drag_boundaries(GameTestHelper helper) {
        for (int total : new int[]{7,15,20,40,60,80}) for(int visible : new int[]{6,8,10}) {
            if(visible >= total) continue;
            for(int row=0;row<=total-visible;row++) {
                var bar=new com.minecrafteft.client.StashScrollbar(100,30,4,visible*18,total,visible,row);
                double grab=bar.thumbHeight()/2.0;
                check(bar.contains(102,bar.thumbY()+grab), "thumb can be clicked");
                check(bar.rowAt(bar.thumbY()+grab,grab)==row, "grabbing thumb preserves row");
                check(bar.rowAt(-1000,grab)==0, "drag clamps above track");
                check(bar.rowAt(1000,grab)==bar.maxRow(), "drag clamps below track");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void small_case_variants_preserve_guns_and_limit_capacity(GameTestHelper helper) {
        for(var block:new WeaponCaseBlock[]{ModContent.SMALL_WEAPON_CASE.get(),ModContent.AR_TEAM_CASE.get(),
                ModContent.ELMO_CASE.get(),ModContent.HIDE404_CASE.get()}) {
            ItemStack host=new ItemStack(block);
            check(StashMenu.footprintWidth(host)==5 && StashMenu.footprintHeight(host)==2,"small case 5x2 footprint");
            FakePlayer player=new FakePlayer(helper.getLevel(),new GameProfile(UUID.randomUUID(),"SmallCase"));
            var data=new StashSavedData();
            var record=data.createRecord(player.getUUID(),StashTier.FOUR);
            record.items().setStackInSlot(0,host);
            StashMenu menu=new StashMenu(11,player.getInventory(),record);
            check(menu.clickMenuButton(player,StashMenu.OPEN_CASE_BUTTON),"small case opens");
            check(menu.openCaseRows()==7 && menu.openCaseSlots()==70,"small popup capacity");
            check(menu.openCaseKind()==block.kind(),"variant identity in popup");
            menu.setCarried(gun());
            menu.clicked(menu.caseSlot(70).index,0,ClickType.PICKUP,player);
            check(!menu.getCarried().isEmpty(),"slot beyond row 7 rejected");
            menu.clicked(menu.caseSlot(68).index,0,ClickType.PICKUP,player);
            check(menu.getCarried().isEmpty() && StashMenu.isTaczGun(menu.caseStack(68)),"last row accepts gun");
            check(!menu.slots.get(0).mayPickup(player),"small host locked while open");
            menu.clickMenuButton(player,StashMenu.CLOSE_CASE_BUTTON);
            check(menu.slots.get(0).mayPickup(player),"small host unlocked on close");
            var entity=new WeaponCaseBlockEntity(BlockPos.ZERO,block.defaultBlockState());
            entity.loadItem(ItemStack.of(record.items().getStackInSlot(0).save(new CompoundTag())));
            var restored=new WeaponCaseBlockEntity(BlockPos.ZERO,block.defaultBlockState());
            restored.load(entity.saveWithFullMetadata());
            check(restored.inventory().getSlots()==70,"small block storage size after reload");
            check(ItemStack.isSameItemSameTags(host,restored.asItem()),"variant and full gun NBT preserved through item/block reload");
            check(StashMenu.isTaczGun(restored.inventory().getStackInSlot(68)),"last-row gun preserved");
            StashMenu worldMenu=new StashMenu(12,player.getInventory(),restored);
            check(worldMenu.visibleSlots()==70 && worldMenu.rows()==7,"standalone small case menu size");
            check(!worldMenu.canPlaceAt(0,new ItemStack(Items.DIAMOND)),"small world menu rejects non-gun");
            check(!worldMenu.canPlaceAt(0,host),"case nesting rejected");
            // Switch the same popup slot array from a large case to a small one.
            record.items().setStackInSlot(30,new ItemStack(ModContent.WEAPON_CASE_ITEM.get()));
            menu.clickMenuButton(player,StashMenu.OPEN_CASE_BUTTON+30);
            check(menu.openCaseSlots()==150,"large popup still has 150 slots");
            menu.clickMenuButton(player,StashMenu.OPEN_CASE_BUTTON);
            check(menu.openCaseSlots()==70 && menu.caseStack(149).isEmpty(),"unused popup tail safely empty after switch");
            check(menu.slots.get(30).mayPickup(player),"closed case stays movable while another case is open");
            check(!menu.isOpenCaseStorageIndex(30) && menu.isOpenCaseStorageIndex(0),"only the active case uses its open preview");
            menu.clicked(30,0,ClickType.PICKUP,player);
            check(StashMenu.isWeaponCase(menu.getCarried()) && menu.isOpenCaseStorageIndex(0),
                    "closed case pickup keeps the other popup open");
            menu.clicked(40,0,ClickType.PICKUP,player);
            check(menu.getCarried().isEmpty() && StashMenu.isWeaponCase(record.items().getStackInSlot(40)),
                    "closed case can be placed elsewhere while popup remains open");
            check(!menu.slots.get(0).mayPickup(player),"only the open host still requires closing");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void small_case_centered_shape_and_drops(GameTestHelper helper) {
        var level=helper.getLevel();
        BlockPos root=helper.absolutePos(new BlockPos(3,1,3));
        for(var block:new WeaponCaseBlock[]{ModContent.SMALL_WEAPON_CASE.get(),ModContent.AR_TEAM_CASE.get(),
                ModContent.ELMO_CASE.get(),ModContent.HIDE404_CASE.get()}) for(Direction facing:Direction.Plane.HORIZONTAL) {
            var state=block.defaultBlockState().setValue(WeaponCaseBlock.CENTERED,true).setValue(WeaponCaseBlock.FACING,facing);
            ItemStack host=new ItemStack(block);
            WeaponCaseInventory contents=new WeaponCaseInventory(7,()->{});
            contents.insertItem(0,gun(),false);
            host.getOrCreateTag().put(WeaponCaseInventory.CONTENTS,contents.write());
            level.setBlock(root,state,3);
            block.setPlacedBy(level,root,state,null,host);
            net.minecraft.world.phys.AABB bounds=null;
            for(int x=0;x<3;x++) {
                BlockPos pos=WeaponCaseBlock.at(root,state,x,0,0);
                var cell=level.getBlockState(pos);
                if(x!=1) {
                    check(cell.is(ModContent.WEAPON_CASE_PART.get()),"small linked side cell");
                    check(WeaponCasePartBlock.anchor(pos,cell).equals(root),"small part maps to center");
                }
                var shape=cell.getShape(level,pos).bounds().move(pos);
                bounds=bounds==null?shape:bounds.minmax(shape);
            }
            double width=facing.getAxis()==Direction.Axis.Z?bounds.getXsize():bounds.getZsize();
            double depth=facing.getAxis()==Direction.Axis.Z?bounds.getZsize():bounds.getXsize();
            check(Math.abs(width-1.7)<.00001 && Math.abs(depth-.8)<.00001
                    && Math.abs(bounds.getYsize()-.4)<.00001,"small dimensions 1.7x.8x.4 in all directions");
            var drops=net.minecraft.world.level.block.Block.getDrops(state,level,root,level.getBlockEntity(root));
            check(drops.size()==1 && ItemStack.isSameItemSameTags(host,drops.get(0)),"small variant drop preserves gun NBT");
            level.destroyBlock(root,false);
            for(int x=0;x<3;x++) check(level.getBlockState(WeaponCaseBlock.at(root,state,x,0,0)).isAir(),"small parts clean up");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ver8_gun_attachment_size_exceptions(GameTestHelper helper) {
        for(String id:new String[]{"tacz:springfield1873","tacz:kar98","tacz:svd","tacz:mrad","tacz:m1866"}) {
            for(String type:new String[]{"sniper","rifle","dmr"}) {
                var size=GunFootprintRules.apply(6,1,id,type,"","",true,"");
                check(size.height()==1,"magazine does not enlarge "+id);
            }
        }
        for(String id:new String[]{"tacz:springfield1873","tacz:kar98","tacz:m1866"})
            for(String type:new String[]{"sniper","rifle","dmr"})
                check(GunFootprintRules.apply(6,1,id,type,"","",true,"tacz:scope_standard_8x").height()==1,
                        "scope and magazine do not enlarge "+id);
        for(String id:new String[]{"tacz:svd","tacz:mrad"})
            check(GunFootprintRules.apply(6,1,id,"sniper","","",true,"tacz:scope_standard_8x").height()==2,
                    "SVD/MRAD scope row retained");
        check(GunFootprintRules.apply(4,2,"tacz:m4a1","rifle","","",true,"").height()==3,
                "other magazine size rules retained");
        helper.succeed();
    }
}
