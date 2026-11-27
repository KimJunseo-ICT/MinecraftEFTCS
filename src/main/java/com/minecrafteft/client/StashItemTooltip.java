package com.minecrafteft.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;

import java.util.List;

final class StashItemTooltip {
    private StashItemTooltip() { }

    static void render(GuiGraphics graphics, Font font, ItemStack stack, List<Component> lines,
                       int screenWidth, int screenHeight, int left, int top, int right, int bottom,
                       int mouseX, int mouseY) {
        InventoryTooltipLayer.draw(graphics, () -> renderContents(graphics, font, stack, lines,
                screenWidth, screenHeight, left, top, right, bottom, mouseX, mouseY));
    }

    private static void renderContents(GuiGraphics graphics, Font font, ItemStack stack, List<Component> lines,
                       int screenWidth, int screenHeight, int left, int top, int right, int bottom,
                       int mouseX, int mouseY) {
        // Keep Forge/TACZ tooltip components (including images and modifier-key details).
        var components = ForgeHooksClient.gatherTooltipComponents(stack, lines, stack.getTooltipImage(),
                0, screenWidth - 12, screenHeight, font);
        if (components.isEmpty()) return;
        var pre = ForgeHooksClient.onRenderTooltipPre(stack, graphics, mouseX, mouseY,
                screenWidth, screenHeight, components, font, DefaultTooltipPositioner.INSTANCE);
        if (pre.isCanceled()) return;
        font = pre.getFont();
        int tooltipWidth = 0;
        int tooltipHeight = components.size() == 1 ? -2 : 0;
        for (var component : components) {
            tooltipWidth = Math.max(tooltipWidth, component.getWidth(font));
            tooltipHeight += component.getHeight();
        }
        if (components.size() > 1) tooltipHeight += 2;
        var position = StashTooltipPlacement.place(screenWidth, screenHeight, left, top, right, bottom,
                mouseX, mouseY, tooltipWidth, tooltipHeight);
        var color = ForgeHooksClient.onRenderTooltipColor(stack, graphics, position.x(), position.y(), font, components);
        graphics.flush();
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(position.x(), position.y(), 400);
            graphics.pose().scale(position.scale(), position.scale(), 1);
            TooltipRenderUtil.renderTooltipBackground(graphics, 4, 4, tooltipWidth, tooltipHeight, 0,
                    color.getBackgroundStart(), color.getBackgroundEnd(), color.getBorderStart(), color.getBorderEnd());
            graphics.flush();
            graphics.pose().translate(0, 0, 1);
            int y = 4;
            for (int i = 0; i < components.size(); i++) {
                var component = components.get(i);
                component.renderText(font, 4, y, graphics.pose().last().pose(), graphics.bufferSource());
                y += component.getHeight() + (i == 0 ? 2 : 0);
            }
            y = 4;
            for (int i = 0; i < components.size(); i++) {
                var component = components.get(i);
                component.renderImage(font, 4, y, graphics);
                y += component.getHeight() + (i == 0 ? 2 : 0);
            }
            graphics.flush();
        } finally {
            graphics.pose().popPose();
        }
    }
}
