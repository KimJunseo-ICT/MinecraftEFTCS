package com.minecrafteft.client;

import com.minecrafteft.vitals.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import java.util.Locale;

/** Deliberately laid out at each size: labels and icons are never stretched with the panel. */
final class MenuVitals {
    private MenuVitals() { }
    static void draw(GuiGraphics g,Player player,int x,int y,int width,int height,boolean detailed) {
        var state=player.getCapability(VitalProvider.CAPABILITY).resolve().orElse(null);
        double hp=state==null?100:state.health(),fraction=hp/VitalState.MAX_HEALTH;
        double stamina=state==null?VitalState.MAX_STAMINA:state.stamina();
        int color=fraction>.5?0xFFB8D9BD:fraction>.2?0xFFE5C86B:0xFFEF615A;
        LoadoutPanel.box(g,x,y,width,height);
        int waveform=detailed?72:31,margin=detailed?8:5;
        int traceX=x+margin,traceY=y+4,traceW=width-margin*2;
        g.fill(traceX,traceY,traceX+traceW,traceY+waveform,0xFF152029);
        String severity=switch(InventoryVitalWaveform.severity(fraction)) { case 0 -> "stable";case 1 -> "caution";default -> "danger"; };
        LoadoutPanel.small(g,Component.translatable("screen.minecrafteft.vital_"+severity).getString(),traceX+3,traceY+3,.65F,color);
        double now=net.minecraft.Util.getMillis()/1000D;int baseline=traceY+waveform*2/3,previous=baseline;
        for(int i=0;i<traceW;i++) {
            double from=now-InventoryVitalWaveform.WINDOW_SECONDS*(traceW-i)/traceW;
            var range=InventoryVitalWaveform.range(fraction,from,from+InventoryVitalWaveform.WINDOW_SECONDS/traceW);
            int amplitude=detailed?23:10,top=baseline-(int)Math.round(amplitude*range.maximum());
            int bottom=baseline-(int)Math.round(amplitude*range.minimum());
            g.fill(traceX+i,Math.min(previous,top),traceX+i+1,Math.max(previous,bottom)+1,color);
            previous=baseline-(int)Math.round(amplitude*range.last());
        }
        int numeric=y+(detailed?82:38),bar=y+(detailed?96:49);
        if(detailed) VitalHud.icon(g,VitalHud.statusIcon("health"),x+10,numeric-2,12);
        centered(g,String.format(Locale.ROOT,"%.1f / %.0f",hp,VitalState.MAX_HEALTH),x+width/2,numeric,detailed?.85F:.7F);
        meter(g,x+margin,bar,traceW,detailed?12:8,fraction,color,detailed?.8F:.65F);
        if(detailed) {
            VitalHud.icon(g,VitalHud.statusIcon("stamina"),x+10,y+119,12);
            centered(g,String.format(Locale.ROOT,"%.1f / %.0f",stamina,VitalState.MAX_STAMINA),x+width/2,y+121,.8F);
            meter(g,x+margin,y+136,traceW,12,stamina/VitalState.MAX_STAMINA,0xFF328BFF,.8F);
        }
        row(g,"energy",state==null?100:state.energy(),x+margin,y+(detailed?160:62),traceW,detailed?10:6,0xFFE7CE77,detailed?.75F:.55F);
        row(g,"hydration",state==null?100:state.hydration(),x+margin,y+(detailed?179:74),traceW,detailed?10:6,0xFF71C5FF,detailed?.75F:.55F);
        if(detailed) {
            int bpm=(int)Math.round(InventoryVitalWaveform.nominalBpm(fraction)+Math.sin(now*1.3)*2);
            label(g,"temperature","36.6 °C",x+8,y+201);
            label(g,"pulse",bpm+" BPM",x+width/2+3,y+201);
            label(g,"infection","0 / 100",x+8,y+220);
            label(g,"radiation","0 / 100",x+width/2+3,y+220);
        }
    }
    private static void label(GuiGraphics g,String key,String value,int x,int y) {
        LoadoutPanel.small(g,Component.translatable("menu.minecrafteft."+key).getString(),x,y,.5F,0xFFA8BAC5);
        LoadoutPanel.small(g,value,x,y+7,.65F,0xFFD9E5EB);
    }
    private static void centered(GuiGraphics g,String text,int x,int y,float size) {
        LoadoutPanel.small(g,text,Math.round(x-Minecraft.getInstance().font.width(text)*size/2),y,size,0xFFE4EDF1);
    }
    private static void meter(GuiGraphics g,int x,int y,int w,int h,double value,int color,float size) {
        value=Math.max(0,Math.min(1,value));VitalHud.gauge(g,x,y,w,h,value,color);
        centered(g,(int)Math.ceil(value*100)+"%",x+w/2,y+(h-7)/2,size);
    }
    private static void row(GuiGraphics g,String icon,double value,int x,int y,int width,int h,int color,float size) {
        VitalHud.icon(g,VitalHud.statusIcon(icon),x,y-2,10);
        meter(g,x+24,y,width-24,h,value/100,value<=10?0xFFEF615A:color,size);
    }
}
