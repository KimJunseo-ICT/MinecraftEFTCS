package com.minecrafteft.client;

import com.minecrafteft.inventory.DayPackItem;
import com.minecrafteft.inventory.PortableBackpackMenu;
import com.minecrafteft.stash.StashMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class PortableBackpackScreen extends AbstractContainerScreen<PortableBackpackMenu> {
    private final LoadoutPanel panel;
    private double scale=1,ox,oy;
    private int mx,my;
    private boolean consumed;
    public PortableBackpackScreen(PortableBackpackMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);panel=new LoadoutPanel(menu.loadout,true);imageWidth=608;imageHeight=288;
    }
    private double ux(double x) { return (x-ox)/scale; }
    private double uy(double y) { return (y-oy)/scale; }
    private int gx() { return leftPos+444; }
    private int gy() { return topPos+60; }
    private boolean inBag(double x,double y) { return x>=gx() && x<gx()+72 && y>=gy() && y<gy()+90; }
    private StashDragPlacement.Target target(double x,double y,ItemStack stack) {
        return StashDragPlacement.target(x,y,gx(),gy(),StashMenu.footprintWidth(stack),StashMenu.footprintHeight(stack));
    }
    private boolean fits(StashDragPlacement.Target target,ItemStack stack) {
        return target.inside(StashMenu.footprintWidth(stack),StashMenu.footprintHeight(stack),DayPackItem.WIDTH,DayPackItem.HEIGHT)
                && menu.contents.canPlace(target.index(DayPackItem.WIDTH),stack);
    }
    private Slot slotAt(double x,double y,ItemStack carried) {
        if(!inBag(x,y)) return panel.slotAt(x,y,carried);
        if(!carried.isEmpty()) { var target=target(x,y,carried);return fits(target,carried)?menu.bagSlots.get(target.index(DayPackItem.WIDTH)):null; }
        int cell=(int)(y-gy())/18*DayPackItem.WIDTH+(int)(x-gx())/18;
        int anchor=menu.contents.anchorAt(cell);return menu.bagSlots.get(anchor<0?cell:anchor);
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my) {
        LoadoutPanel.box(g,leftPos,topPos,imageWidth,imageHeight);
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y) { }
    @Override protected void renderTooltip(GuiGraphics g,int x,int y) { }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
        renderBackground(g);scale=Math.max(.1,Math.min(1,Math.min((width-16D)/imageWidth,(height-16D)/imageHeight)));
        ox=width*(1-scale)/2;oy=height*(1-scale)/2;mx=(int)ux(mouseX);my=(int)uy(mouseY);
        var carried=menu.getCarried();menu.setCarried(ItemStack.EMPTY);
        g.pose().pushPose();g.pose().translate(ox,oy,0);g.pose().scale((float)scale,(float)scale,1);
        try { super.render(g,mx,my,partial); } finally { menu.setCarried(carried); }
        panel.draw(g,leftPos+4,topPos+2,mx,my,scale,ox,oy,carried);
        LoadoutPanel.small(g,Component.translatable("container.minecrafteft.loadout").getString(),leftPos+16,topPos+10,.85F,0xFFE8EDF2);
        g.fill(leftPos+386,topPos+10,leftPos+387,topPos+280,0xFF46515A);
        LoadoutPanel.small(g,title.getString(),leftPos+408,topPos+10,Math.min(.85F,176F/Math.max(1,font.width(title))),0xFFE8EDF2);
        LoadoutPanel.small(g,"4 × 5",leftPos+408,topPos+26,.85F,0xFF9EABB5);
        for(int i=0;i<20;i++) LoadoutPanel.box(g,gx()+i%4*18-1,gy()+i/4*18-1,18,18);
        for(int i=0;i<20;i++) {
            var stack=menu.contents.getStackInSlot(i);if(stack.isEmpty()) continue;
            int x=gx()+i%4*18,y=gy()+i/4*18,w=StashMenu.footprintWidth(stack)*18-2,h=StashMenu.footprintHeight(stack)*18-2;
            LoadoutPanel.box(g,x-1,y-1,w+2,h+2);
            if(StashMenu.isGridItem(stack)) InventoryItemPreview.footprint(g,stack,x+1,y+1,w-2,h-2,false);
            else LoadoutPanel.stack(g,stack,x+1,y+1,w-2,h-2);
        }
        if(!carried.isEmpty() && inBag(mx,my)) {
            var target=target(mx,my,carried);int x=gx()+target.column()*18,y=gy()+target.row()*18;
            g.fill(x,y,x+StashMenu.footprintWidth(carried)*18-2,y+StashMenu.footprintHeight(carried)*18-2,fits(target,carried)?0x9939D873:0x99D14F4F);
        }
        if(StashMenu.isGridItem(carried)) LoadoutPanel.small(g,Component.translatable("screen.minecrafteft.rotate_hint").getString(),leftPos+408,topPos+170,.85F,0xFF94FF75);
        if(!carried.isEmpty()) {
            g.pose().pushPose();g.pose().translate(0,0,1500);InventoryItemPreview.carried(g,carried,mx,my);g.flush();g.pose().popPose();
        }
        g.pose().popPose();hoveredSlot=slotAt(mx,my,carried);
        if(carried.isEmpty() && hoveredSlot!=null && hoveredSlot.hasItem())
            InventoryTooltipLayer.draw(g,()->g.renderTooltip(font,getTooltipFromContainerItem(hoveredSlot.getItem()),hoveredSlot.getItem().getTooltipImage(),hoveredSlot.getItem(),mouseX,mouseY));
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        double px=ux(x),py=uy(y);
        if(button==0 && panel.beginDrag(px,py)) return true;
        if(button==0 && panel.toggleAt(px,py)) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);return true; }
        var slot=slotAt(px,py,menu.getCarried());
        if(slot!=null && (button==0 || button==1)) { slotClicked(slot,slot.index,button,hasShiftDown()?ClickType.QUICK_MOVE:ClickType.PICKUP);consumed=true;return true; }
        if(inBag(px,py)) return true;
        return super.mouseClicked(px,py,button);
    }
    @Override public Slot getSlotUnderMouse() { return slotAt(mx,my,menu.getCarried()); }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        hoveredSlot=getSlotUnderMouse();
        if(key==GLFW.GLFW_KEY_R && StashMenu.isGridItem(menu.getCarried())) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1);return true; }
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean mouseReleased(double x,double y,int button) { if(panel.release() || consumed) { consumed=false;return true; }return super.mouseReleased(ux(x),uy(y),button); }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy) { return panel.drag(ux(x),uy(y)) || super.mouseDragged(ux(x),uy(y),button,dx/scale,dy/scale); }
    @Override public boolean mouseScrolled(double x,double y,double delta) { return panel.scroll(ux(x),uy(y),delta); }
}
