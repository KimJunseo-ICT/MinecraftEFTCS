package com.minecrafteft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.minecrafteft.MinecraftEFTMod;
import com.minecrafteft.block.PersonalStashBlock;
import com.minecrafteft.block.PersonalStashBlockEntity;
import com.minecrafteft.block.ContainerPartBlock;
import com.minecrafteft.stash.StashMenu;
import com.minecrafteft.stash.StashTier;

public final class ModContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MinecraftEFTMod.MOD_ID);
    private static final DeferredRegister<net.minecraft.world.item.Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MinecraftEFTMod.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MinecraftEFTMod.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MinecraftEFTMod.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MinecraftEFTMod.MOD_ID);
    private static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,MinecraftEFTMod.MOD_ID);
    public static final RegistryObject<net.minecraft.world.item.Item> ALU_SPLINT=ITEMS.register("alu_splint",()->new com.minecrafteft.status.SimpleTreatmentItem(true));
    public static final RegistryObject<net.minecraft.world.item.Item> ANALGIN=ITEMS.register("analgin_painkillers",()->new com.minecrafteft.status.SimpleTreatmentItem(false));
    public static final RegistryObject<net.minecraft.world.entity.EntityType<com.minecrafteft.status.BloodMarkEntity>> BLOOD_MARK=
            ENTITIES.register("blood_mark",()->net.minecraft.world.entity.EntityType.Builder
                    .<com.minecrafteft.status.BloodMarkEntity>of(com.minecrafteft.status.BloodMarkEntity::new,net.minecraft.world.entity.MobCategory.MISC)
                    .sized(.7F,.02F).clientTrackingRange(10).updateInterval(60).noSave().noSummon().build("minecrafteft:blood_mark"));
    public static final RegistryObject<net.minecraft.world.item.Item> IFAK_MEDIKIT =
            ITEMS.register("ifak_medikit", () -> new com.minecrafteft.medical.MedikitItem(com.minecrafteft.medical.MedikitType.IFAK));
    public static final RegistryObject<net.minecraft.world.item.Item> AI2_MEDIKIT =
            ITEMS.register("ai2_medikit",com.minecrafteft.medical.Ai2MedikitItem::new);
    public static final RegistryObject<com.minecrafteft.inventory.DayPackItem> DAY_PACK =
            ITEMS.register("lbt_8005a_day_pack", com.minecrafteft.inventory.DayPackItem::new);
    public static final RegistryObject<net.minecraft.world.item.Item> DOGTAG =
            ITEMS.register("basic_dogtag",com.minecrafteft.inventory.DogtagItem::new);
    public static final RegistryObject<net.minecraft.world.entity.EntityType<com.minecrafteft.corpse.CorpseEntity>> CORPSE =
            ENTITIES.register("player_corpse",()->net.minecraft.world.entity.EntityType.Builder
                    .<com.minecrafteft.corpse.CorpseEntity>of(com.minecrafteft.corpse.CorpseEntity::new,net.minecraft.world.entity.MobCategory.MISC)
                    .sized(1.8F,.55F).clientTrackingRange(10).updateInterval(20).noSummon().build("minecrafteft:player_corpse"));
    public static final RegistryObject<MenuType<com.minecrafteft.corpse.CorpseMenu>> CORPSE_MENU =
            MENUS.register("corpse_loot",()->IForgeMenuType.create(com.minecrafteft.corpse.CorpseMenu::new));
    public static final RegistryObject<net.minecraft.world.entity.EntityType<com.minecrafteft.block.StashSeatEntity>> STASH_SEAT =
            ENTITIES.register("stash_seat",()->net.minecraft.world.entity.EntityType.Builder
                    .<com.minecrafteft.block.StashSeatEntity>of(com.minecrafteft.block.StashSeatEntity::new,net.minecraft.world.entity.MobCategory.MISC)
                    .sized(.01F,.01F).clientTrackingRange(8).noSave().noSummon().build("minecrafteft:stash_seat"));

    public static final RegistryObject<PersonalStashBlock> STASH_TIER_1 = registerStash("personal_stash_1", StashTier.ONE);
    public static final RegistryObject<PersonalStashBlock> STASH_TIER_2 = registerStash("personal_stash_2", StashTier.TWO);
    public static final RegistryObject<PersonalStashBlock> STASH_TIER_3 = registerStash("personal_stash_3", StashTier.THREE);
    public static final RegistryObject<PersonalStashBlock> STASH_TIER_4 = registerStash("personal_stash_4", StashTier.FOUR);
    public static final RegistryObject<ContainerPartBlock> STASH_PART = BLOCKS.register("stash_container_part", ContainerPartBlock::new);
    public static final RegistryObject<com.minecrafteft.block.WeaponCaseBlock> WEAPON_CASE =
            BLOCKS.register("large_weapon_case", com.minecrafteft.block.WeaponCaseBlock::new);
    public static final RegistryObject<com.minecrafteft.block.WeaponCasePartBlock> WEAPON_CASE_PART =
            BLOCKS.register("weapon_case_part", com.minecrafteft.block.WeaponCasePartBlock::new);
    public static final RegistryObject<net.minecraft.world.item.Item> WEAPON_CASE_ITEM = ITEMS.register("large_weapon_case",
            () -> new com.minecrafteft.block.WeaponCaseItem(WEAPON_CASE.get()));
    public static final RegistryObject<com.minecrafteft.block.WeaponCaseBlock> SMALL_WEAPON_CASE = registerCase(com.minecrafteft.block.WeaponCaseKind.STANDARD);
    public static final RegistryObject<com.minecrafteft.block.WeaponCaseBlock> AR_TEAM_CASE = registerCase(com.minecrafteft.block.WeaponCaseKind.AR_TEAM);
    public static final RegistryObject<com.minecrafteft.block.WeaponCaseBlock> ELMO_CASE = registerCase(com.minecrafteft.block.WeaponCaseKind.ELMO);
    public static final RegistryObject<com.minecrafteft.block.WeaponCaseBlock> HIDE404_CASE = registerCase(com.minecrafteft.block.WeaponCaseKind.HIDE404);
    public static final RegistryObject<BlockEntityType<com.minecrafteft.block.WeaponCaseBlockEntity>> WEAPON_CASE_ENTITY =
            BLOCK_ENTITIES.register("large_weapon_case", () -> BlockEntityType.Builder.of(
                    com.minecrafteft.block.WeaponCaseBlockEntity::new, WEAPON_CASE.get(), SMALL_WEAPON_CASE.get(),
                    AR_TEAM_CASE.get(), ELMO_CASE.get(), HIDE404_CASE.get()).build(null));

    public static final RegistryObject<BlockEntityType<PersonalStashBlockEntity>> STASH_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("personal_stash", () -> BlockEntityType.Builder.of(
                    PersonalStashBlockEntity::new,
                    STASH_TIER_1.get(), STASH_TIER_2.get(), STASH_TIER_3.get(), STASH_TIER_4.get()).build(null));

    public static final RegistryObject<MenuType<StashMenu>> STASH_MENU =
            MENUS.register("personal_stash", () -> IForgeMenuType.create(StashMenu::new));
    public static final RegistryObject<MenuType<com.minecrafteft.inventory.LoadoutMenu>> LOADOUT_MENU =
            MENUS.register("loadout", () -> new MenuType<>(com.minecrafteft.inventory.LoadoutMenu::new,net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<com.minecrafteft.inventory.PortableBackpackMenu>> PORTABLE_BACKPACK_MENU =
            MENUS.register("portable_backpack", () -> IForgeMenuType.create(com.minecrafteft.inventory.PortableBackpackMenu::new));

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(net.minecraft.network.chat.Component.translatable("itemGroup.minecrafteft"))
                    .icon(() -> new ItemStack(STASH_TIER_1.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(STASH_TIER_1.get());
                        output.accept(STASH_TIER_2.get());
                        output.accept(STASH_TIER_3.get());
                        output.accept(STASH_TIER_4.get());
                        output.accept(WEAPON_CASE_ITEM.get());
                        output.accept(SMALL_WEAPON_CASE.get());
                        output.accept(AR_TEAM_CASE.get());
                        output.accept(ELMO_CASE.get());
                        output.accept(HIDE404_CASE.get());
                        output.accept(AI2_MEDIKIT.get());
                        output.accept(IFAK_MEDIKIT.get());
                        output.accept(ALU_SPLINT.get());output.accept(ANALGIN.get());
                        output.accept(DOGTAG.get());
                        output.accept(DAY_PACK.get());
                    }).build());

    private ModContent() {}

    private static RegistryObject<com.minecrafteft.block.WeaponCaseBlock> registerCase(com.minecrafteft.block.WeaponCaseKind kind) {
        var block = BLOCKS.register(kind.id(), () -> new com.minecrafteft.block.WeaponCaseBlock(kind));
        ITEMS.register(kind.id(), () -> new com.minecrafteft.block.WeaponCaseItem(block.get()));
        return block;
    }

    private static RegistryObject<PersonalStashBlock> registerStash(String name, StashTier tier) {
        RegistryObject<PersonalStashBlock> block = BLOCKS.register(name, () -> new PersonalStashBlock(tier));
        ITEMS.register(name, () -> new BlockItem(block.get(), new net.minecraft.world.item.Item.Properties()));
        return block;
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        TABS.register(modBus);
        ENTITIES.register(modBus);
    }
}
