package com.minecrafteft.client;

import com.minecrafteft.inventory.LoadoutState;
import com.minecrafteft.inventory.StorageEquipment;
import com.minecrafteft.network.CreativeBackpackPacket;
import com.minecrafteft.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

final class CreativeBackpackSlot extends AbstractWidget {
    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath("minecrafteft","textures/gui/categories/backpacks.png");
    private final CreativeModeInventoryScreen screen;

    CreativeBackpackSlot(CreativeModeInventoryScreen screen) {
        super(screen.getGuiLeft()+127, screen.getGuiTop()+18, 18, 18, Component.translatable("loadout.minecrafteft.backpack"));
        this.screen = screen;
        visible = false;
    }
    private ItemStack equipped() {
        return LoadoutState.of(Minecraft.getInstance().player).equipment.getStackInSlot(LoadoutState.BACKPACK);
    }
    @Override protected void renderWidget(GuiGraphics g, int mx, int my, float partialTick) {
        int x = getX(), y = getY();
        g.fill(x,y,x+18,y+18,0xFFFFFFFF);
        g.fill(x,y,x+17,y+17,0xFF373737);
        g.fill(x+1,y+1,x+17,y+17,0xFF8B8B8B);
        ItemStack bag = equipped();
        if (bag.isEmpty()) {
            g.setColor(.45F,.45F,.45F,1);
            CategoryIconRenderer.draw(g,"backpacks",x+2,y+2,14,14);
            g.setColor(1,1,1,1);
        } else g.renderItem(bag,x+1,y+1);
        if (isMouseOver(mx,my)) g.fill(x+1,y+1,x+17,y+17,0x55FFFFFF);
    }
    void drawTooltip(GuiGraphics g, int mx, int my) {
        if (!isMouseOver(mx,my) || !screen.getMenu().getCarried().isEmpty()) return;
        var font = Minecraft.getInstance().font;
        if (equipped().isEmpty()) g.renderTooltip(font,getMessage(),mx,my);
        else g.renderTooltip(font,equipped(),mx,my);
    }
    @Override protected boolean isValidClickButton(int button) { return button == 0 || button == 1; }
    @Override public void onClick(double mx, double my) {
        ItemStack cursor = screen.getMenu().getCarried();
        if (!cursor.isEmpty() && (cursor.getCount() != 1 || !StorageEquipment.accepts(cursor,"backpack"))) return;
        ItemStack previous = equipped();
        var state = LoadoutState.of(Minecraft.getInstance().player);
        state.equipment.setStackInSlot(LoadoutState.BACKPACK,cursor.copy());
        screen.getMenu().setCarried(previous.copy());
        ModNetwork.CHANNEL.sendToServer(new CreativeBackpackPacket(cursor));
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
}
