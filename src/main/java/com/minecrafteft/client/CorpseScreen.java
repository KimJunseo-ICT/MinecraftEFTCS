package com.minecrafteft.client;

import com.minecrafteft.corpse.*;
import com.minecrafteft.inventory.*;
import com.minecrafteft.stash.StashMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import java.util.*;

public final class CorpseScreen extends AbstractContainerScreen<CorpseMenu> {
    private static final int RIGHT=388,PANE_W=224;
    private final LoadoutPanel personal;
    private final List<Hit> hits=new ArrayList<>();
    private final List<GridArea> grids=new ArrayList<>();
    private final Set<Slot> extraSlots=new LinkedHashSet<>();
    private record GridArea(EquipmentGrid grid,List<Slot> slots,int x,int y) { }
    private record Hit(Slot slot,int x,int y,int width,int height) {
        boolean contains(double px,double py) { return px>=x && px<x+width && py>=y && py<y+height; }
    }
    private double scale=1,ox,oy,grab;
    private int mx,my,scroll,limit,thumb;
    private boolean dragging;
    private boolean statusOpen;
    private int statusScroll;
    private final com.minecrafteft.status.StatusState deathStatus=new com.minecrafteft.status.StatusState();
    public CorpseScreen(CorpseMenu menu,Inventory inv,Component title) {
        super(menu,inv,title);personal=new LoadoutPanel(menu.loadout,true);
        if(menu.body!=null) deathStatus.restore(menu.body.appearance().getIntArray("StatusAtDeath"));
        imageWidth=RIGHT+PANE_W+8;imageHeight=284;
    }
    private int left() { return leftPos+RIGHT; }
    private int top() { return topPos+7; }
    private int heightOfPane() { return imageHeight-14; }
    private boolean inside(double x,double y) {
        return x>=left() && x<left()+PANE_W && y>=top() && y<top()+heightOfPane();
    }
    private double ux(double x) { return (x-ox)/scale; }
    private double uy(double y) { return (y-oy)/scale; }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY) {}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
        renderBackground(g);InventoryFrame.draw(g,width,height);
        var placement=InventoryFrame.fit(width,height,imageWidth,imageHeight,leftPos,topPos);
        scale=placement.scale();ox=placement.offsetX();oy=placement.offsetY();
        mx=(int)Math.floor(ux(mouseX));my=(int)Math.floor(uy(mouseY));
        g.pose().pushPose();g.pose().translate(ox,oy,0);g.pose().scale((float)scale,(float)scale,1);
        LoadoutPanel.small(g,Component.translatable("container.minecrafteft.loadout").getString(),leftPos+12,topPos+10,.85F,0xFFE5EDF3);
        personal.draw(g,leftPos+4,topPos+2,mx,my,scale,ox,oy,menu.getCarried());
        drawBody(g);
        if (!menu.getCarried().isEmpty()) {
            g.pose().pushPose();g.pose().translate(0,0,1500);
            InventoryItemPreview.carried(g,menu.getCarried(),mx,my);g.flush();g.pose().popPose();
        }
        g.pose().popPose();
        hoveredSlot=at(mx,my);
        if (menu.getCarried().isEmpty() && hoveredSlot!=null && hoveredSlot.hasItem()) {
            var item=hoveredSlot.getItem();
            InventoryTooltipLayer.draw(g,()->g.renderTooltip(font,getTooltipFromContainerItem(item),item.getTooltipImage(),item,mouseX,mouseY));
        }
    }
    private void drawBody(GuiGraphics g) {
        var loot=menu.loot;
        for (int i=0;i<menu.gear.size();i++) if ((i>=9 && i<=35 || i>=CorpseInventory.RECOVERY) && menu.gear.get(i).hasItem()) extraSlots.add(menu.gear.get(i));
        var extras=new ArrayList<>(extraSlots);
        int bagY=334,pouchY=bagY+Math.max(38,loot.bag.size().height()*18)+8;
        int extraY=pouchY+Math.max(38,loot.pouch.size().height()*18)+8;
        int content=extraY+(extras.isEmpty()?0:13+(extras.size()+9)/10*18);
        limit=Math.max(0,content-heightOfPane()+4);
        scroll=Mth.clamp(scroll,0,limit);
        int x=left()+3,y=top()+2-scroll;
        hits.clear();grids.clear();LoadoutPanel.box(g,left(),top(),PANE_W,heightOfPane());
        g.enableScissor((int)Math.ceil(ox+(left()+1)*scale),(int)Math.ceil(oy+(top()+1)*scale),
                (int)Math.floor(ox+(left()+PANE_W-6)*scale),(int)Math.floor(oy+(top()+heightOfPane()-1)*scale));
        try {
            LoadoutPanel.small(g,title.getString(),x+2,y+1,.8F,0xFFE5EDF3);
            fixed(g,CorpseInventory.DOGTAG,x+32,y+14,40,40,"dogtag");
            if(statusOpen) StatusPresentation.list(g,deathStatus,x,y+100,72,47,statusScroll,inside(mx,my)?mx:Integer.MIN_VALUE,my,scale,ox,oy);
            else {
                LoadoutPanel.box(g,x,y+100,72,47);
                LoadoutPanel.small(g,Component.translatable("corpse.minecrafteft.dead").getString(),x+4,y+104,.65F,0xFFE29B99);
                g.fill(x+4,y+119,x+68,y+120,0xFFE29B99);
                double max=menu.body==null?100:menu.body.appearance().getDouble("MaxHealth");
                LoadoutPanel.small(g,String.format(Locale.ROOT,"0.0 / %.0f",max),x+4,y+132,.7F,0xFFDBE3E8);
                StatusPresentation.button(g,x+62,y+104,false);
            }
            LoadoutPanel.box(g,x+75,y+14,85,133);
            if (y+147>top() && y+14<top()+heightOfPane()) CorpseRenderer.preview(g,menu.body,x+117,y+141,55);
            String[] armor={"head","chest","legs","feet"};
            for(int i=0;i<4;i++) fixed(g,39-i,x+164,y+14+i*34,48,31,armor[i]);
            fixed(g,1,x,y+155,133,32,"primary1");fixed(g,0,x+139,y+155,73,32,"sidearm");
            fixed(g,2,x,y+192,133,32,"primary2");
            var melee=menu.body==null?ItemStack.EMPTY:ItemStack.of(menu.body.appearance().getCompound("Melee"));
            tile(g,null,melee,x+139,y+192,73,32,"melee");
            label(g,"pockets",x,y+235,.75F);
            for(int i=0;i<6;i++) {
                fixed(g,i+3,x+i*34,y+249,31,25,"");
                LoadoutPanel.small(g,Integer.toString(i+4),x+i*34+2,y+251,.5F,0xFFB3C2CC);
            }
            fixed(g,40,x,y+280,38,38,"offhand");
            fixed(g,CorpseInventory.BAG,x,y+bagY,38,38,"backpack");
            grid(g,loot.bag,menu.bagCells,x+44,y+bagY);
            fixed(g,CorpseInventory.POUCH,x,y+pouchY,38,38,"secure");
            grid(g,loot.pouch,menu.pouchCells,x+44,y+pouchY);
            if(!extras.isEmpty()) {
                label(g,"recovery",x,y+extraY,.7F);
                for(int i=0;i<extras.size();i++) {
                    Slot slot=extras.get(i);
                    tile(g,slot,slot.getItem(),x+i%10*18,y+extraY+13+i/10*18,18,18,"");
                }
            }
        } finally { g.disableScissor(); }
        int track=heightOfPane()-4;
        thumb=limit==0?track:Math.max(16,track*heightOfPane()/content);
        int start=top()+2+(limit==0?0:scroll*(track-thumb)/limit);
        int rail=left()+PANE_W-5;
        g.fill(rail,top()+2,rail+3,top()+heightOfPane()-2,0xFF35404A);
        g.fill(rail,start,rail+3,start+thumb,0xFF9BAFBA);
    }
    private void label(GuiGraphics g,String key,int x,int y,float size) {
        LoadoutPanel.small(g,Component.translatable("loadout.minecrafteft."+key).getString(),x,y,size,0xFFC5D1D8);
    }
    private void fixed(GuiGraphics g,int id,int x,int y,int w,int h,String key) {
        var slot=menu.gear.get(id);tile(g,slot,slot.getItem(),x,y,w,h,key);
    }
    private void tile(GuiGraphics g,Slot slot,ItemStack item,int x,int y,int w,int h,String key) {
        tile(g,slot,item,x,y,w,h,key,false);
    }
    private void tile(GuiGraphics g,Slot slot,ItemStack item,int x,int y,int w,int h,String key,boolean gridPreview) {
        if(y+h<=top() || y>=top()+heightOfPane()) return;
        LoadoutPanel.box(g,x,y,w,h);
        var hit=new Hit(slot,x,y,w,h);
        if(slot!=null) {
            hits.add(hit);
            if (inside(mx,my) && hit.contains(mx,my)) {
                var carried=menu.getCarried();
                int color=carried.isEmpty()?0x405A8095:slot.mayPlace(carried)?0x6639D873:0x66D14F4F;
                g.fill(x+1,y+1,x+w-1,y+h-1,color);
            }
        }
        if (!key.isEmpty()) label(g,key,x+2,y+2,Math.min(.62F,(w-4F)/Math.max(1,font.width(Component.translatable("loadout.minecrafteft."+key)))));
        int header=key.isEmpty()?1:10;
        if(gridPreview && StashMenu.isGridItem(item))
            InventoryItemPreview.footprint(g,item,x+2,y+header,w-4,h-header-2,false);
        else LoadoutPanel.stack(g,item,x+2,y+header,w-4,h-header-2);
    }
    private void grid(GuiGraphics g,EquipmentGrid grid,List<Slot> slots,int x,int y) {
        var size=grid.size();
        if(size.cells()==0) return;
        grids.add(new GridArea(grid,slots,x,y));
        for(int i=0;i<size.cells();i++) {
            int cx=x+i%size.width()*18,cy=y+i/size.width()*18;
            if (cy+18>top() && cy<top()+heightOfPane()) {
                LoadoutPanel.box(g,cx,cy,18,18);
                hits.add(new Hit(slots.get(i),cx,cy,18,18));
            }
        }
        for(int i=0;i<size.cells();i++) {
            var item=grid.getStackInSlot(i);if(item.isEmpty()) continue;
            int cx=x+i%size.width()*18,cy=y+i/size.width()*18;
            int w=StashMenu.footprintWidth(item)*18,h=StashMenu.footprintHeight(item)*18;
            tile(g,slots.get(i),item,cx,cy,w,h,"",true);
        }
        var carried=menu.getCarried();
        if(!carried.isEmpty() && inside(mx,my) && mx>=x && mx<x+size.width()*18 && my>=y && my<y+size.height()*18) {
            int w=StashMenu.footprintWidth(carried),h=StashMenu.footprintHeight(carried);
            var target=StashDragPlacement.target(mx,my,x,y,w,h);
            boolean fits=target.inside(w,h,size.width(),size.height()) && grid.canPlace(target.index(size.width()),carried);
            int left=x+target.column()*18,top=y+target.row()*18;
            g.fill(left+1,top+1,left+w*18-1,top+h*18-1,fits?0x9939D873:0x99D14F4F);
        }
    }
    private Slot at(double x,double y) {
        if (!inside(x,y)) return personal.slotAt(x,y,menu.getCarried());
        if (x>=left()+PANE_W-6) return null;
        var carried=menu.getCarried();
        if(!carried.isEmpty()) for(var area:grids) {
            var size=area.grid.size();
            if(x>=area.x && x<area.x+size.width()*18 && y>=area.y && y<area.y+size.height()*18) {
                int w=StashMenu.footprintWidth(carried),h=StashMenu.footprintHeight(carried);
                var target=StashDragPlacement.target(x,y,area.x,area.y,w,h);
                return target.inside(w,h,size.width(),size.height()) && area.grid.canPlace(target.index(size.width()),carried)
                        ?area.slots.get(target.index(size.width())):null;
            }
        }
        for (int i=hits.size()-1;i>=0;i--) if(hits.get(i).contains(x,y)) return hits.get(i).slot;
        return null;
    }
    @Override public Slot getSlotUnderMouse() { return at(mx,my); }
    @Override protected boolean hasClickedOutside(double x,double y,int left,int top,int button) {
        return !InventoryFrame.bounds(width,height).contains(ox+x*scale,oy+y*scale);
    }
    private boolean overDeathVitals(double px,double py) {
        int x=left()+3,y=top()+102-scroll;
        return inside(px,py) && px>=x && px<x+72 && py>=y && py<y+47;
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        double px=ux(x),py=uy(y);
        if(button==0 && overDeathVitals(px,py)) {
            int vx=left()+3,vy=top()+102-scroll;
            if(px>=vx+62 && px<vx+69 && py>=vy+4 && py<vy+11) { statusOpen=!statusOpen;statusScroll=0; }
            return true;
        }
        if(button==0 && inside(px,py) && px>=left()+PANE_W-6) {
            int travel=heightOfPane()-4-thumb;
            double start=top()+2+(limit==0?0:scroll*travel/(double)limit);
            grab=py>=start && py<start+thumb?py-start:thumb/2D;
            dragging=true;moveBar(py);return true;
        }
        if(button==0 && personal.beginDrag(px,py)) return true;
        if(button==0 && personal.toggleAt(px,py)) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);return true; }
        Slot slot=at(px,py);
        if(slot!=null && (button==0 || button==1)) {
            slotClicked(slot,slot.index,button,hasShiftDown()?ClickType.QUICK_MOVE:ClickType.PICKUP);return true;
        }
        if (px>=leftPos && px<leftPos+imageWidth && py>=topPos && py<topPos+imageHeight) return true;
        return super.mouseClicked(px,py,button);
    }
    private void moveBar(double py) { scroll=Mth.clamp((int)Math.round((py-top()-2-grab)*limit/Math.max(1,heightOfPane()-4-thumb)),0,limit); }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy) {
        if(dragging) { moveBar(uy(y));return true; }
        return personal.drag(ux(x),uy(y));
    }
    @Override public boolean mouseReleased(double x,double y,int button) { dragging=false;personal.release();return true; }
    @Override public boolean mouseScrolled(double x,double y,double delta) {
        if(statusOpen && overDeathVitals(ux(x),uy(y))) {
            int max=Math.max(0,StatusPresentation.rows(deathStatus).size()*23-28);
            statusScroll=Mth.clamp(statusScroll-(int)Math.signum(delta)*23,0,max);return true;
        }
        if(inside(ux(x),uy(y))) { scroll=Mth.clamp(scroll-(int)Math.signum(delta)*24,0,limit);return true; }
        return personal.scroll(ux(x),uy(y),delta);
    }
    @Override public boolean keyPressed(int key,int scan,int mods) {
        hoveredSlot=getSlotUnderMouse();
        if(key==GLFW.GLFW_KEY_R && StashMenu.isGridItem(menu.getCarried())) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1);return true;
        }
        return super.keyPressed(key,scan,mods);
    }
}
