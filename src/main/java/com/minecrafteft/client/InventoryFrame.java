package com.minecrafteft.client;

import net.minecraft.client.gui.GuiGraphics;

/** One screen-space frame; fit content uniformly without changing slot/grid geometry. */
public final class InventoryFrame {
    private InventoryFrame() { }
    public record Bounds(int left,int top,int width,int height) {
        public boolean contains(double x,double y) {
            return x>=left && x<left+width && y>=top && y<top+height;
        }
    }
    public record Placement(double scale,double offsetX,double offsetY) { }
    public static Bounds bounds(int width,int height) {
        int left=Math.max(3,(int)Math.round(width*.008));
        int top=Math.max(18,(int)Math.round(height*.08));
        int bottom=Math.max(top+1,(int)Math.round(height*.924));
        return new Bounds(left,top,Math.max(1,width-left*2),bottom-top);
    }
    public static Placement fit(int width,int height,int contentWidth,int contentHeight,int leftPos,int topPos) {
        var frame=bounds(width,height);
        double scale=Math.max(.05,Math.min((frame.width-8D)/contentWidth,(frame.height-8D)/contentHeight));
        return new Placement(scale,frame.left+frame.width/2D-(leftPos+contentWidth/2D)*scale,
                frame.top+frame.height/2D-(topPos+contentHeight/2D)*scale);
    }
    public static void draw(GuiGraphics graphics,int width,int height) {
        var frame=bounds(width,height);
        LoadoutPanel.box(graphics,frame.left,frame.top,frame.width,frame.height);
        graphics.fill(frame.left+1,frame.top+1,frame.left+frame.width-1,frame.top+frame.height-1,0xFF111820);
    }
}
