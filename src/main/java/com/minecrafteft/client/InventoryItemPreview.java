package com.minecrafteft.client;

import com.minecrafteft.stash.StashMenu;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/** One appearance path for grid items, including the cursor between inventory panes. */
final class InventoryItemPreview {
    private InventoryItemPreview() { }

    static void carried(GuiGraphics graphics, ItemStack stack, int mouseX, int mouseY) {
        if (stack.isEmpty()) return;
        if (StashMenu.isGridItem(stack)) {
            int width = StashMenu.footprintWidth(stack) * 18 - 2;
            int height = StashMenu.footprintHeight(stack) * 18 - 2;
            footprint(graphics, stack, mouseX - width / 2, mouseY - height / 2, width, height, false);
        } else {
            graphics.renderItem(stack, mouseX - 8, mouseY - 8);
            graphics.renderItemDecorations(Minecraft.getInstance().font, stack, mouseX - 8, mouseY - 8);
        }
    }

    static void footprint(GuiGraphics graphics, ItemStack stack, int x, int y, int width, int height, boolean caseOpen) {
        if (StashMenu.isRotated(stack)) {
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(x + width, y, 0);
                graphics.pose().mulPose(Axis.ZP.rotationDegrees(90));
                unrotated(graphics, stack, 0, 0, height, width, caseOpen);
            } finally { graphics.pose().popPose(); }
        } else unrotated(graphics, stack, x, y, width, height, caseOpen);
    }

    private static void unrotated(GuiGraphics graphics, ItemStack stack, int x, int y, int width, int height, boolean caseOpen) {
        if ((StashMenu.isTaczGun(stack) || StashMenu.isWeaponCase(stack))
                && TaczGunPreviewRenderer.render(graphics, stack, x, y, width, height, caseOpen)) return;
        if (StashMenu.isWeaponCase(stack)) {
            int w = Math.max(1, Math.min(width - 4, (height - 4) * 2)), h = Math.max(1, w / 2);
            int left = x + (width - w) / 2, top = y + (height - h) / 2;
            graphics.fill(left, top, left + w, top + h, 0xFFE7E7DC);
            graphics.fill(left, top + h / 4, left + w, top + h / 4 + 1, 0xFF1E2226);
            return;
        }
        // Keep a visible fallback until TACZ's model is ready or capture can be retried.
        graphics.pose().pushPose();
        try {
            float scale = Math.min(width, height) / 16F;
            graphics.pose().translate(x + (width - 16 * scale) / 2, y + (height - 16 * scale) / 2, 110);
            graphics.pose().scale(scale, scale, 1);
            graphics.renderItem(stack, 0, 0);
        } finally { graphics.pose().popPose(); }
    }
}
