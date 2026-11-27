package com.minecrafteft.client;

/** Screen-coordinate layout, independent of Minecraft rendering for edge-case checks. */
public final class StashTooltipPlacement {
    private static final int MARGIN = 5;
    private static final int GAP = 7;
    private StashTooltipPlacement() { }

    public static Placement place(int screenWidth, int screenHeight, int left, int top,
                                  int right, int bottom, int mouseX, int mouseY,
                                  int contentWidth, int contentHeight) {
        float scale = Math.min(1F, Math.min((screenWidth - 2F * MARGIN) / (contentWidth + 8F),
                (screenHeight - 2F * MARGIN) / (contentHeight + 8F)));
        scale = Math.max(0.01F, scale);
        int width = (int) Math.ceil((contentWidth + 8) * scale);
        int height = (int) Math.ceil((contentHeight + 8) * scale);
        // Edges: top, bottom, left, right. Prefer the nearest edge, then its opposite.
        int[] distance = {Math.abs(mouseY - top), Math.abs(bottom - mouseY),
                Math.abs(mouseX - left), Math.abs(right - mouseX)};
        int nearest = 0;
        for (int i = 1; i < 4; i++) if (distance[i] < distance[nearest]) nearest = i;
        int[] order = {nearest, nearest ^ 1, nearest < 2 ? 2 : 0, nearest < 2 ? 3 : 1};
        int x = 0;
        int y = 0;
        for (int i = 0; i < order.length; i++) {
            int side = order[i];
            x = side == 2 ? left - GAP - width : side == 3 ? right + GAP : mouseX - width / 2;
            y = side == 0 ? top - GAP - height : side == 1 ? bottom + GAP : mouseY - height / 2;
            boolean fits = side < 2 ? y >= MARGIN && y + height <= screenHeight - MARGIN
                    : x >= MARGIN && x + width <= screenWidth - MARGIN;
            if (fits || i == order.length - 1) break;
        }
        x = Math.max(MARGIN, Math.min(screenWidth - MARGIN - width, x));
        y = Math.max(MARGIN, Math.min(screenHeight - MARGIN - height, y));
        return new Placement(x, y, scale);
    }

    /** x/y include the tooltip's four-pixel background padding. */
    public record Placement(int x, int y, float scale) { }
}
