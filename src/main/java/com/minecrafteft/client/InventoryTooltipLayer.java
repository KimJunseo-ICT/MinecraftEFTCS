package com.minecrafteft.client;

import net.minecraft.client.gui.GuiGraphics;

/** Tooltips, including Forge image components, must sit above items and case popups. */
final class InventoryTooltipLayer {
    private InventoryTooltipLayer() { }
    static void draw(GuiGraphics graphics, Runnable render) {
        graphics.flush();
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0, 0, 4000);
            render.run();
            graphics.flush();
        } finally {
            graphics.pose().popPose();
        }
    }
}
