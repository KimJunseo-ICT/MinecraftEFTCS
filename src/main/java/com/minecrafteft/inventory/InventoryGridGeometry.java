package com.minecrafteft.inventory;

import java.util.function.IntUnaryOperator;

/** Shared server placement predicate, kept independent of Minecraft for boundary testing. */
public final class InventoryGridGeometry {
    private InventoryGridGeometry() { }
    public static boolean fits(int columns,int rows,int anchor,int width,int height,IntUnaryOperator ownerAt) {
        if(columns<=0 || rows<=0 || anchor<0 || anchor>=columns*rows || width<=0 || height<=0) return false;
        int x=anchor%columns,y=anchor/columns;
        if(width>columns-x || height>rows-y) return false;
        for(int row=0;row<height;row++) for(int column=0;column<width;column++) {
            int occupied=ownerAt.applyAsInt(anchor+row*columns+column);
            if(occupied>=0) return false;
        }
        return true;
    }
}
