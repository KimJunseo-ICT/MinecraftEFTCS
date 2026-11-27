package com.minecrafteft.client;

/** Shared geometry for drawing, wheel hit testing and thumb dragging in both storage views. */
public record StashScrollbar(int x, int y, int width, int height, int totalRows, int visibleRows, int row) {
    public int maxRow() { return Math.max(0, totalRows - visibleRows); }
    public int thumbHeight() { return Math.min(height, Math.max(12, height * visibleRows / totalRows)); }
    public int thumbY() { return y + (maxRow() == 0 ? 0
            : (int) Math.round((height - thumbHeight()) * (double) row / maxRow())); }
    public boolean contains(double mouseX, double mouseY) {
        return maxRow() > 0 && mouseX >= x - 2 && mouseX < x + width + 2 && mouseY >= y && mouseY < y + height;
    }
    public boolean onThumb(double mouseY) { return mouseY >= thumbY() && mouseY < thumbY() + thumbHeight(); }
    public int rowAt(double mouseY, double grabOffset) {
        int travel = height - thumbHeight();
        return travel <= 0 ? 0 : Math.max(0, Math.min(maxRow(),
                (int) Math.round((mouseY - y - grabOffset) * maxRow() / travel)));
    }
}
