package com.minecrafteft.client;

/** One calculation for both the landing highlight and the placement click. */
public final class StashDragPlacement {
    private StashDragPlacement() { }

    public static Target target(double mouseX, double mouseY, int gridX, int gridY,
                                int itemWidth, int itemHeight) {
        int column = (int) Math.floor((mouseX - gridX - itemWidth * 9.0 + 9.0) / 18.0);
        int row = (int) Math.floor((mouseY - gridY - itemHeight * 9.0 + 9.0) / 18.0);
        return new Target(column, row);
    }

    public record Target(int column, int row) {
        public boolean inside(int width, int height, int rows) {
            return inside(width,height,10,rows);
        }
        public boolean inside(int width,int height,int columns,int rows) { return column>=0 && row>=0 && column+width<=columns && row+height<=rows; }
        public int viewIndex() { return row * 10 + column; }
        public int index(int columns) { return row*columns+column; }
    }
}
