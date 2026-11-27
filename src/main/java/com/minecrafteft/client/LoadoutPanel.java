package com.minecrafteft.client;

import com.minecrafteft.inventory.*;
import com.minecrafteft.stash.StashMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Shared equipment surface for E-inventory and the left side of personal storage. */
public final class LoadoutPanel {
    public static final int WIDTH=438,HEIGHT=276;
    private final LoadoutSlots slots;
    private final boolean compact;
    private boolean renewed,healthMode;
    private final List<Hit> hits=new ArrayList<>();
    private final List<GridArea> grids=new ArrayList<>();
    private int scroll,maxScroll,x,y;
    private boolean dragging;
    private boolean statusOpen;
    private int statusScroll,healthMouseX,healthMouseY;
    private double scale=1,offsetX,offsetY;
    private record Hit(Slot slot,int x,int y,int w,int h) { boolean contains(double px,double py) { return px>=x && px<x+w && py>=y && py<y+h; } }
    private record GridArea(EquipmentGrid grid,List<Slot> slots,int x,int y) { }
    public LoadoutPanel(LoadoutSlots slots) { this(slots,false); }
    public LoadoutPanel(LoadoutSlots slots,boolean compact) { this.slots=slots;this.compact=compact; }
    public void renewed(boolean health) { renewed=true;healthMode=health; }
    public void clearHits() { hits.clear();grids.clear(); }
    private int panelWidth() { return renewed?608:compact?WIDTH-54:WIDTH; }
    private int rightX() { return x+(renewed?86:compact?-54:0); }
    private int rightWidth() { return renewed?282:204; }
    private int bottom() { return renewed?267:263; }
    public void draw(GuiGraphics g,int x,int y,int mouseX,int mouseY,double scale,double ox,double oy,ItemStack carried) {
        healthMouseX=mouseX;healthMouseY=mouseY;
        this.x=x;this.y=y;this.scale=scale;offsetX=ox;offsetY=oy;hits.clear();grids.clear();
        g.pose().pushPose();g.pose().translate(0,0,210);
        g.fill(x+4,y+22,x+panelWidth(),y+HEIGHT-4,0xFF111820);
        if(renewed) drawRenewed(g,mouseX,mouseY);
        else {
        int characterX=x+(compact?12:98),characterWidth=compact?104:74,characterCenter=characterX+characterWidth/2;
        box(g,characterX,y+28,characterWidth,144);scissor(g,characterX+1,y+29,characterX+characterWidth-1,y+171);
        try { InventoryScreen.renderEntityInInventoryFollowsMouse(g,characterCenter,y+165,59,characterCenter-mouseX,y+88-mouseY,slots.player); }
        finally { g.disableScissor(); }
        if(!compact) {
            fixed(g,slots.dogtag,x+52,y+28,42,42,"dogtag",mouseX,mouseY);
            health(g,x+12,y+73);
        }
        String[] armor={"head","chest","legs","feet"};
        for(int i=0;i<4;i++) fixed(g,slots.armor.get(i),x+(compact?128:176),y+28+i*37,compact?36:42,33,armor[i],mouseX,mouseY);
        fixed(g,slots.hotbar.get(1),x+12,y+188,compact?104:126,34,"primary1",mouseX,mouseY);
        fixed(g,slots.hotbar.get(2),x+12,y+230,compact?104:126,34,"primary2",mouseX,mouseY);
        fixed(g,slots.hotbar.get(0),x+(compact?128:150),y+188,compact?36:68,34,"sidearm",mouseX,mouseY);
        fixed(g,slots.melee,x+(compact?128:150),y+230,compact?36:68,34,"melee",mouseX,mouseY);
        }
        int rx=rightX();
        box(g,rx+228,y+28,rightWidth(),bottom()-27);
        var state=LoadoutState.of(slots.player);int bagHeight=state.backpack.size().height()*18;
        int bagTop=renewed?140:128;
        int secureTop=bagTop+Math.max(38,bagHeight)+6;
        int recoveryTop=secureTop+Math.max(42,state.secure.size().height()*18)+18;
        boolean recovery=slots.recovery.stream().anyMatch(Slot::hasItem);
        maxScroll=Math.max(0,(recovery?recoveryTop+91:secureTop+Math.max(38,state.secure.size().height()*18))-bottom());
        scroll=Math.min(scroll,maxScroll);
        scissor(g,rx+230,y+29,rx+228+rightWidth()-5,y+bottom());
        try {
            text(g,"pockets",rx+236,y+35-scroll,.8F,0xFFB8C4CE);
            for(int i=0;i<6;i++) {
                int pitch=renewed?38:29;
                Slot slot=slots.hotbar.get(i+3);cell(g,slot,rx+236+i*pitch,y+49-scroll,renewed?34:26,renewed?27:23,mouseX,mouseY);
                small(g,Integer.toString(i+4),rx+238+i*pitch,y+50-scroll,.55F,0xFF97A6B4);
            }
            fixed(g,slots.offhand,rx+236,y+(renewed?88:82)-scroll,renewed?36:32,30,"offhand",mouseX,mouseY);
            int toggleY=y+(renewed?131:119)-scroll;
            g.fill(rx+236,toggleY,rx+246,toggleY+8,state.hideBackpack?0xFF7CBA8D:0xFF48525C);
            text(g,"backpack_visibility",rx+250,toggleY,.65F,0xFFAFBCC6);
            fixed(g,slots.bag,rx+236,y+bagTop-scroll,36,38,"backpack",mouseX,mouseY);
            grid(g,state.backpack,slots.backpack,rx+278,y+bagTop-scroll,mouseX,mouseY,carried);
            fixed(g,slots.pouch,rx+236,y+secureTop-scroll,36,38,"secure",mouseX,mouseY);
            grid(g,state.secure,slots.secure,rx+278,y+secureTop-scroll,mouseX,mouseY,carried);
            if(recovery) {
                text(g,"recovery",rx+236,y+recoveryTop-scroll,.7F,0xFFDFC17E);
                for(int i=0;i<36;i++) cell(g,slots.recovery.get(i),rx+236+i%10*18,y+recoveryTop+13+i/10*18-scroll,16,16,mouseX,mouseY);
            }
        } finally { g.disableScissor(); }
        int track=bottom()-33,barX=rx+228+rightWidth()-4;
        g.fill(barX,y+31,barX+2,y+31+track,0xFF29343E);
        int thumb=maxScroll==0?track:Math.max(18,track*(bottom()-29)/(bottom()-29+maxScroll));
        int thumbY=y+31+(maxScroll==0?0:(track-thumb)*scroll/maxScroll);
        g.fill(barX,thumbY,barX+2,thumbY+thumb,0xFF91A5B0);
        g.pose().popPose();
    }
    private void drawRenewed(GuiGraphics g,int mx,int my) {
        if(healthMode) MenuVitals.draw(g,slots.player,x+12,y+28,152,240,true);
        else {
            fixed(g,slots.hotbar.get(1),x+12,y+28,152,52,"primary1",mx,my);
            fixed(g,slots.hotbar.get(2),x+12,y+85,152,52,"primary2",mx,my);
            fixed(g,slots.hotbar.get(0),x+12,y+142,92,30,"sidearm",mx,my);
            fixed(g,slots.melee,x+110,y+142,54,30,"melee",mx,my);
            fixed(g,slots.dogtag,x+116,y+184,48,48,"dogtag",mx,my);
        }
        box(g,x+168,y+28,80,144);scissor(g,x+169,y+29,x+247,y+171);
        try { InventoryScreen.renderEntityInInventoryFollowsMouse(g,x+208,y+165,59,x+208-mx,y+88-my,slots.player); }
        finally { g.disableScissor(); }
        String[] armor={"head","chest","legs","feet"};
        for(int i=0;i<4;i++) fixed(g,slots.armor.get(i),x+252,y+28+i*37,44,33,armor[i],mx,my);
        if(healthMode || statusOpen) {
            StatusPresentation.list(g,com.minecrafteft.status.StatusProvider.of(slots.player),x+168,y+184,128,84,statusScroll,mx,my,scale,offsetX,offsetY,!healthMode);
        } else {
            MenuVitals.draw(g,slots.player,x+168,y+184,128,84,false);
            StatusPresentation.button(g,x+286,y+188,false);
            if(mx>=x+286 && mx<x+293 && my>=y+188 && my<y+195)
                g.renderTooltip(Minecraft.getInstance().font,Component.translatable("screen.minecrafteft.status_details"),mx,my);
        }
    }
    private void scissor(GuiGraphics g,int left,int top,int right,int bottom) {
        g.enableScissor((int)Math.ceil(offsetX+left*scale),(int)Math.ceil(offsetY+top*scale),
                (int)Math.floor(offsetX+right*scale),(int)Math.floor(offsetY+bottom*scale));
    }
    private void fixed(GuiGraphics g,Slot slot,int x,int y,int w,int h,String label,int mx,int my) {
        box(g,x,y,w,h);
        String caption=Component.translatable("loadout.minecrafteft."+label).getString();
        String key=keyFor(label);
        var font=Minecraft.getInstance().font;
        int keyWidth=key.isEmpty()?0:Math.max(9,Math.min(w/2,(int)Math.ceil(font.width(key)*.55F)+5));
        small(g,caption,x+3,y+2,Math.min(.65F,(w-6F-(keyWidth==0?0:keyWidth+3))/Math.max(1,font.width(caption))),0xFFB8C4CE);
        if(keyWidth>0) {
            int keyX=x+w-keyWidth;
            g.fill(keyX,y+1,keyX+keyWidth-1,y+10,0xFF35424E);
            float keyScale=Math.min(.55F,(keyWidth-4F)/Math.max(1,font.width(key)));
            centered(g,key,keyX+keyWidth/2F,y+2,keyScale,0xFFE3ECF1);
        }
        Hit hit=new Hit(slot,x+1,y+11,w-2,h-12);hits.add(hit);
        if(hit.contains(mx,my)) g.fill(x+1,y+11,x+w-1,y+h-1,0x404D7081);
        stack(g,slot.getItem(),x+3,y+12,w-6,h-14);
    }
    private static String keyFor(String label) {
        var options=Minecraft.getInstance().options;
        return switch(label) {
            case "primary1" -> options.keyHotbarSlots[1].getTranslatedKeyMessage().getString();
            case "primary2" -> options.keyHotbarSlots[2].getTranslatedKeyMessage().getString();
            case "sidearm" -> options.keyHotbarSlots[0].getTranslatedKeyMessage().getString();
            case "melee" -> LoadoutClient.MELEE.getTranslatedKeyMessage().getString();
            case "offhand" -> options.keySwapOffhand.getTranslatedKeyMessage().getString();
            default -> "";
        };
    }
    private void cell(GuiGraphics g,Slot slot,int x,int y,int w,int h,int mx,int my) {
        box(g,x-1,y-1,w+2,h+2);Hit hit=new Hit(slot,x,y,w,h);hits.add(hit);
        if(hit.contains(mx,my)) g.fill(x,y,x+w,y+h,0x405A8095);
        stack(g,slot.getItem(),x+1,y+1,w-2,h-2);
    }
    private void grid(GuiGraphics g,EquipmentGrid grid,List<Slot> cells,int gx,int gy,int mx,int my,ItemStack carried) {
        var size=grid.size();if(size.cells()==0) return;
        grids.add(new GridArea(grid,cells,gx,gy));
        for(int i=0;i<size.cells();i++) {
            int cx=gx+i%size.width()*18,cy=gy+i/size.width()*18;box(g,cx-1,cy-1,18,18);
            hits.add(new Hit(cells.get(i),cx,cy,16,16));
        }
        for(int i=0;i<size.cells();i++) {
            ItemStack item=grid.getStackInSlot(i);if(item.isEmpty()) continue;
            int cx=gx+i%size.width()*18,cy=gy+i/size.width()*18;
            int w=StashMenu.footprintWidth(item)*18-2,h=StashMenu.footprintHeight(item)*18-2;
            box(g,cx-1,cy-1,w+2,h+2);
            if (StashMenu.isGridItem(item)) InventoryItemPreview.footprint(g,item,cx+1,cy+1,w-2,h-2,false);
            else stack(g,item,cx+1,cy+1,w-2,h-2);hits.add(new Hit(cells.get(i),cx,cy,w,h));
        }
        if(!carried.isEmpty() && mx>=gx && mx<gx+size.width()*18 && my>=gy && my<gy+size.height()*18) {
            var target=StashDragPlacement.target(mx,my,gx,gy,StashMenu.footprintWidth(carried),StashMenu.footprintHeight(carried));
            int col=target.column(),row=target.row(),index=target.index(size.width());
            int w=StashMenu.footprintWidth(carried)*18-2,h=StashMenu.footprintHeight(carried)*18-2;
            boolean fits=target.inside(StashMenu.footprintWidth(carried),StashMenu.footprintHeight(carried),size.width(),size.height()) && grid.canPlace(index,carried);
            g.fill(gx+col*18,gy+row*18,gx+col*18+w,gy+row*18+h,fits?0x9939D873:0x99D14F4F);
        }
    }
    public Slot slotAt(double mx,double my,ItemStack carried) {
        if(mx>=rightX()+228 && !insideViewport(mx,my)) return null;
        if(!carried.isEmpty() && insideViewport(mx,my)) for(GridArea area:grids) {
            var size=area.grid.size();
            if(mx>=area.x && mx<area.x+size.width()*18 && my>=area.y && my<area.y+size.height()*18)
            {
                var target=StashDragPlacement.target(mx,my,area.x,area.y,StashMenu.footprintWidth(carried),StashMenu.footprintHeight(carried));
                return target.inside(StashMenu.footprintWidth(carried),StashMenu.footprintHeight(carried),size.width(),size.height())
                        && area.grid.canPlace(target.index(size.width()),carried)?area.slots.get(target.index(size.width())):null;
            }
        }
        for(int i=hits.size()-1;i>=0;i--) if(hits.get(i).contains(mx,my)) return hits.get(i).slot;
        return null;
    }
    public boolean inside(double mx,double my) { return mx>=x+4 && mx<x+panelWidth() && my>=y+22 && my<y+HEIGHT; }
    public boolean insideViewport(double mx,double my) { return mx>=rightX()+230 && mx<rightX()+228+rightWidth() && my>=y+29 && my<y+bottom(); }
    public boolean scroll(double mx,double my,double delta) {
        if(!compact && (statusOpen || healthMode) && overHealth(mx,my)) {
            int max=Math.max(0,StatusPresentation.rows(com.minecrafteft.status.StatusProvider.of(slots.player)).size()*23-(renewed?65:80));
            statusScroll=Math.max(0,Math.min(max,statusScroll-(int)Math.signum(delta)*23));return true;
        }
        if(!insideViewport(mx,my)) return false;scroll=Math.max(0,Math.min(maxScroll,scroll-(int)Math.signum(delta)*18));return true;
    }
    private boolean overHealth(double mx,double my) { return renewed?mx>=x+168 && mx<x+296 && my>=y+184 && my<y+268:mx>=x+12 && mx<x+94 && my>=y+73 && my<y+172; }
    public boolean toggleAt(double mx,double my) { int top=y+(renewed?131:119)-scroll;return insideViewport(mx,my) && mx>=rightX()+236 && mx<rightX()+270 && my>=top && my<top+8; }
    public boolean beginDrag(double mx,double my) {
        if(!compact && overHealth(mx,my)) {
            int bx=x+(renewed?286:84),by=y+(renewed?188:77);
            if(!healthMode && mx>=bx && mx<bx+7 && my>=by && my<by+7) { statusOpen=!statusOpen;statusScroll=0; }
            return true;
        }
        if(mx<rightX()+228+rightWidth()-6 || mx>=rightX()+228+rightWidth()+2 || my<y+29 || my>=y+bottom()) return false;
        dragging=true;drag(mx,my);return true;
    }
    public boolean drag(double mx,double my) { if(!dragging) return false;scroll=(int)(Math.max(0,Math.min(1,(my-y-31)/(bottom()-33)))*maxScroll);return true; }
    public boolean release() { boolean was=dragging;dragging=false;return was; }
    public static void stack(GuiGraphics g,ItemStack stack,int x,int y,int w,int h) {
        if(stack.isEmpty() || w<=0 || h<=0) return;
        if((StashMenu.isTaczGun(stack) || StashMenu.isWeaponCase(stack)) && TaczGunPreviewRenderer.render(g,stack,x,y,w,h,false)) return;
        float s=Math.min(w,h)/16F;g.pose().pushPose();g.pose().translate(x+(w-16*s)/2,y+(h-16*s)/2,120);g.pose().scale(s,s,1);
        g.renderItem(stack,0,0);g.renderItemDecorations(Minecraft.getInstance().font,stack,0,0);g.pose().popPose();
    }
    public static void box(GuiGraphics g,int x,int y,int w,int h) {
        g.fill(x,y,x+w,y+h,0xFF5B6871);g.fill(x+1,y+1,x+w-1,y+h-1,0xFF202B34);
    }
    public static void small(GuiGraphics g,String value,int x,int y,float scale,int color) {
        g.pose().pushPose();g.pose().translate(x,y,2);g.pose().scale(scale,scale,1);g.drawString(Minecraft.getInstance().font,value,0,0,color,false);g.pose().popPose();
    }
    private static void centered(GuiGraphics g,String value,float centerX,int y,float scale,int color) {
        small(g,value,Math.round(centerX-Minecraft.getInstance().font.width(value)*scale/2),y,scale,color);
    }
    private static void text(GuiGraphics g,String key,int x,int y,float scale,int color) { small(g,Component.translatable("loadout.minecrafteft."+key).getString(),x,y,scale,color); }
    private void health(GuiGraphics g,int x,int y) {
        if(statusOpen) { StatusPresentation.list(g,com.minecrafteft.status.StatusProvider.of(slots.player),x,y,statusScroll,healthMouseX,healthMouseY,scale,offsetX,offsetY);return; }
        var vitals=slots.player.getCapability(com.minecrafteft.vitals.VitalProvider.CAPABILITY).resolve().orElse(null);
        double hp=vitals==null?100:vitals.health(),energy=vitals==null?100:vitals.energy(),hydration=vitals==null?100:vitals.hydration();
        double maximum=com.minecrafteft.vitals.VitalState.MAX_HEALTH,fraction=Math.max(0,Math.min(1,hp/maximum));
        int color=fraction>.5?0xFFB8D9BD:fraction>.2?0xFFE5C86B:0xFFEF615A;
        int innerWidth=74;
        box(g,x,y,82,99);g.fill(x+4,y+4,x+78,y+44,0xFF152029);
        String status=switch(InventoryVitalWaveform.severity(fraction)) { case 0 -> "stable";case 1 -> "caution";default -> "danger"; };
        small(g,Component.translatable("screen.minecrafteft.vital_"+status).getString(),x+6,y+6,.55F,color);
        double now=net.minecraft.Util.getMillis()/1000D;
        int previous=0;
        for(int pixel=0;pixel<innerWidth;pixel++) {
            double start=now-InventoryVitalWaveform.WINDOW_SECONDS*(innerWidth-pixel)/innerWidth;
            double end=start+InventoryVitalWaveform.WINDOW_SECONDS/innerWidth;
            var range=InventoryVitalWaveform.range(fraction,start,end);
            int py=y+27-(int)Math.round(14*range.last());
            int top=y+27-(int)Math.round(14*range.maximum()),bottom=y+27-(int)Math.round(14*range.minimum());
            if(pixel==0) previous=py;
            g.fill(x+4+pixel,Math.min(top,previous),x+5+pixel,Math.max(bottom,previous)+1,color);previous=py;
        }
        // Current/max HP belongs between the waveform and health bar.
        centered(g,String.format(java.util.Locale.ROOT,"%.1f / %.0f",hp,maximum),x+41,y+49,.7F,0xFFD3DEE5);
        VitalHud.gauge(g,x+5,y+60,72,8,fraction,color);
        centered(g,(int)Math.ceil(fraction*100)+"%",x+41,y+61,.65F,0xFFFFFFFF);
        vitalRow(g,"energy",energy,x,y+77,energy<=10?0xFFFF625C:0xFFE7CE77);
        vitalRow(g,"hydration",hydration,x,y+89,hydration<=10?0xFFFF625C:0xFF71C5FF);
        StatusPresentation.button(g,x+72,y+4,false);
        if(healthMouseX>=x+72 && healthMouseX<x+79 && healthMouseY>=y+4 && healthMouseY<y+11)
            g.renderTooltip(Minecraft.getInstance().font,Component.translatable("screen.minecrafteft.status_details"),healthMouseX,healthMouseY);
    }
    private static void vitalRow(GuiGraphics g,String icon,double value,int x,int y,int color) {
        g.setColor(1,1,1,1);
        VitalHud.icon(g,VitalHud.statusIcon(icon),x+4,y-2,10);
        VitalHud.gauge(g,x+20,y,57,6,Math.max(0,Math.min(1,value/100)),color);
        centered(g,(int)Math.ceil(value)+"%",x+48.5F,y,.55F,0xFFFFFFFF);
    }
}
