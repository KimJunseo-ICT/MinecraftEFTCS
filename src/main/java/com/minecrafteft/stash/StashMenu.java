package com.minecrafteft.stash;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.minecrafteft.registry.ModContent;
import com.minecrafteft.inventory.DayPackItem;
import com.minecrafteft.inventory.EquipmentGrid;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.network.chat.Component;

public final class StashMenu extends AbstractContainerMenu {
    public static final int ROTATE_CARRIED_BUTTON = 0;
    public static final int CLOSE_CASE_BUTTON = 1;
    public static final int OPEN_CASE_BUTTON = 1000;
    private static final String ROTATED_TAG = "MinecraftEFTStashRotated";
    public static final int VIEW_ROWS = 13;
    // A rotated gun can be up to 8 cells tall; include all possible anchors above the view.
    public static final int CONTEXT_ROWS = 9;
    public static final int STASH_X = 408;
    public static final int GRID_Y = 46;
    public static final int PLAYER_X = 16;
    public static final int PLAYER_Y = 46;

    private static final Footprint ONE_CELL = new Footprint(1, 1);
    private static final Map<String, Footprint> TACZ_GUN_FOOTPRINTS = Map.ofEntries(
            Map.entry("tacz:taurus943", ONE_CELL),
            Map.entry("tacz:glock_17", new Footprint(2, 1)),
            Map.entry("tacz:cz75", new Footprint(2, 1)),
            Map.entry("tacz:p320", new Footprint(2, 1)),
            Map.entry("tacz:m1911", new Footprint(2, 1)),
            Map.entry("tacz:b93r", new Footprint(2, 1)),
            Map.entry("tacz:tec_9", new Footprint(2, 1)),
            Map.entry("tacz:m9a4", new Footprint(2, 1)),
            Map.entry("tacz:hk_mk23", new Footprint(2, 1)),
            Map.entry("tacz:rhino357", new Footprint(2, 1)),
            Map.entry("tacz:timeless50", new Footprint(2, 1)),
            Map.entry("tacz:deagle", new Footprint(2, 1)),
            Map.entry("tacz:deagle_golden", new Footprint(2, 1)),
            Map.entry("tacz:taurus500", new Footprint(3, 1)),
            Map.entry("tacz:lonetrail", new Footprint(3, 1)),
            Map.entry("tacz:uzi", new Footprint(2, 1)),
            Map.entry("tacz:hk_mp7a1", new Footprint(2, 1)),
            Map.entry("tacz:hk_mp5a5", new Footprint(2, 2)),
            Map.entry("tacz:ump45", new Footprint(3, 2)),
            Map.entry("tacz:vector45", new Footprint(2, 2)),
            Map.entry("tacz:p90", new Footprint(3, 2)),

            // Shotguns, rifles and heavy weapons (width x height in stash cells).
            Map.entry("tacz:db_short", new Footprint(2, 1)),
            Map.entry("tacz:m320", new Footprint(3, 2)),
            Map.entry("tacz:spas_12", new Footprint(4, 1)),
            Map.entry("tacz:m1866", new Footprint(4, 1)),

            Map.entry("tacz:db_long", new Footprint(5, 1)),
            Map.entry("tacz:m870", new Footprint(5, 1)),
            Map.entry("tacz:m1897", new Footprint(5, 1)),
            Map.entry("tacz:m1014", new Footprint(5, 1)),
            Map.entry("tacz:sks_tactical", new Footprint(5, 1)),
            Map.entry("tacz:scar_mk20", new Footprint(5, 1)),
            Map.entry("tacz:mk14", new Footprint(5, 1)),
            Map.entry("tacz:spr15hb", new Footprint(5, 1)),
            Map.entry("tacz:m700", new Footprint(5, 1)),

            Map.entry("tacz:aa12", new Footprint(4, 2)),
            Map.entry("tacz:ak47", new Footprint(4, 2)),
            Map.entry("tacz:m4a1", new Footprint(4, 2)),
            Map.entry("tacz:scar_h", new Footprint(4, 2)),
            Map.entry("tacz:scar_l", new Footprint(4, 2)),
            Map.entry("tacz:hk416d", new Footprint(4, 2)),
            Map.entry("tacz:aug", new Footprint(4, 2)),
            Map.entry("tacz:qbz_95", new Footprint(4, 2)),
            Map.entry("tacz:g36k", new Footprint(4, 2)),
            Map.entry("tacz:qbz_191", new Footprint(4, 2)),
            Map.entry("tacz:mcx_spear_lt", new Footprint(4, 2)),
            Map.entry("tacz:qr_24", new Footprint(4, 2)),
            Map.entry("tacz:mk18", new Footprint(4, 2)),
            Map.entry("tacz:urgi", new Footprint(4, 2)),

            Map.entry("tacz:hk_g3", new Footprint(5, 2)),
            Map.entry("tacz:m16a1", new Footprint(5, 2)),
            Map.entry("tacz:m16a4", new Footprint(5, 2)),
            Map.entry("tacz:type_81", new Footprint(5, 2)),
            Map.entry("tacz:fn_fal", new Footprint(5, 2)),
            Map.entry("tacz:hcar", new Footprint(5, 2)),
            Map.entry("tacz:ak74m", new Footprint(5, 2)),
            Map.entry("tacz:ak12", new Footprint(5, 2)),
            Map.entry("tacz:fn_evolys", new Footprint(5, 2)),

            Map.entry("tacz:ai_awp", new Footprint(6, 1)),
            Map.entry("tacz:springfield1873", new Footprint(6, 1)),
            Map.entry("tacz:kar98", new Footprint(6, 1)),
            Map.entry("tacz:svd", new Footprint(6, 1)),
            Map.entry("tacz:mrad", new Footprint(6, 1)),

            Map.entry("tacz:m249", new Footprint(6, 2)),
            Map.entry("tacz:rpk", new Footprint(6, 2)),
            Map.entry("tacz:pkm", new Footprint(6, 2)),

            Map.entry("tacz:m95", new Footprint(7, 2)),
            Map.entry("tacz:m107", new Footprint(7, 2)),
            Map.entry("tacz:hm50b", new Footprint(7, 2)),
            Map.entry("tacz:rpg7", new Footprint(7, 2)),
            Map.entry("tacz:minigun", new Footprint(7, 3))
    );

    private final IItemHandler stashItems;
    private final int stashSlots;
    private final int rows;
    private final int visibleRows;
    private final int visibleSlots;
    private final StashViewportHandler viewport;
    private final StashViewportHandler contextViewport;
    private final java.util.List<StashSlot> displaySlots = new ArrayList<>();
    private final StashSavedData.StashRecord record;
    private int scrollRow;
    private final boolean clientView;
    private boolean caseOnly;
    private com.minecrafteft.block.WeaponCaseBlockEntity caseEntity;
    private final CasePopupHandler popupHandler = new CasePopupHandler(this);
    private int popupSlotStart;
    private int openedCaseIndex = -1;
    private ItemStack openedCaseHost = ItemStack.EMPTY;
    private WeaponCaseInventory openedCaseInventory;
    private EquipmentGrid openedBagInventory;
    private boolean openedBag;
    private com.minecrafteft.block.WeaponCaseKind openedCaseKind = com.minecrafteft.block.WeaponCaseKind.LARGE;
    private net.minecraft.core.BlockPos viewedAnchor;
    private net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> viewedDimension;
    public final com.minecrafteft.inventory.LoadoutSlots loadout;

    /** Client constructor called by Forge after receiving the server's menu packet. */
    public StashMenu(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
        this(containerId, playerInventory, new ItemStackHandler(StashTier.FOUR.slots()), data.readInt(), null);
        caseOnly = data.readBoolean();
    }

    public StashMenu(int id, Inventory inventory, com.minecrafteft.block.WeaponCaseBlockEntity entity) {
        this(id, inventory, entity.inventory(), entity.inventory().getSlots(), null);
        caseOnly = true;
        caseEntity = entity;
    }

    /** Server constructor uses the inventory stored in overworld SavedData. */
    public StashMenu(int containerId, Inventory playerInventory, StashSavedData.StashRecord record) {
        this(containerId, playerInventory, record.items(), record.tier().slots(), record);
    }

    private StashMenu(int containerId, Inventory playerInventory, IItemHandler items,
                      int stashSlots, StashSavedData.StashRecord record) {
        super(ModContent.STASH_MENU.get(), containerId);
        var equipmentState=com.minecrafteft.inventory.LoadoutState.of(playerInventory.player);
        if(equipmentState.meleeHost>=0) {
            equipmentState.restoreMelee(playerInventory.player);
            if(playerInventory.player instanceof ServerPlayer serverPlayer) com.minecrafteft.inventory.LoadoutEvents.sync(serverPlayer);
        }
        this.stashItems = items;
        this.stashSlots = Math.max(10, Math.min(StashTier.FOUR.slots(), stashSlots));
        this.rows = this.stashSlots / 10;
        this.visibleRows = Math.min(VIEW_ROWS, rows);
        this.visibleSlots = visibleRows * 10;
        this.record = record;
        this.clientView = playerInventory.player.level().isClientSide;
        if (record != null && record.anchorPos() != null && record.anchorDimension() != null) {
            viewedAnchor = record.anchorPos().immutable();
            viewedDimension = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                    ResourceLocation.parse(record.anchorDimension()));
        }
        if (record != null && !reflowChangedFootprints()) {
            playerInventory.player.sendSystemMessage(
                    Component.translatable("message.minecrafteft.stash_resize_full"));
        }
        this.viewport = new StashViewportHandler(stashItems, this, visibleRows);
        this.contextViewport = new StashViewportHandler(stashItems, this, CONTEXT_ROWS);
        this.contextViewport.setFirstRow(-CONTEXT_ROWS);

        for (int row = 0; row < visibleRows; row++) {
            for (int column = 0; column < 10; column++) {
                int index = row * 10 + column;
                StashSlot slot = new StashSlot(this, viewport, index, STASH_X + column * 18, GRID_Y + row * 18);
                addSlot(slot);
                displaySlots.add(slot);
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9,-10000,-10000) {
                    @Override public boolean isActive() { return false; }
                    @Override public boolean mayPlace(ItemStack stack) { return false; }
                    @Override public boolean mayPickup(Player player) { return false; }
                });
            }
        }
        var hotbar=new java.util.ArrayList<Slot>();
        for (int column = 0; column < 9; column++) hotbar.add(addSlot(com.minecrafteft.inventory.LoadoutSlots.hotbar(playerInventory,column)));
        // Keep player slot indexes stable. These extra menu slots synchronize off-screen
        // anchors, and can be picked up only when their footprint overlaps the visible view.
        for (int row = 0; row < CONTEXT_ROWS; row++) {
            for (int column = 0; column < 10; column++) {
                int index = row * 10 + column;
                int viewSlot = index - CONTEXT_ROWS * 10;
                StashSlot slot = new StashSlot(this, contextViewport, index, STASH_X + column * 18,
                        GRID_Y + (row - CONTEXT_ROWS) * 18, viewSlot);
                addSlot(slot);
                displaySlots.add(slot);
            }
        }
        popupSlotStart = slots.size();
        for (int index = 0; index < WeaponCaseInventory.SLOTS; index++) {
            addSlot(new CasePopupSlot(this, popupHandler, index));
        }
        loadout=new com.minecrafteft.inventory.LoadoutSlots(playerInventory,this::addSlot,hotbar);
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override public int get() { return openedCaseIndex; }
            @Override public void set(int value) { openedCaseIndex = value; }
        });
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override public int get() { return openedBag ? -1 : openedCaseKind.ordinal(); }
            @Override public void set(int value) {
                openedBag = value == -1;
                if (value >= 0 && value < com.minecrafteft.block.WeaponCaseKind.values().length)
                    openedCaseKind = com.minecrafteft.block.WeaponCaseKind.values()[value];
            }
        });
    }

    public int rows() { return rows; }
    public int visibleRows() { return visibleRows; }
    public int visibleSlots() { return visibleSlots; }
    public int scrollRow() { return scrollRow; }
    public int maxScrollRow() { return Math.max(0, rows - visibleRows); }
    public int storageIndexForViewSlot(int viewSlot) { return scrollRow * 10 + viewSlot; }
    public boolean isStashSlot(Slot slot) { return slot instanceof StashSlot; }
    public ItemStack stashStackAtStorageIndex(int index) {
        return index < 0 || index >= stashSlots ? ItemStack.EMPTY : stashItems.getStackInSlot(index);
    }
    public java.util.List<StashSlot> stashDisplaySlots() { return java.util.Collections.unmodifiableList(displaySlots); }
    public boolean isClientView() { return clientView; }
    public boolean isCaseOnly() { return caseOnly; }
    public boolean isCaseOpen() { return openedCaseIndex >= 0; }
    public boolean isOpenCaseStorageIndex(int index) { return isCaseOpen() && index == openedCaseIndex; }
    public boolean isLockedStorageIndex(int index) {
        return isOpenCaseStorageIndex(index);
    }
    public Slot caseSlot(int index) { return slots.get(popupSlotStart + index); }
    public boolean isInteractiveCaseSlot(Slot slot) {
        return isCaseOpen() && slot.index>=popupSlotStart && slot.index<popupSlotStart+openCaseSlots()
                && slots.get(slot.index)==slot;
    }
    public ItemStack caseStack(int index) { return popupHandler.getStackInSlot(index); }
    public boolean canPlaceInCase(int index, ItemStack stack) {
        return isCaseOpen() && popupHandler.isItemValid(index, stack);
    }
    WeaponCaseInventory openInventory() { return openedCaseInventory; }
    IItemHandlerModifiable popupInventory() {
        return openedBag ? openedBagInventory : openedCaseInventory;
    }
    public boolean isBackpackOpen() { return isCaseOpen() && openedBag; }
    public int openColumns() { return openedBag ? DayPackItem.WIDTH : 10; }
    public boolean acceptsPopupItem(ItemStack stack) {
        return isCaseOpen() && (openedBag
                ? !stack.isEmpty() && com.minecrafteft.inventory.StorageEquipment.type(stack).isEmpty()
                : WeaponCaseInventory.accepts(stack));
    }
    public com.minecrafteft.block.WeaponCaseKind openCaseKind() { return openedCaseKind; }
    public int openCaseRows() { return openedBag ? DayPackItem.HEIGHT : openedCaseKind.rows(); }
    public int openCaseSlots() { return openColumns() * openCaseRows(); }

    private boolean openCase(int index) {
        if (caseOnly || index < 0 || index >= stashSlots) return false;
        ItemStack host = stashItems.getStackInSlot(index);
        int row = index / 10;
        if (!isOpenable(host) || row >= scrollRow + visibleRows
                || row + footprintHeight(host) <= scrollRow) return false;
        persistOpenCase();
        openedCaseIndex = index;
        openedCaseHost = host;
        openedBag = host.getItem() instanceof DayPackItem;
        openedBagInventory = null;
        openedCaseInventory = null;
        if (openedBag) {
            openedBagInventory = new EquipmentGrid(() -> host, () -> {
                if (!clientView && caseHostValid()) ((IItemHandlerModifiable) stashItems).setStackInSlot(index, host);
            });
            if (clientView) popupHandler.preview(openedBagInventory);
            return true;
        }
        openedCaseKind = com.minecrafteft.block.WeaponCaseKind.of(host);
        openedCaseInventory = new WeaponCaseInventory(openedCaseKind.rows(), () -> {
            if (clientView || openedCaseInventory == null || !caseHostValid()) return;
            openedCaseHost.getOrCreateTag().put(WeaponCaseInventory.CONTENTS, openedCaseInventory.write());
            ((net.minecraftforge.items.IItemHandlerModifiable) stashItems).setStackInSlot(openedCaseIndex, openedCaseHost);
        });
        openedCaseInventory.readItem(host);
        if (clientView) popupHandler.preview(openedCaseInventory);
        return true;
    }

    private boolean caseHostValid() {
        return isCaseOpen() && stashItems.getStackInSlot(openedCaseIndex) == openedCaseHost
                && isOpenable(openedCaseHost);
    }

    private void closeCase() {
        persistOpenCase();
        openedCaseIndex = -1;
        openedBag = false;
        openedBagInventory = null;
        openedCaseHost = ItemStack.EMPTY;
        openedCaseInventory = null;
    }

    @Override public void broadcastChanges() {
        persistOpenCase();
        if (!clientView && isCaseOpen() && !caseHostValid()) closeCase();
        super.broadcastChanges();
    }

    private void persistOpenCase() {
        if (clientView || !caseHostValid()) return;
        if (openedBag) {
            openedBagInventory.flush();
            return;
        }
        if (openedCaseInventory == null) return;
        var contents=openedCaseInventory.write();
        if(!contents.equals(openedCaseHost.getOrCreateTag().getCompound(WeaponCaseInventory.CONTENTS))) {
            openedCaseHost.getOrCreateTag().put(WeaponCaseInventory.CONTENTS,contents);
            ((net.minecraftforge.items.IItemHandlerModifiable)stashItems).setStackInSlot(openedCaseIndex,openedCaseHost);
        }
    }

    /** Preserve valid anchors; relocate overlaps caused by updated size/attachment rules.
     * Commit only a complete plan, so a full stash can never lose an item. */
    private boolean reflowChangedFootprints() {
        ItemStack[] planned = new ItemStack[stashSlots];
        Arrays.fill(planned, ItemStack.EMPTY);
        boolean[] occupied = new boolean[stashSlots];
        var displaced = new ArrayList<ItemStack>();
        for (int index = 0; index < stashSlots; index++) {
            ItemStack stack = stashItems.getStackInSlot(index);
            if (stack.isEmpty()) continue;
            if (fitsLayout(occupied, index, stack)) {
                reserveLayout(occupied, index, stack);
                planned[index] = stack;
            } else {
                displaced.add(stack);
            }
        }
        if (displaced.isEmpty()) return true;
        for (ItemStack stack : displaced) {
            boolean placed = false;
            for (int index = 0; index < stashSlots; index++) {
                if (!fitsLayout(occupied, index, stack)) continue;
                reserveLayout(occupied, index, stack);
                planned[index] = stack;
                placed = true;
                break;
            }
            if (!placed) return false;
        }
        for (int index = 0; index < stashSlots; index++) record.items().setStackInSlot(index, ItemStack.EMPTY);
        for (int index = 0; index < stashSlots; index++) record.items().setStackInSlot(index, planned[index]);
        return true;
    }

    private boolean fitsLayout(boolean[] occupied, int anchor, ItemStack stack) {
        int width = footprintWidth(stack);
        int height = footprintHeight(stack);
        if (anchor % 10 + width > 10 || anchor / 10 + height > rows) return false;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (occupied[anchor + y * 10 + x]) return false;
            }
        }
        return true;
    }

    private void reserveLayout(boolean[] occupied, int anchor, ItemStack stack) {
        for (int y = 0; y < footprintHeight(stack); y++) {
            for (int x = 0; x < footprintWidth(stack); x++) occupied[anchor + y * 10 + x] = true;
        }
    }

    public void setScrollRow(int row) {
        int nextRow = Math.max(0, Math.min(maxScrollRow(), row));
        if (nextRow == scrollRow) return;
        if (clientView) {
            for (int slot = 0; slot < stashItems.getSlots(); slot++) {
                int count = stashItems.getStackInSlot(slot).getCount();
                if (count > 0) stashItems.extractItem(slot, count, false);
            }
        }
        scrollRow = nextRow;
        viewport.setFirstRow(scrollRow);
        contextViewport.setFirstRow(scrollRow - CONTEXT_ROWS);
    }

    public boolean canPlaceAt(int targetIndex, ItemStack stack) {
        if (stack.isEmpty() || targetIndex < 0 || targetIndex >= stashSlots) return false;
        if (isLockedStorageIndex(targetIndex) || (caseOnly && !WeaponCaseInventory.accepts(stack))) return false;
        int width = footprintWidth(stack);
        int height = footprintHeight(stack);
        int targetX = targetIndex % 10;
        int targetY = targetIndex / 10;
        if (targetX + width > 10 || targetY + height > rows) return false;

        for (int otherIndex = 0; otherIndex < stashSlots; otherIndex++) {
            if (otherIndex == targetIndex) continue;
            ItemStack other = stashItems.getStackInSlot(otherIndex);
            if (other.isEmpty()) continue;
            int otherX = otherIndex % 10;
            int otherY = otherIndex / 10;
            if (rectanglesOverlap(targetX, targetY, width, height,
                    otherX, otherY, footprintWidth(other), footprintHeight(other))) return false;
        }
        return true;
    }

    public boolean isReservedStorageIndex(int index) {
        if (index < 0 || index >= stashSlots) return true;
        int x = index % 10;
        int y = index / 10;
        for (int anchor = 0; anchor < stashSlots; anchor++) {
            if (anchor == index) continue;
            ItemStack stack = stashItems.getStackInSlot(anchor);
            if (stack.isEmpty()) continue;
            int anchorX = anchor % 10;
            int anchorY = anchor / 10;
            if (x >= anchorX && x < anchorX + footprintWidth(stack)
                    && y >= anchorY && y < anchorY + footprintHeight(stack)) return true;
        }
        return false;
    }

    public static int footprintWidth(ItemStack stack) {
        Footprint size = footprint(stack);
        return isRotated(stack) ? size.height() : size.width();
    }

    public static int footprintHeight(ItemStack stack) {
        Footprint size = footprint(stack);
        return isRotated(stack) ? size.width() : size.height();
    }

    public static boolean isRotated(ItemStack stack) {
        return isGridItem(stack) && stack.hasTag() && stack.getTag().getBoolean(ROTATED_TAG);
    }

    public static ItemStack rotatedCopy(ItemStack stack) {
        ItemStack rotated = stack.copy();
        if (isRotated(rotated)) rotated.removeTagKey(ROTATED_TAG);
        else rotated.getOrCreateTag().putBoolean(ROTATED_TAG, true);
        return rotated;
    }

    @Override public void clicked(int slotId, int button, ClickType type, Player player) {
        if(com.minecrafteft.inventory.LoadoutSlots.blockedClick(this,slotId,button,type,player)) return;
        if (!caseOnly && type == ClickType.PICKUP && button == 0 && isTaczGun(getCarried())
                && slotId >= 0 && slotId < slots.size() && slots.get(slotId) instanceof StashSlot slot) {
            int index = storageIndexForViewSlot(slot.viewSlot());
            ItemStack host = stashStackAtStorageIndex(index);
            int row = index / 10;
            if (isWeaponCase(host) && row < scrollRow + visibleRows
                    && row + footprintHeight(host) > scrollRow) {
                // The server owns both inventories. Even a full case consumes this
                // click, so failure never swaps the gun with the case itself.
                if (!clientView) storeCarriedInCase(index, host);
                return;
            }
        }
        super.clicked(slotId, button, type, player);
    }

    private void storeCarriedInCase(int index, ItemStack host) {
        boolean opened = isOpenCaseStorageIndex(index) && caseHostValid();
        WeaponCaseInventory inventory;
        if (opened) inventory = openedCaseInventory;
        else {
            inventory = new WeaponCaseInventory(com.minecrafteft.block.WeaponCaseKind.of(host).rows(), () -> { });
            inventory.readItem(host);
        }
        ItemStack carried = getCarried();
        ItemStack remainder = inventory.insertFirstFit(carried);
        if (remainder.getCount() == carried.getCount()) return;
        // Use the live open inventory when applicable; otherwise persisting the
        // stale popup later could overwrite the newly inserted gun.
        host.getOrCreateTag().put(WeaponCaseInventory.CONTENTS, inventory.write());
        ((net.minecraftforge.items.IItemHandlerModifiable) stashItems).setStackInSlot(index, host);
        setCarried(remainder);
    }

    /** Read-only preview using the same rotation and rectangle check as insertion. */
    public boolean canStoreInCase(int index, ItemStack gun) {
        ItemStack host = stashStackAtStorageIndex(index);
        if (caseOnly || !isTaczGun(gun) || !isWeaponCase(host)) return false;
        if (!clientView && isOpenCaseStorageIndex(index) && caseHostValid())
            return openedCaseInventory.canFit(gun);
        var inventory = new WeaponCaseInventory(com.minecrafteft.block.WeaponCaseKind.of(host).rows(), () -> { });
        if (clientView && isOpenCaseStorageIndex(index)) inventory.read(popupHandler.snapshot());
        else inventory.readItem(host);
        return inventory.canFit(gun);
    }

    public static String gunCategory(ItemStack stack) {
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null) return "";
        ResourceLocation id = gun.getGunId(stack);
        return id == null ? "" : TimelessAPI.getCommonGunIndex(id).map(index -> index.getType()).orElse("");
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if(button==2) { var state=com.minecrafteft.inventory.LoadoutState.of(player);state.hideBackpack=!state.hideBackpack;state.dirty=true;return true; }
        if (button == CLOSE_CASE_BUTTON) {
            closeCase();
            if (!clientView) broadcastFullState();
            return true;
        }
        if (button >= OPEN_CASE_BUTTON && button < OPEN_CASE_BUTTON + stashSlots) {
            boolean opened = openCase(button - OPEN_CASE_BUTTON);
            if (!clientView) broadcastFullState();
            return opened;
        }
        if (button != ROTATE_CARRIED_BUTTON || !isGridItem(getCarried())) return false;
        // Use a copy so menu sync notices the change and the stored stack keeps its orientation.
        setCarried(rotatedCopy(getCarried()));
        if (!player.level().isClientSide) broadcastFullState();
        return true;
    }

    public static boolean hasExpandedFootprint(ItemStack stack) {
        return isTaczGun(stack) || footprintWidth(stack) > 1 || footprintHeight(stack) > 1;
    }

    public static boolean isTaczGun(ItemStack stack) {
        return !stack.isEmpty() && IGun.getIGunOrNull(stack) != null;
    }

    public static boolean isWeaponCase(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof com.minecrafteft.block.WeaponCaseItem;
    }

    public static boolean isOpenable(ItemStack stack) {
        return isWeaponCase(stack) || stack.getItem() instanceof DayPackItem;
    }
    public static boolean isGridItem(ItemStack stack) { return isTaczGun(stack) || isOpenable(stack); }

    private static Footprint footprint(ItemStack stack) {
        if (stack.isEmpty()) return ONE_CELL;
        if (stack.getItem() instanceof DayPackItem) return new Footprint(DayPackItem.WIDTH, DayPackItem.HEIGHT);
        if (isWeaponCase(stack)) return new Footprint(5, com.minecrafteft.block.WeaponCaseKind.of(stack).footprintHeight());
        if (stack.is(Items.DIAMOND_SWORD)) return new Footprint(2, 2);

        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null) return ONE_CELL;
        ResourceLocation gunId = gun.getGunId(stack);
        if (gunId == null) return ONE_CELL;
        Footprint base = TACZ_GUN_FOOTPRINTS.get(gunId.toString());
        if (base == null) return ONE_CELL;
        String type = TimelessAPI.getCommonGunIndex(gunId).map(index -> index.getType()).orElse("");
        GunFootprintRules.Size size = GunFootprintRules.apply(base.width(), base.height(),
                gunId.toString(), type,
                effectiveAttachment(gun, stack, AttachmentType.MUZZLE),
                effectiveAttachment(gun, stack, AttachmentType.STOCK),
                !effectiveAttachment(gun, stack, AttachmentType.EXTENDED_MAG).isEmpty(),
                effectiveAttachment(gun, stack, AttachmentType.SCOPE));
        return new Footprint(size.width(), size.height());
    }

    private static String effectiveAttachment(IGun gun, ItemStack stack, AttachmentType type) {
        ResourceLocation id = gun.getAttachmentId(stack, type);
        if (id == null || DefaultAssets.EMPTY_ATTACHMENT_ID.equals(id)) {
            id = gun.getBuiltInAttachmentId(stack, type);
        }
        return id == null || DefaultAssets.EMPTY_ATTACHMENT_ID.equals(id) ? "" : id.toString();
    }

    private record Footprint(int width, int height) { }

    private static boolean rectanglesOverlap(int ax, int ay, int aw, int ah,
                                             int bx, int by, int bw, int bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    @Override
    public boolean stillValid(Player player) {
        if (clientView) return true;
        if (caseEntity != null) return !caseEntity.isRemoved()
                && player.level() == caseEntity.getLevel()
                && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(caseEntity.getBlockPos())) <= 64;
        if (record == null) return false;
        if (!player.getUUID().equals(record.owner())) return false;
        if (player.level().isClientSide) return true;
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        return StashService.withinRange(serverPlayer, record) && StashSavedData.get(serverPlayer.serverLevel()).hasActiveAnchor(
                serverPlayer.serverLevel().getServer(), record);
    }

    public boolean isViewingCaseBlock(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                                      net.minecraft.core.BlockPos anchor) {
        return caseEntity != null && caseEntity.getLevel() != null
                && caseEntity.getLevel().dimension().equals(dimension) && caseEntity.getBlockPos().equals(anchor);
    }

    public boolean isViewingContainer(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                                      net.minecraft.core.BlockPos anchor) {
        return dimension.equals(viewedDimension) && anchor.equals(viewedAnchor);
    }

    @Override public void removed(Player player) {
        super.removed(player);
        closeCase();
        if (caseEntity != null) caseEntity.stopViewing(player.getUUID());
        if (player instanceof ServerPlayer serverPlayer && viewedDimension != null && viewedAnchor != null) {
            var level = serverPlayer.server.getLevel(viewedDimension);
            if (level != null && level.getBlockEntity(viewedAnchor) instanceof com.minecrafteft.block.PersonalStashBlockEntity stash)
                stash.stopViewing(player.getUUID());
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack empty = ItemStack.EMPTY;
        if (index < 0 || index >= slots.size()) return empty;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return empty;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index >= popupSlotStart && index < popupSlotStart+WeaponCaseInventory.SLOTS) {
            if (!moveItemStackTo(stack, 0, visibleSlots, false)
                    && !com.minecrafteft.inventory.LoadoutInventory.insert(player,stack)) return empty;
        } else if (acceptsPopupItem(stack)
                && moveItemStackTo(stack, popupSlotStart, popupSlotStart + openCaseSlots(), false)) {
            // The case handler persists each successful transfer into the host item.
        } else if (isStashSlot(slot)) {
            if (!com.minecrafteft.inventory.LoadoutInventory.insert(player,stack)) return empty;
        } else if (!moveItemStackTo(stack, 0, visibleSlots, false)) {
            return empty;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
