package com.minecrafteft.client;

import com.minecrafteft.MinecraftEFTMod;
import com.minecrafteft.network.VitalSyncPacket;
import com.minecrafteft.vitals.VitalFoodData;
import com.minecrafteft.vitals.VitalProvider;
import com.minecrafteft.vitals.VitalState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MinecraftEFTMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class VitalHud {
    private static final ResourceLocation[] ICONS = {
            statusIcon("health"), statusIcon("stamina")
    };
    private static final VitalGaugeAnimation[] GAUGES = {
            new VitalGaugeAnimation(), new VitalGaugeAnimation()
    };
    private static java.lang.ref.WeakReference<net.minecraft.world.entity.player.Player> gaugeOwner =
            new java.lang.ref.WeakReference<>(null);
    private VitalHud() { }
    static ResourceLocation statusIcon(String name) {
        return ResourceLocation.fromNamespaceAndPath(MinecraftEFTMod.MOD_ID, "textures/gui/status/" + name + ".png");
    }
    public static void accept(VitalSyncPacket packet) {
        var player = Minecraft.getInstance().player;
        if (player != null) player.getCapability(VitalProvider.CAPABILITY).ifPresent(state ->
                state.update(packet.health(), packet.energy(), packet.hydration(), packet.stamina(), packet.sprintLocked(),StatusPresentation.sprintRestartFlag(packet)));
        // Vanilla's later health packet calls LocalPlayer.hurtTo on a drop in HP. Mirror the
        // authoritative value silently first; actual hurt cues come from DamageEvent packets.
        if(player!=null && player.isAlive() && packet.health()>0)
            player.setHealth((float)(packet.health()/VitalState.MAX_HEALTH*player.getMaxHealth()));
    }
    @SubscribeEvent public static void overlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("vitals", (gui, graphics, partialTick, width, height) -> {
            var mc = Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui || mc.player.isSpectator()) return;
            mc.player.getCapability(VitalProvider.CAPABILITY).ifPresent(state -> {
                graphics.flush();
                graphics.pose().pushPose();
                try {
                    // Chat draws at a higher depth than ordinary HUD quads.
                    if(mc.screen==null || mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen)
                        graphics.pose().translate(0,0,150);
                    render(graphics,state,width,height);
                    graphics.flush();
                } finally { graphics.pose().popPose(); }
            });
        });
    }
    private static void render(GuiGraphics graphics, VitalState state, int width, int height) {
        var font = Minecraft.getInstance().font;
        long now = System.nanoTime();
        double[] fractions = {state.health() / VitalState.MAX_HEALTH, state.stamina() / VitalState.MAX_STAMINA};
        var player = Minecraft.getInstance().player;
        boolean newPlayer = gaugeOwner.get() != player;
        for (int i = 0; i < GAUGES.length; i++) {
            if (newPlayer) GAUGES[i].snap(fractions[i], now);
            else GAUGES[i].target(fractions[i], now);
        }
        if (newPlayer) gaugeOwner = new java.lang.ref.WeakReference<>(player);
        // Health above stamina; energy and hydration are shown in the equipment vitals panel.
        // The two icons occupy [height-37, height-16), between chat history and chat input.
        // Shrink horizontally on narrow GUIs instead of raising a row into the history.
        int barWidth = Math.max(18,Math.min(44,width/2-140)), barHeight = 6, iconSize = 10;
        int top = height - 35;
        int[] colors = {state.health() <= VitalState.MAX_HEALTH * .25 ? 0xFFEF4C4C : 0xFFB8D9BD,
                state.stamina() <= VitalState.RED_STAMINA ? 0xFFEF4C4C : 0xFF338EFF};
        graphics.setColor(1, 1, 1, 1);
        for (int row = 0; row < ICONS.length; row++) {
            int iconX = 7, barX = iconX + iconSize + 3;
            int y = top + row * 11;
            icon(graphics,ICONS[row],iconX,y+(barHeight-iconSize)/2,iconSize);
            gauge(graphics, barX, y, barWidth, barHeight, GAUGES[row].value(now), colors[row]);
            if (row == 1) continue; // Stamina remains a visual gauge without a percentage.
            String text = percent(fractions[row]) + "%";
            graphics.pose().pushPose();
            graphics.pose().translate(barX + barWidth / 2F, y + barHeight / 2F, 1);
            graphics.pose().scale(.55F, .55F, 1);
            graphics.drawString(font, text, -font.width(text) / 2, -font.lineHeight / 2, 0xFFFFFFFF, true);
            graphics.pose().popPose();
        }
        var effects=com.minecrafteft.status.StatusProvider.of(player);
        if(effects!=null && effects.painTicks()>0) {
            float alpha=effects.painTicks()<200?.25F+.75F*(float)((Math.sin(System.nanoTime()/1e9*9)+1)/2):1;
            StatusPresentation.icon(graphics,"on_painkillers",7+iconSize+3+barWidth+3,top-2,10,alpha);
        }
    }
    static void icon(GuiGraphics graphics,ResourceLocation texture,int x,int y,int size) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        graphics.blit(texture,x,y,size,size,0,0,200,200,200,200);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }
    static void gauge(GuiGraphics graphics, int x, int y, int width, int height, double fraction, int color) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF72808B);
        graphics.fill(x, y, x + width, y + height, 0xDC131D27);
        float filled = (float) (width * fraction);
        if (filled <= 0) return;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(filled, 1, 1);
        graphics.fill(0, 0, 1, height, color);
        graphics.pose().popPose();
    }
    private static int percent(double fraction) { return (int) Math.ceil(fraction * 100); }

    @Mod.EventBusSubscriber(modid = MinecraftEFTMod.MOD_ID, value = Dist.CLIENT)
    public static final class ClientEvents {
        @SubscribeEvent public static void hideVanilla(RenderGuiOverlayEvent.Pre event) {
            if (Minecraft.getInstance().player != null
                    && (event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())
                    || event.getOverlay().id().equals(VanillaGuiOverlay.FOOD_LEVEL.id()))) event.setCanceled(true);
        }
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
            var player = Minecraft.getInstance().player;
            if (event.phase != TickEvent.Phase.END || player == null) return;
            VitalFoodData.install(player);
            if(com.minecrafteft.medical.Ai2Treatment.using(player)) player.setSprinting(false);
            player.getCapability(VitalProvider.CAPABILITY).ifPresent(state -> {
                if ((state.sprintLocked() || state.sprintRestartRequired()) && !player.isCreative()) player.setSprinting(false);
            });
        }
    }
}
