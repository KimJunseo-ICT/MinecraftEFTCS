package com.minecrafteft.client;

import com.minecrafteft.MinecraftEFTMod;
import com.minecrafteft.status.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid=MinecraftEFTMod.MOD_ID,value=Dist.CLIENT)
public final class StatusPresentation {
    private static final java.lang.reflect.Field SPRINT_TRIGGER=net.minecraftforge.fml.util.ObfuscationReflectionHelper
            .findField(net.minecraft.client.player.LocalPlayer.class,"f_108583_");
    private static final java.lang.reflect.Field SPRINT_KEY_DOWN=net.minecraftforge.fml.util.ObfuscationReflectionHelper
            .findField(net.minecraft.client.KeyMapping.class,"f_90817_");
    public record Row(String icon,Component name,int ticks,String description) { }
    private record DescriptionTooltip(net.minecraft.client.gui.screens.Screen screen,String key,int x,int y) { }
    private static DescriptionTooltip pendingDescription;
    private record SprintRequest(net.minecraft.world.entity.player.Player owner,int id) { }
    private static SprintRequest pendingSprint;
    private static int nextSprintRequest;
    public static boolean sprintRestartFlag(com.minecrafteft.network.VitalSyncPacket packet) {
        var player=Minecraft.getInstance().player;
        if(pendingSprint!=null && pendingSprint.owner()==player) {
            if(packet.sprintRestartAcknowledgement()==pendingSprint.id()) pendingSprint=null;
            else return player.getCapability(com.minecrafteft.vitals.VitalProvider.CAPABILITY)
                    .map(v->v.sprintRestartRequired()).orElse(packet.sprintRestartRequired());
        }
        return packet.sprintRestartRequired();
    }
    public static void accept(int[] snapshot) {
        var player=Minecraft.getInstance().player;
        if(player!=null) { var state=StatusProvider.of(player);if(state!=null) state.restore(snapshot); }
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void blockUnavailableSprint(net.minecraftforge.client.event.MovementInputUpdateEvent event) {
        if(!(event.getEntity() instanceof net.minecraft.client.player.LocalPlayer player)) return;
        var state=StatusProvider.of(player);
        var vitals=player.getCapability(com.minecrafteft.vitals.VitalProvider.CAPABILITY).resolve().orElse(null);
        if(!StatusEvents.eligible(player) || !(state!=null && state.sprintBlocked()
                || vitals!=null && vitals.sprintRestartRequired())) return;
        player.setSprinting(false);
        // This hook precedes LocalPlayer's sprint decisions. Clear the double-tap window too,
        // so neither sprint key nor repeated forward input can apply sprint speed during travel.
        try {
            SPRINT_TRIGGER.setInt(player,0);
            // ToggleKeyMapping ignores setDown(false) in toggle mode; clear the underlying
            // pressed state so hold and toggle sprint both require a new input after exhaustion.
            SPRINT_KEY_DOWN.setBoolean(Minecraft.getInstance().options.keySprint,false);
        }
        catch(IllegalAccessException error) { throw new IllegalStateException("Cannot disable fracture sprint trigger",error); }
    }
    private static void requestSprintRestart() {
        var mc=Minecraft.getInstance();var player=mc.player;
        if(player==null || mc.screen!=null || !StatusEvents.eligible(player) || player.isUsingItem()) return;
        var status=StatusProvider.of(player);if(status!=null && status.sprintBlocked()) return;
        player.getCapability(com.minecrafteft.vitals.VitalProvider.CAPABILITY).ifPresent(vitals->{
            if(vitals.restartSprint()) {
                pendingSprint=new SprintRequest(player,++nextSprintRequest);
                com.minecrafteft.network.ModNetwork.CHANNEL.sendToServer(new com.minecrafteft.network.SprintRestartPacket(pendingSprint.id()));
            }
        });
    }
    @SubscribeEvent public static void freshSprintKey(net.minecraftforge.client.event.InputEvent.Key event) {
        if(event.getAction()==org.lwjgl.glfw.GLFW.GLFW_PRESS && Minecraft.getInstance().options.keySprint.matches(event.getKey(),event.getScanCode()))
            requestSprintRestart();
    }
    @SubscribeEvent public static void freshSprintMouse(net.minecraftforge.client.event.InputEvent.MouseButton.Post event) {
        if(event.getAction()==org.lwjgl.glfw.GLFW.GLFW_PRESS && Minecraft.getInstance().options.keySprint.matchesMouse(event.getButton()))
            requestSprintRestart();
    }
    @SubscribeEvent public static void statusClientTick(TickEvent.ClientTickEvent event) {
        var mc=Minecraft.getInstance();var player=mc.player;
        if(pendingSprint!=null && pendingSprint.owner()!=player) pendingSprint=null;
        if(event.phase!=TickEvent.Phase.END || player==null || mc.isPaused()) return;
        var state=StatusProvider.of(player);if(state==null) return;
        if(state.sprintBlocked() && StatusEvents.eligible(player)) player.setSprinting(false);
        if(player.isUsingItem() && player.getUseItem().getItem() instanceof SimpleTreatmentItem) player.setSprinting(false);
        if(player.isAlive()) { state.tick(false,()->1);state.resources().clientTick(); }
    }
    public static List<Row> rows(StatusState state) {
        var rows=new ArrayList<Row>();if(state==null) return rows;
        if(state.light()>0) rows.add(row("light_bleeding",Component.translatable("status.minecrafteft.light_bleeding").append(" ×"+state.light()),-1,"light_bleeding"));
        if(state.heavy()>0) rows.add(row("heavy_bleeding",Component.translatable("status.minecrafteft.heavy_bleeding").append(" ×"+state.heavy()),-1,"heavy_bleeding"));
        for(int ticks:state.wounds()) rows.add(row("fresh_wound",Component.translatable("status.minecrafteft.fresh_wound"),ticks,"fresh_wound"));
        if(state.arm()) rows.add(row("fracture",Component.translatable("status.minecrafteft.fracture_arm"),-1,"fracture_arm"));
        if(state.leg()) rows.add(row("fracture",Component.translatable("status.minecrafteft.fracture_leg"),-1,"fracture_leg"));
        if(state.painTicks()>0) rows.add(row("on_painkillers",Component.translatable("status.minecrafteft.on_painkillers"),state.painTicks(),"on_painkillers"));
        var resources=state.resources();
        if(resources.dehydrated()) rows.add(row("dehydration",Component.translatable("status.minecrafteft.dehydration"),resources.dehydrationTicks(),"dehydration"));
        if(resources.malnourished()) rows.add(row("malnutrition",Component.translatable("status.minecrafteft.malnutrition"),resources.malnutritionTicks(),"malnutrition"));
        if(resources.fatigue()>0) rows.add(row("fatigue",Component.translatable("status.minecrafteft.fatigue").append(" ×"+resources.fatigue()),resources.fatigueTicks(),"fatigue"));
        return rows;
    }
    private static Row row(String icon,Component name,int ticks,String key) { return new Row(icon,name,ticks,"status.minecrafteft.description."+key); }
    public static String time(int ticks) { int seconds=(ticks+19)/20;return ticks<0?"-":String.format(Locale.ROOT,"%d:%02d",seconds/60,seconds%60); }
    @SubscribeEvent public static void drawStatusDescriptionLast(net.minecraftforge.client.event.ScreenEvent.Render.Post event) {
        var request=pendingDescription;pendingDescription=null;
        if(request==null || request.screen()!=event.getScreen()) return;
        var font=Minecraft.getInstance().font;
        var lines=new ArrayList<net.minecraft.util.FormattedCharSequence>();
        // Each sentence starts on a new line; long sentences still wrap within the tooltip.
        for(String sentence:Component.translatable(request.key()).getString().split("(?<=[.!?])\\s+"))
            lines.addAll(font.split(Component.literal(sentence),180));
        InventoryTooltipLayer.draw(event.getGuiGraphics(),()->event.getGuiGraphics().renderTooltip(font,lines,request.x(),request.y()));
    }
    public static void icon(GuiGraphics g,String name,int x,int y,int size,float alpha) {
        var texture=ResourceLocation.fromNamespaceAndPath(MinecraftEFTMod.MOD_ID,"textures/gui/effects/"+name+".png");
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();g.setColor(1,1,1,alpha);
        // Crop only at draw time. Original 1024px artwork and its transparency stay intact.
        g.blit(texture,x,y,size,size,304,304,416,416,1024,1024);
        g.setColor(1,1,1,1);RenderSystem.disableBlend();
    }
    public static void button(GuiGraphics g,int x,int y,boolean open) {
        g.fill(x,y,x+7,y+7,0xFF24BCE9);g.fill(x+1,y+1,x+6,y+6,0xFF11232F);
        g.fill(x+2,y+3,x+5,y+4,0xFF24BCE9);
        if(!open) g.fill(x+3,y+2,x+4,y+5,0xFF24BCE9);
    }
    public static void list(GuiGraphics g,StatusState state,int x,int y,int scroll,int mx,int my,double scale,double ox,double oy) {
        list(g,state,x,y,82,99,scroll,mx,my,scale,ox,oy);
    }
    public static void list(GuiGraphics g,StatusState state,int x,int y,int width,int height,int scroll,int mx,int my,double scale,double ox,double oy) {
        list(g,state,x,y,width,height,scroll,mx,my,scale,ox,oy,true);
    }
    public static void list(GuiGraphics g,StatusState state,int x,int y,int width,int height,int scroll,int mx,int my,double scale,double ox,double oy,boolean closeButton) {
        LoadoutPanel.box(g,x,y,width,height);
        LoadoutPanel.small(g,Component.translatable("screen.minecrafteft.status_list").getString(),x+4,y+5,.6F,0xFFE5EDF3);
        if(closeButton) button(g,x+width-10,y+4,true);
        int viewport=height-19;
        var rows=rows(state);int max=Math.max(0,rows.size()*23-viewport);scroll=Math.min(scroll,max);
        g.enableScissor((int)Math.ceil(ox+(x+2)*scale),(int)Math.ceil(oy+(y+15)*scale),
                (int)Math.floor(ox+(x+width-3)*scale),(int)Math.floor(oy+(y+height-2)*scale));
        Row hovered=null;
        try {
            if(rows.isEmpty()) LoadoutPanel.small(g,Component.translatable("screen.minecrafteft.status_none").getString(),x+5,y+22,.6F,0xFFB8D9BD);
            for(int i=0;i<rows.size();i++) {
                Row row=rows.get(i);int top=y+16+i*23-scroll;
                if(top+22<y+15 || top>y+height-2) continue;
                if(mx>=x+2 && mx<x+width-4 && my>=Math.max(top,y+15) && my<Math.min(top+22,y+height-2)) {
                    g.fill(x+2,top,x+width-4,top+22,0xFF31404B);hovered=row;
                }
                icon(g,row.icon(),x+3,top+2,17,1);
                LoadoutPanel.small(g,row.name().getString(),x+22,top+3,Math.min(.55F,(width-28F)/Math.max(1,Minecraft.getInstance().font.width(row.name()))),0xFFE7EDF1);
                LoadoutPanel.small(g,time(row.ticks()),x+22,top+13,.55F,0xFFAABBC6);
            }
        } finally { g.disableScissor(); }
        if(max>0) {
            int h=Math.max(8,viewport*viewport/(rows.size()*23)),top=y+16+scroll*(viewport-h)/max;
            g.fill(x+width-3,y+15,x+width-2,y+height-2,0xFF3A4853);g.fill(x+width-3,top,x+width-2,top+h,0xFF8BABB9);
        }
        if(hovered!=null) {
            // The caller may still be inside the corpse pane's scissor. Draw after every
            // inventory pane has restored its clipping and transform, using screen pixels.
            pendingDescription=new DescriptionTooltip(Minecraft.getInstance().screen,hovered.description(),
                    (int)Math.round(ox+mx*scale),(int)Math.round(oy+my*scale));
        }
    }
}
