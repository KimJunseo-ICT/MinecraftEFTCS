package com.minecrafteft.client;

import com.minecrafteft.inventory.LoadoutMenu;
import com.minecrafteft.network.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class LoadoutScreen extends AbstractContainerScreen<LoadoutMenu> {
    private final LoadoutPanel panel;
    private double scale=1,ox,oy;
    private int mx,my;
    private boolean consumed;
    public LoadoutScreen(LoadoutMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);panel=new LoadoutPanel(menu.loadout);imageWidth=LoadoutPanel.WIDTH+8;imageHeight=LoadoutPanel.HEIGHT+6;
    }
    private String switchLabel() { return Component.translatable("loadout.minecrafteft.vanilla").getString(); }
    private int switchWidth() { return (int)Math.ceil(font.width(switchLabel()) * .8) + 16; }
    private boolean overSwitch(double x,double y) {
        return x>=leftPos+10 && x<leftPos+10+switchWidth() && y>=topPos+6 && y<topPos+22;
    }
    private double ux(double x) { return (x-ox)/scale; }
    private double uy(double y) { return (y-oy)/scale; }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY) { LoadoutPanel.box(g,leftPos,topPos,imageWidth,imageHeight); }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY) { }
    @Override protected void renderTooltip(GuiGraphics g,int mouseX,int mouseY) { }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
        renderBackground(g);scale=Math.max(.1,Math.min(1,Math.min((width-12D)/imageWidth,(height-12D)/imageHeight)));
        ox=width*(1-scale)/2;oy=height*(1-scale)/2;mx=(int)ux(mouseX);my=(int)uy(mouseY);
        ItemStack carried=menu.getCarried();menu.setCarried(ItemStack.EMPTY);
        g.pose().pushPose();g.pose().translate(ox,oy,0);g.pose().scale((float)scale,(float)scale,1);
        try { super.render(g,mx,my,partial); } finally { menu.setCarried(carried); }
        panel.draw(g,leftPos+4,topPos+2,mx,my,scale,ox,oy,carried);
        if(minecraft.player.isCreative()) {
            LoadoutPanel.box(g,leftPos+10,topPos+6,switchWidth(),16);
            if (overSwitch(mx,my)) g.fill(leftPos+11,topPos+7,leftPos+9+switchWidth(),topPos+21,0xFF394B59);
            LoadoutPanel.small(g,switchLabel(),leftPos+18,topPos+10,.8F,0xFFE4EDF1);
        } else LoadoutPanel.small(g,title.getString(),leftPos+12,topPos+10,.85F,0xFFE5EDF3);
        if(!carried.isEmpty()) { g.pose().pushPose();g.pose().translate(0,0,1500);InventoryItemPreview.carried(g,carried,mx,my);g.flush();g.pose().popPose(); }
        g.pose().popPose();hoveredSlot=panel.slotAt(mx,my,carried);
        if(carried.isEmpty() && hoveredSlot!=null && hoveredSlot.hasItem()) InventoryTooltipLayer.draw(g,()->g.renderTooltip(font,getTooltipFromContainerItem(hoveredSlot.getItem()),hoveredSlot.getItem().getTooltipImage(),hoveredSlot.getItem(),mouseX,mouseY));
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        double px=ux(x),py=uy(y);
        if(button==0 && minecraft.player.isCreative() && overSwitch(px,py)) { LoadoutClient.vanilla();return true; }
        if(button==0 && panel.beginDrag(px,py)) return true;
        if(button==0 && panel.toggleAt(px,py)) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);return true; }
        Slot slot=panel.slotAt(px,py,menu.getCarried());
        if(slot!=null && (button==0 || button==1)) { slotClicked(slot,slot.index,button,hasShiftDown()?ClickType.QUICK_MOVE:ClickType.PICKUP);consumed=true;return true; }
        return super.mouseClicked(px,py,button);
    }
    @Override public boolean mouseReleased(double x,double y,int button) { if(panel.release() || consumed) { consumed=false;return true; }return super.mouseReleased(ux(x),uy(y),button); }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy) { return panel.drag(ux(x),uy(y)) || super.mouseDragged(ux(x),uy(y),button,dx/scale,dy/scale); }
    @Override public boolean mouseScrolled(double x,double y,double delta) { return panel.scroll(ux(x),uy(y),delta); }
    @Override public Slot getSlotUnderMouse() { return panel.slotAt(mx,my,menu.getCarried()); }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        hoveredSlot=getSlotUnderMouse();
        if(key==GLFW.GLFW_KEY_R && com.minecrafteft.stash.StashMenu.isGridItem(menu.getCarried())) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1);return true; }
        return super.keyPressed(key,scan,modifiers);
    }
}
