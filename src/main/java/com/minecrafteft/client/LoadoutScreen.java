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
    private final ProfileView profile=new ProfileView();
    private static final String[] TABS={"overview","equipment","health","skills","quests","achievements"};
    private int selected=1;
    private double scale=1,ox,oy;
    private int mx,my;
    private boolean consumed;
    public LoadoutScreen(LoadoutMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);panel=new LoadoutPanel(menu.loadout);imageWidth=616;imageHeight=282;
    }
    private String switchLabel() { return Component.translatable("loadout.minecrafteft.vanilla").getString(); }
    private int switchWidth() { return (int)Math.ceil(font.width(switchLabel()) * .8) + 16; }
    private boolean overSwitch(double x,double y) {
        return x>=leftPos+10 && x<leftPos+10+switchWidth() && y>=topPos+6 && y<topPos+22;
    }
    private double ux(double x) { return (x-ox)/scale; }
    private double uy(double y) { return (y-oy)/scale; }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY) { }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY) { }
    @Override protected void renderTooltip(GuiGraphics g,int mouseX,int mouseY) { }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
        renderBackground(g);InventoryFrame.draw(g,width,height);
        var placement=InventoryFrame.fit(width,height,imageWidth,imageHeight,leftPos,topPos);
        scale=placement.scale();ox=placement.offsetX();oy=placement.offsetY();mx=(int)ux(mouseX);my=(int)uy(mouseY);
        ItemStack carried=menu.getCarried();menu.setCarried(ItemStack.EMPTY);
        g.pose().pushPose();g.pose().translate(ox,oy,0);g.pose().scale((float)scale,(float)scale,1);
        try { if(inventoryTab()) super.render(g,mx,my,partial); } finally { menu.setCarried(carried); }
        if(inventoryTab()) { panel.renewed(selected==2);panel.draw(g,leftPos+4,topPos+2,mx,my,scale,ox,oy,carried); }
        else if(selected==0) profile.draw(g,leftPos+4,topPos+2,mx,my,scale,ox,oy);
        else {
            String developing=Component.translatable("menu.minecrafteft.developing").getString();
            LoadoutPanel.small(g,developing,leftPos+imageWidth/2-font.width(developing)/2,topPos+imageHeight/2,1,0xFFCCD8E3);
        }
        if(inventoryTab() && minecraft.player.isCreative()) {
            LoadoutPanel.box(g,leftPos+10,topPos+6,switchWidth(),16);
            if (overSwitch(mx,my)) g.fill(leftPos+11,topPos+7,leftPos+9+switchWidth(),topPos+21,0xFF394B59);
            LoadoutPanel.small(g,switchLabel(),leftPos+18,topPos+10,.8F,0xFFE4EDF1);
        } else if(selected<=2) LoadoutPanel.small(g,Component.translatable("menu.minecrafteft."+TABS[selected]).getString(),leftPos+12,topPos+10,.85F,0xFFE5EDF3);
        if(!carried.isEmpty()) { g.pose().pushPose();g.pose().translate(0,0,1500);InventoryItemPreview.carried(g,carried,mx,my);g.flush();g.pose().popPose(); }
        g.pose().popPose();drawTabs(g,mouseX,mouseY);hoveredSlot=inventoryTab()?panel.slotAt(mx,my,carried):null;
        if(carried.isEmpty() && hoveredSlot!=null && hoveredSlot.hasItem()) InventoryTooltipLayer.draw(g,()->g.renderTooltip(font,getTooltipFromContainerItem(hoveredSlot.getItem()),hoveredSlot.getItem().getTooltipImage(),hoveredSlot.getItem(),mouseX,mouseY));
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        int tab=tabAt(x,y);
        if(button==0 && tab>=0) { selected=tab;panel.release();panel.clearHits();profile.release();hoveredSlot=null;return true; }
        double px=ux(x),py=uy(y);
        if(button==0 && inventoryTab() && minecraft.player.isCreative() && overSwitch(px,py)) { LoadoutClient.vanilla();return true; }
        if(!inventoryTab()) {
            if(selected==0 && button==0 && profile.click(px,py)) return true;
            if(InventoryFrame.bounds(width,height).contains(x,y)) return true;
            return super.mouseClicked(px,py,button);
        }
        if(button==0 && panel.beginDrag(px,py)) return true;
        if(button==0 && panel.toggleAt(px,py)) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);return true; }
        Slot slot=panel.slotAt(px,py,menu.getCarried());
        if(slot!=null && (button==0 || button==1)) { slotClicked(slot,slot.index,button,hasShiftDown()?ClickType.QUICK_MOVE:ClickType.PICKUP);consumed=true;return true; }
        return super.mouseClicked(px,py,button);
    }
    @Override protected boolean hasClickedOutside(double x,double y,int left,int top,int button) {
        return !InventoryFrame.bounds(width,height).contains(ox+x*scale,oy+y*scale);
    }
    @Override public boolean mouseReleased(double x,double y,int button) { profile.release();if(!inventoryTab()) return true;if(panel.release() || consumed) { consumed=false;return true; }return super.mouseReleased(ux(x),uy(y),button); }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy) {
        if(selected==0) return button==0 && profile.drag(dx/scale);
        return inventoryTab() && (panel.drag(ux(x),uy(y)) || super.mouseDragged(ux(x),uy(y),button,dx/scale,dy/scale));
    }
    @Override public boolean mouseScrolled(double x,double y,double delta) { return selected==0?profile.scroll(ux(x),uy(y),delta):inventoryTab() && panel.scroll(ux(x),uy(y),delta); }
    @Override public Slot getSlotUnderMouse() { return inventoryTab()?panel.slotAt(mx,my,menu.getCarried()):null; }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        hoveredSlot=getSlotUnderMouse();
        if(inventoryTab() && key==GLFW.GLFW_KEY_R && com.minecrafteft.stash.StashMenu.isGridItem(menu.getCarried())) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1);return true; }
        return super.keyPressed(key,scan,modifiers);
    }
    private boolean inventoryTab() { return selected==1 || selected==2; }
    private int tabWidth() { return Math.max(1,Math.min(InventoryFrame.bounds(width,height).width()/6,Math.max(48,InventoryFrame.bounds(width,height).width()*55/600))); }
    private int tabAt(double x,double y) {
        var frame=InventoryFrame.bounds(width,height);
        if(y<frame.top()-17 || y>=frame.top()-3 || x<frame.left() || x>=frame.left()+6*tabWidth()) return -1;
        return (int)(x-frame.left())/tabWidth();
    }
    private void drawTabs(GuiGraphics g,int mx,int my) {
        var frame=InventoryFrame.bounds(width,height);int size=tabWidth(),hovered=tabAt(mx,my);
        String[][] icons={
                {"0011100","0100010","1001001","1000001","1001001","1001001","0111110"},
                {"1100011","1111111","0111110","0111110","0111110","0111110","0111110"},
                {"0011100","0011100","1111111","1111111","1111111","0011100","0011100"},
                {"0001000","1001001","0111110","0011100","0111110","0100010","1000001"},
                {"1111100","1001010","1001111","1010101","1000001","1011101","1111111"},
                {"1111111","1011101","1011101","0111110","0011100","0001000","0011100"}};
        g.pose().pushPose();g.pose().translate(0,0,1800);
        for(int i=0;i<6;i++) {
            int left=frame.left()+i*size,top=frame.top()-17;boolean active=i==selected;
            for(int row=0;row<14;row++) {
                int edge=Math.max(0,(row-7)/2),shade=active?235-row*3:(i==hovered?74:49)-row;
                int color=active?0xFF000000|shade<<16|(shade+4)<<8|Math.min(255,shade+8):0xFF000000|shade<<16|(shade+12)<<8|(shade+21);
                g.fill(left,top+row,left+size-1-edge,top+row+1,color);
            }
            int foreground=active?0xFF192B38:0xFFD3E1EA;
            for(int row=0;row<7;row++) for(int col=0;col<7;col++) if(icons[i][row].charAt(col)=='1') g.fill(left+4+col,top+3+row,left+5+col,top+4+row,foreground);
            String label=Component.translatable("menu.minecrafteft."+TABS[i]).getString();
            float labelScale=Math.min(.8F,(size-17F)/Math.max(1,font.width(label)));
            LoadoutPanel.small(g,label,left+14,top+4,labelScale,foreground);
        }
        g.pose().popPose();
    }
}
