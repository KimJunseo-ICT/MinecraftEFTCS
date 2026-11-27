package com.minecrafteft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import com.minecrafteft.network.ModNetwork;
import com.minecrafteft.network.ScrollStashPacket;
import com.minecrafteft.stash.StashMenu;
import com.minecrafteft.stash.StashSlot;
import com.tacz.guns.api.item.IGun;
import net.minecraft.world.inventory.ClickType;
import org.lwjgl.glfw.GLFW;
import net.minecraft.resources.ResourceLocation;

public final class StashScreen extends AbstractContainerScreen<StashMenu> {
    private static final int SCREEN_WIDTH = 608;
    private static final int MIN_SCREEN_HEIGHT = 284;
    private static final int GRID_WIDTH = 180;
    private final Component stashHeading = Component.translatable("container.minecrafteft.personal_stash");
    private double currentMouseX;
    private double currentMouseY;
    private final int stashX = StashMenu.STASH_X;
    private double uiScale = 1.0D;
    private double uiOffsetX;
    private double uiOffsetY;
    private boolean categoryButtonsOpen;
    private int firstCategory;
    private String highlightedCategory = "";
    private static final String[] CATEGORIES = {"pistol", "smg", "shotgun", "rifle", "dmr", "sniper", "rpg", "mg"};
    private boolean consumePlacementRelease;
    private boolean rotationKeyDown;
    private boolean caseCloseKeyDown;
    private int caseWidth() { return menu.openColumns() * 18 + 12; }
    private int caseVisibleRows() { return menu.isBackpackOpen() ? menu.openCaseRows() : Math.min(8, menu.openCaseRows() - 1); }
    private int caseHeight() { return 30 + caseVisibleRows() * 18; }
    private int caseX, caseY, caseScroll;
    private boolean casePositioned, draggingCase, caseKeyDown;
    private double caseDragX, caseDragY;
    private boolean draggingStashScrollbar, draggingCaseScrollbar;
    private double scrollbarGrab;
    private long caseNoticeUntil;
    private int insertionCaseIndex = -1;
    private ItemStack insertionHost = ItemStack.EMPTY, insertionGun = ItemStack.EMPTY;
    private java.util.List<ItemStack> insertionContents = java.util.List.of();
    private boolean insertionCaseOpen, insertionFits;
    private static final int CASE_LAYER = 1000;
    private final LoadoutPanel loadoutPanel;

    public StashScreen(StashMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        loadoutPanel=new LoadoutPanel(menu.loadout,true);
        this.imageWidth = SCREEN_WIDTH;
        this.imageHeight = Math.max(MIN_SCREEN_HEIGHT, StashMenu.GRID_Y + menu.visibleRows() * 18 + 8);
        this.inventoryLabelY = StashMenu.PLAYER_Y + 1;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        graphics.fill(x + stashX - 22, y + 10, x + stashX - 21, y + imageHeight - 8, 0xFF46515A);
        graphics.fill(x + 7, y + 37, x + 184, y + 38, 0xFF46515A);
        graphics.fill(x + stashX - 4, y + 37, x + imageWidth - 8, y + 38, 0xFF46515A);

        for (Slot slot : menu.slots) {
            if (!slot.isActive()) continue;
            int slotX = x + slot.x;
            int slotY = y + slot.y;
            graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF69737B);
            graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF252D34);
        }

        enableGridScissor(graphics);
        try {
            drawReservedCells(graphics);
        } finally {
            graphics.disableScissor();
        }
        drawScrollbar(graphics);
        drawCategoryButtons(graphics, mouseX, mouseY);
    }

    private void drawReservedCells(GuiGraphics graphics) {
        for (StashSlot slot : menu.stashDisplaySlots()) {
            ItemStack stack = menu.stashStackAtStorageIndex(menu.storageIndexForViewSlot(slot.viewSlot()));
            if (!intersectsGrid(slot, stack)) continue;
            if (stack.isEmpty() || !StashMenu.hasExpandedFootprint(stack)) continue;
            for (int row = 0; row < StashMenu.footprintHeight(stack); row++) {
                for (int column = 0; column < StashMenu.footprintWidth(stack); column++) {
                    if (row == 0 && column == 0) continue;
                    int cellX = leftPos + slot.x + column * 18;
                    int cellY = topPos + slot.y + row * 18;
                    graphics.fill(cellX, cellY, cellX + 16, cellY + 16, 0xFF303A43);
                }
            }
        }
    }

    private void drawScrollbar(GuiGraphics graphics) {
        if (menu.rows() <= menu.visibleRows()) return;
        drawScrollbar(graphics, stashScrollbar(), 0xFF9AA8B4);
    }

    private StashScrollbar stashScrollbar() {
        return new StashScrollbar(leftPos + imageWidth - 12, topPos + StashMenu.GRID_Y,
                4, menu.visibleRows() * 18, menu.rows(), menu.visibleRows(), menu.scrollRow());
    }

    private StashScrollbar caseScrollbar() {
        return new StashScrollbar(caseX + caseWidth() - 5, caseY + 24, 3, caseVisibleRows() * 18,
                menu.openCaseRows(), caseVisibleRows(), caseScroll);
    }

    private void drawScrollbar(GuiGraphics graphics, StashScrollbar bar, int color) {
        graphics.fill(bar.x(), bar.y(), bar.x() + bar.width(), bar.y() + bar.height(), 0xFF20272D);
        graphics.fill(bar.x(), bar.thumbY(), bar.x() + bar.width(), bar.thumbY() + bar.thumbHeight(), color);
    }

    private void scrollStashTo(int row) {
        int next = Math.max(0, Math.min(menu.maxScrollRow(), row));
        if (next == menu.scrollRow()) return;
        menu.setScrollRow(next);
        ModNetwork.CHANNEL.sendToServer(new ScrollStashPacket(menu.containerId, next));
    }

    private void beginScrollbarDrag(StashScrollbar bar, double mouseY, boolean popup) {
        scrollbarGrab = bar.onThumb(mouseY) ? mouseY - bar.thumbY() : bar.thumbHeight() / 2.0;
        draggingCaseScrollbar = popup;
        draggingStashScrollbar = !popup;
        if (popup) caseScroll = bar.rowAt(mouseY, scrollbarGrab);
        else scrollStashTo(bar.rowAt(mouseY, scrollbarGrab));
    }

    private void drawCategoryButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        drawCategoryButton(graphics, 0, Component.translatable("screen.minecrafteft.categories"),
                categoryButtonsOpen, mouseX, mouseY);
        if (!categoryButtonsOpen) return;
        for (int row=0;row<categoryRows();row++) {
            int i=firstCategory+row;
            drawCategoryButton(graphics, i + 1, Component.translatable("screen.minecrafteft.category." + CATEGORIES[i]),
                    highlightedCategory.equals(CATEGORIES[i]), mouseX, mouseY);
        }
    }

    private void drawCategoryButton(GuiGraphics graphics, int index, Component label,
                                    boolean selected, int mouseX, int mouseY) {
        int x = leftPos + stashX - 18;
        int y = topPos + StashMenu.GRID_Y + (index==0?0:index-firstCategory) * 17;
        boolean hovered = mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 15;
        graphics.fill(x, y, x + 16, y + 15, selected ? 0xFF64FF35 : hovered ? 0xFFB7C9D4 : 0xFF69737B);
        graphics.fill(x + 1, y + 1, x + 15, y + 14, selected ? 0xFF23442C : 0xFF252D34);
        if (index > 0) {
            CategoryIconRenderer.draw(graphics,CATEGORIES[index-1],x+1,y+1,14,13);
            return;
        }
        float scale = Math.min(0.75F, 14.0F / Math.max(1, font.width(label)));
        graphics.pose().pushPose();
        graphics.pose().translate(x + (16 - font.width(label) * scale) / 2, y + (15 - 9 * scale) / 2, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, label, 0, 0, selected ? 0xFF94FF75 : 0xFFE8EDF2, false);
        graphics.pose().popPose();
    }

    private int categoryButtonAt(double mouseX, double mouseY) {
        double x = mouseX - leftPos - stashX + 18;
        double y = mouseY - topPos - StashMenu.GRID_Y;
        if (x < 0 || x >= 16 || y < 0) return -1;
        int index = (int) y / 17;
        if (y - index * 17 >= 15 || index > (categoryButtonsOpen ? categoryRows() : 0)) return -1;
        return index==0?0:firstCategory+index;
    }
    private int categoryRows() { return Math.max(1,Math.min(CATEGORIES.length,(menu.visibleRows()*18-17)/17)); }

    private boolean clickCategoryButton(double mouseX, double mouseY) {
        int index = categoryButtonAt(mouseX, mouseY);
        if (index < 0) return false;
        if (index == 0) categoryButtonsOpen = !categoryButtonsOpen;
        else {
            String category = CATEGORIES[index - 1];
            highlightedCategory = highlightedCategory.equals(category) ? "" : category;
        }
        return true;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("container.minecrafteft.loadout"), StashMenu.PLAYER_X, 10, 0xFFE8EDF2, false);
        Component heading = menu.isCaseOnly() ? title : stashHeading;
        float headingScale = Math.min(1F, 88F / Math.max(1, font.width(heading)));
        graphics.pose().pushPose();
        graphics.pose().translate(stashX, 10, 0);
        graphics.pose().scale(headingScale, headingScale, 1);
        graphics.drawString(font, heading, 0, 0, 0xFFE8EDF2, false);
        graphics.pose().popPose();
        Component dimensions = Component.translatable("screen.minecrafteft.stash_rows", menu.rows(), menu.rows() * 10);
        graphics.drawString(font, dimensions, stashX, 25, 0xFF9EABB5, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        mouseX = toUiX(mouseX);
        mouseY = toUiY(mouseY);
        currentMouseX = mouseX;
        currentMouseY = mouseY;
        if(!insideCase(mouseX,mouseY) && categoryButtonsOpen && mouseX>=leftPos+stashX-18 && mouseX<leftPos+stashX-2
                && mouseY>=topPos+StashMenu.GRID_Y && mouseY<topPos+StashMenu.GRID_Y+(categoryRows()+1)*17) {
            firstCategory=Math.max(0,Math.min(CATEGORIES.length-categoryRows(),firstCategory-(int)Math.signum(delta)));
            return true;
        }
        if (insideCase(mouseX, mouseY)) {
            caseScroll = Math.max(0, Math.min(menu.openCaseRows() - caseVisibleRows(), caseScroll - (int) Math.signum(delta)));
            return true;
        }
        if(loadoutPanel.scroll(mouseX,mouseY,delta)) return true;
        int gridX = leftPos + stashX;
        int gridY = topPos + StashMenu.GRID_Y;
        int gridBottom = gridY + menu.visibleRows() * 18;
        if (((mouseX >= gridX && mouseX < gridX + GRID_WIDTH && mouseY >= gridY && mouseY < gridBottom)
                || stashScrollbar().contains(mouseX, mouseY))
                && menu.maxScrollRow() > 0 && delta != 0) {
            scrollStashTo(menu.scrollRow() - (int) Math.signum(delta));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        mouseX = toUiX(mouseX);
        mouseY = toUiY(mouseY);
        currentMouseX = mouseX;
        currentMouseY = mouseY;
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        mouseX = toUiX(mouseX);
        mouseY = toUiY(mouseY);
        currentMouseX = mouseX;
        currentMouseY = mouseY;
        if (clickCase(mouseX, mouseY, button)) return true;
        if(button==0 && loadoutPanel.beginDrag(mouseX,mouseY)) return true;
        if(button==0 && loadoutPanel.toggleAt(mouseX,mouseY)) { caseButton(2);consumePlacementRelease=true;return true; }
        Slot loadoutSlot=loadoutPanel.slotAt(mouseX,mouseY,menu.getCarried());
        if(loadoutSlot!=null && (button==0 || button==1)) {
            slotClicked(loadoutSlot,loadoutSlot.index,button,hasShiftDown()?ClickType.QUICK_MOVE:ClickType.PICKUP);
            consumePlacementRelease=true;return true;
        }
        if (button == 0 && stashScrollbar().contains(mouseX, mouseY)) {
            beginScrollbarDrag(stashScrollbar(), mouseY, false);
            return true;
        }
        if (button == 0 && clickCategoryButton(mouseX, mouseY)) return true;
        Slot footprintSlot = footprintSlotAt(mouseX, mouseY);
        if (button == 0 && !menu.isCaseOnly() && StashMenu.isTaczGun(menu.getCarried())
                && footprintSlot instanceof StashSlot && StashMenu.isWeaponCase(footprintSlot.getItem())) {
            slotClicked(footprintSlot, menu.slots.indexOf(footprintSlot), button, ClickType.PICKUP);
            consumePlacementRelease = true;
            return true;
        }
        if (footprintSlot instanceof StashSlot stash
                && menu.isLockedStorageIndex(menu.storageIndexForViewSlot(stash.viewSlot()))) {
            caseNoticeUntil = net.minecraft.Util.getMillis() + 3500;
            consumePlacementRelease = true;
            return true;
        }
        if ((button == 0 || button == 1) && StashMenu.isGridItem(menu.getCarried()) && insideGrid(mouseX, mouseY)) {
            ItemStack carried = menu.getCarried();
            int w = StashMenu.footprintWidth(carried);
            int h = StashMenu.footprintHeight(carried);
            var target = StashDragPlacement.target(mouseX, mouseY, leftPos + stashX,
                    topPos + StashMenu.GRID_Y, w, h);
            if (canLand(target, carried)) {
                int index = target.viewIndex();
                slotClicked(menu.slots.get(index), index, button, ClickType.PICKUP);
            }
            consumePlacementRelease = true;
            return true;
        }
        if (footprintSlot != null && menu.getCarried().isEmpty() && (button == 0 || button == 1)) {
            slotClicked(footprintSlot, menu.slots.indexOf(footprintSlot), button,
                    hasShiftDown() ? ClickType.QUICK_MOVE : ClickType.PICKUP);
            consumePlacementRelease = true;
            return true;
        }
        if (footprintSlot != null) {
            mouseX = leftPos + footprintSlot.x + 8;
            mouseY = topPos + footprintSlot.y + 8;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if(loadoutPanel.drag(toUiX(mouseX),toUiY(mouseY))) return true;
        mouseX = toUiX(mouseX);
        mouseY = toUiY(mouseY);
        currentMouseX = mouseX;
        currentMouseY = mouseY;
        if (button == 0 && (draggingStashScrollbar || draggingCaseScrollbar)) {
            if (draggingCaseScrollbar) caseScroll = caseScrollbar().rowAt(mouseY, scrollbarGrab);
            else scrollStashTo(stashScrollbar().rowAt(mouseY, scrollbarGrab));
            return true;
        }
        if (draggingCase && button == 0) {
            caseX = (int) (mouseX - caseDragX);
            caseY = (int) (mouseY - caseDragY);
            clampCase();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX / uiScale, dragY / uiScale);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if(loadoutPanel.release()) return true;
        if (draggingStashScrollbar || draggingCaseScrollbar) {
            draggingStashScrollbar = draggingCaseScrollbar = false;
            return true;
        }
        if (draggingCase) { draggingCase = false; return true; }
        if (consumePlacementRelease) {
            consumePlacementRelease = false;
            return true;
        }
        mouseX = toUiX(mouseX);
        mouseY = toUiY(mouseY);
        Slot footprintSlot = footprintSlotAt(mouseX, mouseY);
        if (footprintSlot != null) {
            mouseX = leftPos + footprintSlot.x + 8;
            mouseY = topPos + footprintSlot.y + 8;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public Slot getSlotUnderMouse() {
        if (insideCase(currentMouseX, currentMouseY)) {
            int index = caseAnchorAt(currentMouseX, currentMouseY);
            return index < 0 ? null : menu.caseSlot(index);
        }
        Slot equipped=loadoutPanel.slotAt(currentMouseX,currentMouseY,menu.getCarried());
        if(equipped!=null) return equipped;
        Slot footprintSlot = footprintSlotAt(currentMouseX, currentMouseY);
        if (footprintSlot != null) return footprintSlot;
        return super.getSlotUnderMouse();
    }

    private Slot footprintSlotAt(double mouseX, double mouseY) {
        int gridX = leftPos + stashX;
        int gridY = topPos + StashMenu.GRID_Y;
        if (mouseX < gridX || mouseX >= gridX + GRID_WIDTH
                || mouseY < gridY || mouseY >= gridY + menu.visibleRows() * 18) return null;
        for (StashSlot anchor : menu.stashDisplaySlots()) {
            ItemStack stack = anchor.getItem();
            if (stack.isEmpty() || !StashMenu.hasExpandedFootprint(stack)) continue;
            int itemX = leftPos + anchor.x;
            int itemY = topPos + anchor.y;
            int itemWidth = StashMenu.footprintWidth(stack) * 18;
            int itemHeight = StashMenu.footprintHeight(stack) * 18;
            if (mouseX >= itemX && mouseX < itemX + itemWidth
                    && mouseY >= itemY && mouseY < itemY + itemHeight) return anchor;
        }
        return null;
    }

    private void renderStashPlacement(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack carried = menu.getCarried();
        if (!StashMenu.isGridItem(carried) || insideCase(mouseX, mouseY)) return;
        int gridX = leftPos + stashX;
        int gridY = topPos + StashMenu.GRID_Y;
        if (mouseX < gridX || mouseX >= gridX + GRID_WIDTH
                || mouseY < gridY || mouseY >= gridY + menu.visibleRows() * 18) return;
        int itemWidth = StashMenu.footprintWidth(carried);
        int itemHeight = StashMenu.footprintHeight(carried);
        var target = StashDragPlacement.target(mouseX, mouseY, gridX, gridY, itemWidth, itemHeight);
        boolean fits = canLand(target, carried);
        int itemX = gridX + target.column() * 18;
        int itemY = gridY + target.row() * 18;
        int itemRight = itemX + itemWidth * 18 - 1;
        int itemBottom = itemY + itemHeight * 18 - 1;
        int placementColor = fits ? 0xAA39FF65 : 0xAACD3737;
        Slot hovered = footprintSlotAt(mouseX, mouseY);
        if (!menu.isCaseOnly() && StashMenu.isTaczGun(carried)
                && hovered instanceof StashSlot stash && StashMenu.isWeaponCase(hovered.getItem())) {
            int index = menu.storageIndexForViewSlot(stash.viewSlot());
            placementColor = canInsertIntoCase(index, hovered.getItem(), carried) ? 0xAA378BFF : 0xAACD3737;
            // Highlight the entire target case, including rotated/partly visible cases.
            itemX = leftPos + hovered.x;
            itemY = topPos + hovered.y;
            itemRight = itemX + StashMenu.footprintWidth(hovered.getItem()) * 18 - 1;
            itemBottom = itemY + StashMenu.footprintHeight(hovered.getItem()) * 18 - 1;
        }
        enableGridScissor(graphics);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0, 0, 200);
            graphics.fill(itemX - 1, itemY - 1, itemRight, itemBottom, placementColor);
        } finally {
            graphics.pose().popPose();
            graphics.disableScissor();
        }
    }

    private boolean canInsertIntoCase(int index, ItemStack host, ItemStack gun) {
        boolean opened = menu.isOpenCaseStorageIndex(index);
        boolean changed = index != insertionCaseIndex || opened != insertionCaseOpen
                || !ItemStack.matches(host, insertionHost) || !ItemStack.matches(gun, insertionGun);
        if (!changed && opened) {
            changed = insertionContents.size() != menu.openCaseSlots();
            for (int i = 0; !changed && i < insertionContents.size(); i++)
                changed = !ItemStack.matches(menu.caseStack(i), insertionContents.get(i));
        }
        if (changed) {
            insertionFits = menu.canStoreInCase(index, gun);
            insertionCaseIndex = index;
            insertionCaseOpen = opened;
            insertionHost = host.copy();
            insertionGun = gun.copy();
            var snapshot = new java.util.ArrayList<ItemStack>();
            if (opened) for (int i = 0; i < menu.openCaseSlots(); i++) snapshot.add(menu.caseStack(i).copy());
            insertionContents = snapshot;
        }
        return insertionFits;
    }

    private boolean insideGrid(double mouseX, double mouseY) {
        return mouseX >= leftPos + stashX && mouseX < leftPos + stashX + GRID_WIDTH
                && mouseY >= topPos + StashMenu.GRID_Y
                && mouseY < topPos + StashMenu.GRID_Y + menu.visibleRows() * 18;
    }

    private boolean canLand(StashDragPlacement.Target target, ItemStack carried) {
        if (!target.inside(StashMenu.footprintWidth(carried), StashMenu.footprintHeight(carried), menu.visibleRows())) return false;
        int index = target.viewIndex();
        return menu.slots.get(index).getItem().isEmpty()
                && menu.canPlaceAt(menu.storageIndexForViewSlot(index), carried);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if ((key == GLFW.GLFW_KEY_ESCAPE || minecraft.options.keyInventory.matches(key, scanCode))
                && (menu.isCaseOpen() || caseCloseKeyDown)) {
            if (!caseCloseKeyDown) { caseCloseKeyDown = true; onClose(); }
            return true;
        }
        if(!insideCase(currentMouseX,currentMouseY)) {
            Slot equipped=loadoutPanel.slotAt(currentMouseX,currentMouseY,menu.getCarried());
            if(equipped!=null) hoveredSlot=equipped;
        }
        if (key == GLFW.GLFW_KEY_O) {
            if (!caseKeyDown) {
                if (insideCase(currentMouseX, currentMouseY)) caseButton(StashMenu.CLOSE_CASE_BUTTON);
                else {
                    Slot slot = footprintSlotAt(currentMouseX, currentMouseY);
                    if (!menu.isCaseOnly() && slot instanceof StashSlot stash && StashMenu.isOpenable(slot.getItem())) {
                        caseButton(StashMenu.OPEN_CASE_BUTTON + menu.storageIndexForViewSlot(stash.viewSlot()));
                        if (!casePositioned) {
                            caseX = leftPos + 30;
                            caseY = topPos + 30;
                            casePositioned = true;
                        }
                        caseScroll = 0;
                        clampCase();
                    }
                }
            }
            caseKeyDown = true;
            return true;
        }
        // Vanilla keyboard shortcuts use the hidden slot's offscreen coordinates.
        // Route them explicitly while a case window covers the underlying inventory.
        if (insideCase(currentMouseX, currentMouseY) && key != GLFW.GLFW_KEY_R
                && key != GLFW.GLFW_KEY_ESCAPE && !minecraft.options.keyInventory.matches(key, scanCode)) return true;
        if (key == GLFW.GLFW_KEY_R && StashMenu.isGridItem(menu.getCarried())) {
            if (!rotationKeyDown && minecraft != null && minecraft.player != null && minecraft.gameMode != null) {
                menu.clickMenuButton(minecraft.player, StashMenu.ROTATE_CARRIED_BUTTON);
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, StashMenu.ROTATE_CARRIED_BUTTON);
            }
            rotationKeyDown = true;
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int key, int scanCode, int modifiers) {
        if (caseCloseKeyDown && (key == GLFW.GLFW_KEY_ESCAPE || minecraft.options.keyInventory.matches(key, scanCode))) {
            caseCloseKeyDown = false;
            return true;
        }
        if (key == GLFW.GLFW_KEY_O) { caseKeyDown = false; return true; }
        if (key == GLFW.GLFW_KEY_R && rotationKeyDown) {
            rotationKeyDown = false;
            return true;
        }
        return super.keyReleased(key, scanCode, modifiers);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw expanded stash items before the tooltip so their larger sprite stays visible.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        InventoryFrame.draw(graphics,width,height);
        var placement=InventoryFrame.fit(width,height,imageWidth,imageHeight,leftPos,topPos);
        uiScale=placement.scale();uiOffsetX=placement.offsetX();uiOffsetY=placement.offsetY();
        int uiMouseX = (int) toUiX(mouseX);
        int uiMouseY = (int) toUiY(mouseY);
        currentMouseX = uiMouseX;
        currentMouseY = uiMouseY;
        graphics.pose().pushPose();
        graphics.pose().translate(uiOffsetX, uiOffsetY, 0);
        graphics.pose().scale((float) uiScale, (float) uiScale, 1.0F);
        ItemStack carried = menu.getCarried();
        for (StashSlot slot : menu.stashDisplaySlots()) slot.setSuppressItemRender(true);
        boolean hoveringCase = insideCase(uiMouseX, uiMouseY);
        boolean hoveringLoadout=loadoutPanel.inside(uiMouseX,uiMouseY) && !hoveringCase;
        // A single cursor renderer retains the 3D model across grids, equipment and pane borders.
        menu.setCarried(ItemStack.EMPTY);
        try {
            super.render(graphics, uiMouseX, uiMouseY, partialTick);
        } finally {
            menu.setCarried(carried);
            for (StashSlot slot : menu.stashDisplaySlots()) slot.setSuppressItemRender(false);
        }
        renderExpandedItems(graphics);
        loadoutPanel.draw(graphics,leftPos+4,topPos+2,uiMouseX,uiMouseY,uiScale,uiOffsetX,uiOffsetY,carried);
        renderStashPlacement(graphics, uiMouseX, uiMouseY);
        renderCase(graphics, uiMouseX, uiMouseY);
        renderCaseNotice(graphics);
        renderRotationHint(graphics, carried);
        if (!carried.isEmpty()) {
            graphics.flush();
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, CASE_LAYER + 500);
            InventoryItemPreview.carried(graphics, carried, uiMouseX, uiMouseY);
            graphics.flush();
            graphics.pose().popPose();
        }
        // Tooltips use actual screen coordinates, outside the scaled stash pose.
        graphics.pose().popPose();
        if(hoveringLoadout) hoveredSlot=loadoutPanel.slotAt(uiMouseX,uiMouseY,carried);
        if (hoveringCase) {
            int index = caseAnchorAt(uiMouseX, uiMouseY);
            if (carried.isEmpty() && index >= 0) renderCaseTooltip(graphics, index, mouseX, mouseY);
            return;
        }
        int categoryButton = categoryButtonAt(uiMouseX, uiMouseY);
        if (categoryButton >= 0) {
            Component label = Component.translatable(categoryButton == 0 ? "screen.minecrafteft.categories"
                    : "screen.minecrafteft.category." + CATEGORIES[categoryButton - 1]);
            InventoryTooltipLayer.draw(graphics, () -> graphics.renderTooltip(font, label, mouseX, mouseY));
            return;
        }
        Slot footprint = footprintSlotAt(uiMouseX, uiMouseY);
        if (menu.getCarried().isEmpty() && footprint != null && footprint.hasItem()) {
            renderFootprintTooltip(graphics, footprint, mouseX, mouseY);
        } else if (menu.getCarried().isEmpty() && hoveredSlot != null && hoveredSlot.hasItem()) {
            InventoryTooltipLayer.draw(graphics, () -> graphics.renderTooltip(font, getTooltipFromContainerItem(hoveredSlot.getItem()),
                    hoveredSlot.getItem().getTooltipImage(), hoveredSlot.getItem(), mouseX, mouseY));
        }
    }

    @Override public void onClose() {
        if (menu.isCaseOpen()) {
            caseButton(StashMenu.CLOSE_CASE_BUTTON);
            draggingCase = draggingCaseScrollbar = false;
            consumePlacementRelease = false;
            return;
        }
        super.onClose();
    }

    private void renderRotationHint(GuiGraphics graphics, ItemStack carried) {
        if (!StashMenu.isGridItem(carried)) return;
        Component hint = Component.translatable("screen.minecrafteft.rotate_hint");
        float scale = Math.min(.85F, 88F / Math.max(1, font.width(hint)));
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos + stashX + 92, topPos + 11, 2500);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, hint, 0, 0, 0xFF94FF75, false);
        graphics.flush();
        graphics.pose().popPose();
    }

    private void renderCaseNotice(GuiGraphics graphics) {
        if (net.minecraft.Util.getMillis() >= caseNoticeUntil) return;
        Component notice = Component.translatable("screen.minecrafteft.close_case_first");
        float scale = Math.min(.85F, 132F / Math.max(1, font.width(notice)));
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos + stashX - 28 - font.width(notice) * scale, topPos + 11, 2500);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, notice, 0, 0, 0xFFFFCF6E, false);
        graphics.flush();
        graphics.pose().popPose();
    }

    private boolean insideCase(double x, double y) {
        return menu.isCaseOpen() && x >= caseX && x < caseX + caseWidth()
                && y >= caseY && y < caseY + caseHeight();
    }

    private boolean insideCaseGrid(double x, double y) {
        return insideCase(x, y) && x >= caseX + 5 && x < caseX + 5 + menu.openColumns() * 18
                && y >= caseY + 24 && y < caseY + 24 + caseVisibleRows() * 18;
    }

    private void clampCase() {
        int minX = (int) Math.ceil(toUiX(4)), minY = (int) Math.ceil(toUiY(4));
        int maxX = (int) Math.floor(toUiX(width - 4)) - caseWidth();
        int maxY = (int) Math.floor(toUiY(height - 4)) - caseHeight();
        caseX = Math.max(minX, Math.min(Math.max(minX, maxX), caseX));
        caseY = Math.max(minY, Math.min(Math.max(minY, maxY), caseY));
    }

    private void caseButton(int button) {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) return;
        menu.clickMenuButton(minecraft.player, button);
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
    }

    private int caseAnchorAt(double mouseX, double mouseY) {
        if (!insideCaseGrid(mouseX, mouseY)) return -1;
        int x = (int) (mouseX - caseX - 5) / 18;
        int y = (int) (mouseY - caseY - 24) / 18 + caseScroll;
        for (int i = 0; i < menu.openCaseSlots(); i++) {
            ItemStack stack = menu.caseStack(i);
            if (!stack.isEmpty() && x >= i % menu.openColumns() && x < i % menu.openColumns() + StashMenu.footprintWidth(stack)
                    && y >= i / menu.openColumns() && y < i / menu.openColumns() + StashMenu.footprintHeight(stack)) return i;
        }
        return -1;
    }

    private StashDragPlacement.Target caseTarget(double x, double y, ItemStack stack) {
        return StashDragPlacement.target(x, y, caseX + 5, caseY + 24,
                StashMenu.footprintWidth(stack), StashMenu.footprintHeight(stack));
    }

    private boolean canLandInCase(StashDragPlacement.Target target, ItemStack stack) {
        if (!menu.acceptsPopupItem(stack) || target.column() < 0
                || target.column() + StashMenu.footprintWidth(stack) > menu.openColumns()
                || target.row() + caseScroll < 0
                || target.row() + caseScroll + StashMenu.footprintHeight(stack) > menu.openCaseRows()) return false;
        int index = (target.row() + caseScroll) * menu.openColumns() + target.column();
        ItemStack existing = menu.caseStack(index);
        boolean room = existing.isEmpty() || (ItemStack.isSameItemSameTags(existing, stack)
                && existing.getCount() < existing.getMaxStackSize());
        return room && menu.canPlaceInCase(index, stack);
    }

    private boolean clickCase(double x, double y, int button) {
        if (!insideCase(x, y)) return false;
        if (button == 0 && caseScrollbar().contains(x, y)) {
            beginScrollbarDrag(caseScrollbar(), y, true);
            return true;
        }
        if (button == 0 && y < caseY + 20) {
            if (x >= caseX + caseWidth() - 18) caseButton(StashMenu.CLOSE_CASE_BUTTON);
            else { draggingCase = true; caseDragX = x - caseX; caseDragY = y - caseY; }
            return true;
        }
        if ((button == 0 || button == 1) && insideCaseGrid(x, y)) {
            ItemStack carried = menu.getCarried();
            if (carried.isEmpty()) {
                int index = caseAnchorAt(x, y);
                if (index >= 0) {
                    Slot slot = menu.caseSlot(index);
                    slotClicked(slot, slot.index, button, hasShiftDown() ? ClickType.QUICK_MOVE : ClickType.PICKUP);
                }
            } else {
                var target = caseTarget(x, y, carried);
                if (canLandInCase(target, carried)) {
                    Slot slot = menu.caseSlot((target.row() + caseScroll) * menu.openColumns() + target.column());
                    slotClicked(slot, slot.index, button, ClickType.PICKUP);
                }
            }
        }
        consumePlacementRelease = true;
        return true;
    }

    private void caseScissor(GuiGraphics graphics) {
        graphics.enableScissor((int) Math.ceil(uiOffsetX + (caseX + 4) * uiScale),
                (int) Math.ceil(uiOffsetY + (caseY + 23) * uiScale),
                (int) Math.floor(uiOffsetX + (caseX + 4 + menu.openColumns() * 18) * uiScale),
                (int) Math.floor(uiOffsetY + (caseY + 23 + caseVisibleRows() * 18) * uiScale));
    }

    private void renderCase(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!menu.isCaseOpen()) { draggingCase = draggingCaseScrollbar = false; return; }
        clampCase();
        // Flush the underlying item batches before drawing an opaque foreground window.
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, CASE_LAYER);
        graphics.fill(caseX + 3, caseY + 3, caseX + caseWidth() + 3, caseY + caseHeight() + 3, 0x99000000);
        graphics.fill(caseX, caseY, caseX + caseWidth(), caseY + caseHeight(), 0xFF929DA5);
        graphics.fill(caseX + 1, caseY + 1, caseX + caseWidth() - 1, caseY + caseHeight() - 1, 0xFF111820);
        graphics.fill(caseX + 1, caseY + 1, caseX + caseWidth() - 1, caseY + 19, 0xFF35434E);
        Component name = menu.isBackpackOpen() ? Component.literal("LBT-8005A") : Component.translatable("block.minecrafteft." + menu.openCaseKind().id());
        float nameScale = Math.min(1F, (caseWidth() - (menu.isBackpackOpen() ? 48F : 70F)) / Math.max(1, font.width(name)));
        graphics.pose().pushPose();
        graphics.pose().translate(caseX + 5, caseY + 6, 0);
        graphics.pose().scale(nameScale, nameScale, 1);
        graphics.drawString(font, name, 0, 0, 0xFFE8EDF2, false);
        graphics.pose().popPose();
        graphics.drawString(font, menu.openColumns() + "×" + menu.openCaseRows(), caseX + caseWidth() - (menu.isBackpackOpen() ? 42 : 65), caseY + 6, 0xFFCEB46E, false);
        boolean closeHover = mouseX >= caseX + caseWidth() - 18 && mouseY < caseY + 20 && insideCase(mouseX, mouseY);
        graphics.drawString(font, "×", caseX + caseWidth() - 13, caseY + 5, closeHover ? 0xFFFF7878 : 0xFFE8EDF2, false);
        for (int row = 0; row < caseVisibleRows(); row++) for (int column = 0; column < menu.openColumns(); column++) {
            int x = caseX + 5 + column * 18, y = caseY + 24 + row * 18;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF69737B);
            graphics.fill(x, y, x + 16, y + 16, 0xFF252D34);
        }
        caseScissor(graphics);
        try {
            for (int i = 0; i < menu.openCaseSlots(); i++) {
                ItemStack stack = menu.caseStack(i);
                if (stack.isEmpty()) continue;
                int w = StashMenu.footprintWidth(stack), h = StashMenu.footprintHeight(stack);
                int row = i / menu.openColumns() - caseScroll;
                if (row >= caseVisibleRows() || row + h <= 0) continue;
                int x = caseX + 5 + i % menu.openColumns() * 18, y = caseY + 24 + row * 18;
                int border = !highlightedCategory.isEmpty() && highlightedCategory.equals(StashMenu.gunCategory(stack))
                        ? 0xFF64FF35 : 0xFF929DA5;
                graphics.fill(x - 1, y - 1, x + w * 18 - 1, y + h * 18 - 1, border);
                graphics.fill(x, y, x + w * 18 - 2, y + h * 18 - 2, 0xFF252D34);
                renderFootprintPreview(graphics, stack, x + 1, y + 1, w * 18 - 4, h * 18 - 4);
                if (stack.getCount() > 1) graphics.renderItemDecorations(font, stack, x, y);
            }
            ItemStack carried = menu.getCarried();
            if (!carried.isEmpty() && insideCaseGrid(mouseX, mouseY)) {
                int w = StashMenu.footprintWidth(carried), h = StashMenu.footprintHeight(carried);
                var target = caseTarget(mouseX, mouseY, carried);
                int x = caseX + 5 + target.column() * 18, y = caseY + 24 + target.row() * 18;
                graphics.fill(x - 1, y - 1, x + w * 18 - 1, y + h * 18 - 1,
                        canLandInCase(target, carried) ? 0xAA39FF65 : 0xAACD3737);
            }
        } finally { graphics.disableScissor(); }
        if (menu.openCaseRows() > caseVisibleRows()) drawScrollbar(graphics, caseScrollbar(), 0xFFCEB46E);
        graphics.flush();
        graphics.pose().popPose();
    }

    private void renderCaseTooltip(GuiGraphics graphics, int index, int mouseX, int mouseY) {
        ItemStack stack = menu.caseStack(index);
        int x = caseX + 5 + index % menu.openColumns() * 18, y = caseY + 24 + (index / menu.openColumns() - caseScroll) * 18;
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, CASE_LAYER);
        StashItemTooltip.render(graphics, font, stack, getTooltipFromContainerItem(stack), width, height,
                (int) (uiOffsetX + x * uiScale), (int) (uiOffsetY + Math.max(y, caseY + 24) * uiScale),
                (int) (uiOffsetX + (x + StashMenu.footprintWidth(stack) * 18) * uiScale),
                (int) (uiOffsetY + Math.min(y + StashMenu.footprintHeight(stack) * 18, caseY + 24 + caseVisibleRows() * 18) * uiScale), mouseX, mouseY);
        graphics.pose().popPose();
    }

    private double toUiX(double x) {
        return (x - uiOffsetX) / uiScale;
    }
    @Override protected boolean hasClickedOutside(double x,double y,int left,int top,int button) {
        return !InventoryFrame.bounds(width,height).contains(uiOffsetX+x*uiScale,uiOffsetY+y*uiScale);
    }

    private double toUiY(double y) {
        return (y - uiOffsetY) / uiScale;
    }

    private void enableGridScissor(GuiGraphics graphics) {
        // GuiGraphics scissors ignore PoseStack transforms in 1.20.1.
        int x = leftPos + stashX;
        int y = topPos + StashMenu.GRID_Y;
        // Include the outer top/left border, otherwise the original cell dividers remain exposed.
        graphics.enableScissor((int) Math.floor(uiOffsetX + (x - 1) * uiScale),
                (int) Math.floor(uiOffsetY + (y - 1) * uiScale),
                (int) Math.floor(uiOffsetX + (x + GRID_WIDTH - 1) * uiScale),
                (int) Math.floor(uiOffsetY + (y + menu.visibleRows() * 18 - 1) * uiScale));
    }

    private void renderFootprintTooltip(GuiGraphics graphics, Slot slot, int mouseX, int mouseY) {
        ItemStack stack = slot.getItem();
        int x = leftPos + slot.x;
        int y = topPos + slot.y;
        int right = Math.min(x + StashMenu.footprintWidth(stack) * 18, leftPos + stashX + GRID_WIDTH);
        int bottom = Math.min(y + StashMenu.footprintHeight(stack) * 18,
                topPos + StashMenu.GRID_Y + menu.visibleRows() * 18);
        StashItemTooltip.render(graphics, font, stack, getTooltipFromContainerItem(stack), width, height,
                (int) (uiOffsetX + x * uiScale),
                (int) (uiOffsetY + Math.max(y, topPos + StashMenu.GRID_Y) * uiScale),
                (int) (uiOffsetX + right * uiScale), (int) (uiOffsetY + bottom * uiScale), mouseX, mouseY);
    }

    private void renderExpandedItems(GuiGraphics graphics) {
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 100);
        enableGridScissor(graphics);
        try {
            for (StashSlot slot : menu.stashDisplaySlots()) {
                ItemStack stack = menu.stashStackAtStorageIndex(menu.storageIndexForViewSlot(slot.viewSlot()));
                if (!intersectsGrid(slot, stack)) continue;
                if (stack.isEmpty()) continue;
                int footprintWidth = StashMenu.footprintWidth(stack);
                int footprintHeight = StashMenu.footprintHeight(stack);
                int itemX = leftPos + slot.x;
                int itemY = topPos + slot.y;
                if (!StashMenu.hasExpandedFootprint(stack)) {
                    graphics.renderItem(stack, itemX, itemY);
                    graphics.renderItemDecorations(font, stack, itemX, itemY);
                    continue;
                }
                int itemWidth = footprintWidth * 18 - 1;
                int itemHeight = footprintHeight * 18 - 1;
                int border = !highlightedCategory.isEmpty() && highlightedCategory.equals(StashMenu.gunCategory(stack))
                        ? 0xFF64FF35 : 0xFF929DA5;
                graphics.fill(itemX - 1, itemY - 1, itemX + itemWidth, itemY + itemHeight, border);
                graphics.fill(itemX, itemY, itemX + itemWidth - 1, itemY + itemHeight - 1, 0xFF252D34);
                renderFootprintPreview(graphics, stack, itemX + 1, itemY + 1,
                        itemWidth - 2, itemHeight - 2, menu.isOpenCaseStorageIndex(menu.storageIndexForViewSlot(slot.viewSlot())));
                graphics.renderItemDecorations(font, stack,
                        itemX + (footprintWidth - 1) * 18, itemY + (footprintHeight - 1) * 18);
            }
        } finally {
            graphics.flush();
            graphics.disableScissor();
            graphics.pose().popPose();
        }
    }

    private boolean intersectsGrid(Slot slot, ItemStack stack) {
        return !stack.isEmpty() && slot.y < StashMenu.GRID_Y + menu.visibleRows() * 18
                && slot.y + StashMenu.footprintHeight(stack) * 18 > StashMenu.GRID_Y;
    }

    private void renderFootprintPreview(GuiGraphics graphics, ItemStack stack, int x, int y, int pixelWidth, int pixelHeight) {
        renderFootprintPreview(graphics,stack,x,y,pixelWidth,pixelHeight,false);
    }

    private void renderFootprintPreview(GuiGraphics graphics, ItemStack stack, int x, int y, int pixelWidth, int pixelHeight, boolean caseOpen) {
        InventoryItemPreview.footprint(graphics, stack, x, y, pixelWidth, pixelHeight, caseOpen);
    }

}
