package com.minecrafteft.client;

import com.minecrafteft.network.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class ProfileView {
    private static CompoundTag data=new CompoundTag();
    private static Object connection;
    private int x,y;
    private boolean dragging;
    private float yaw;
    private int modelTick=-1;
    private String model="";
    public static void accept(CompoundTag tag) {
        data=tag==null?new CompoundTag():tag.copy();connection=Minecraft.getInstance().getConnection();
    }
    private static CompoundTag data(Player player) {
        if(connection!=Minecraft.getInstance().getConnection() || !data.hasUUID("Owner") || !data.getUUID("Owner").equals(player.getUUID())) return new CompoundTag();
        return data;
    }
    void draw(GuiGraphics g,int x,int y,int mouseX,int mouseY,double scale,double ox,double oy) {
        this.x=x;this.y=y;var player=Minecraft.getInstance().player;if(player==null) return;
        var row=data(player);int level=Math.max(1,row.getInt("Level")),rank=Math.min(5,(level-1)/10);
        LoadoutPanel.box(g,x+12,y+28,180,236);
        var texture=ResourceLocation.fromNamespaceAndPath("minecrafteft","textures/gui/menu/"+(rank+1)+"_level.png");
        int textureWidth=rank<3?340:345,textureHeight=rank<3?330:340;
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.blit(texture,x+20,y+35,36,Math.round(36F*textureHeight/textureWidth),0,0,textureWidth,textureHeight,textureWidth,textureHeight);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        String digits=String.format(Locale.ROOT,"%02d",level);
        text(g,digits,x+60,y+38,Math.min(4F,102F/Math.max(1,Minecraft.getInstance().font.width(digits))),0xFFE4EDF1);
        text(g,tr("rank."+rank),x+20,y+76,.75F,0xFFB8C6CE);
        if(player.isCreative()) {
            button(g,"+",x+171,y+36,mouseX,mouseY);button(g,"−",x+171,y+51,mouseX,mouseY);
        }
        g.enableScissor((int)Math.ceil(ox+(x+18)*scale),(int)Math.ceil(oy+(y+87)*scale),
                (int)Math.floor(ox+(x+186)*scale),(int)Math.floor(oy+(y+227)*scale));
        try { avatar(g,player,x+102,y+225,57,yaw); } finally { g.disableScissor(); }
        centered(g,player.getGameProfile().getName(),x+102,y+236,160,.95F);
        if(modelTick<0 || Math.abs(player.tickCount-modelTick)>=20) {
            modelTick=player.tickCount;model="";
            if(net.minecraftforge.fml.ModList.get().isLoaded("yes_steve_model")) {
                var selected=player.saveWithoutId(new CompoundTag()).getCompound("ForgeCaps").getCompound("yes_steve_model:model_id");
                if(!selected.getBoolean("disabled")) model=selected.getString("model_id");
            }
        }
        centered(g,model,x+102,y+251,160,.65F);
        LoadoutPanel.box(g,x+202,y+28,394,66);
        text(g,tr("overview"),x+212,y+35,.8F,0xFFDCE7EF);
        long created=row.getLong("CreatedAt");
        String[] headlines={tr("age"),tr("survival"),tr("playtime")};
        String[] values={Long.toString(created==0?0:Math.max(0,(System.currentTimeMillis()-created)/86_400_000L))+" "+tr("days"),duration(row.getLong("LifeTicks")),duration(row.getLong("OnlineTicks"))};
        for(int i=0;i<3;i++) {
            int cx=x+211+i*128;text(g,headlines[i],cx,y+52,.65F,0xFF9BAFBC);text(g,values[i],cx,y+69,.85F,0xFFE4EDF1);
        }
        LoadoutPanel.box(g,x+202,y+101,394,163);
        String date=created==0?"—":DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(created));
        g.enableScissor((int)Math.ceil(ox+(x+204)*scale),(int)Math.ceil(oy+(y+103)*scale),
                (int)Math.floor(ox+(x+591)*scale),(int)Math.floor(oy+(y+262)*scale));
        try {
            text(g,tr("general"),x+212,y+111,.75F,0xFFB8D9BD);
            statistic(g,"created",date,x+212,y+128,176);
            statistic(g,"survival_rank",tr("rank."+rank),x+212,y+146,176);
            text(g,tr("travel_stats"),x+212,y+185,.75F,0xFFB8D9BD);
            statistic(g,"distance",String.format(Locale.ROOT,"%.3f km",row.getLong("TravelCm")/100000D),x+212,y+203,176);
            text(g,tr("body_stats"),x+408,y+111,.75F,0xFFB8D9BD);
            String[] names={"healed","fractures","dehydrations","drinks","foods","medicines"};
            String[] totals={String.format(Locale.ROOT,"%.1f",row.getDouble("Healed")),Long.toString(row.getLong("Fractures")),Long.toString(row.getLong("Dehydrations")),Long.toString(row.getLong("Drink")),Long.toString(row.getLong("Food")),Long.toString(row.getLong("Medicine"))};
            for(int i=0;i<names.length;i++) {
                statistic(g,names[i],totals[i],x+408,y+128+i*18,176);
            }
        } finally { g.disableScissor(); }
    }
    boolean click(double px,double py) {
        var player=Minecraft.getInstance().player;if(player==null) return false;
        if(player.isCreative() && px>=x+171 && px<x+184 && py>=y+36 && py<y+65) {
            ModNetwork.CHANNEL.sendToServer(new ProfileLevelPacket(py<y+50?1:-1));return true;
        }
        dragging=px>=x+18 && px<x+186 && py>=y+87 && py<y+227;return dragging;
    }
    boolean drag(double dx) { if(!dragging) return false;yaw=(float)((yaw+dx*1.5)%360);return true; }
    void release() { dragging=false; }
    boolean scroll(double px,double py,double amount) {
        return false;
    }
    static void avatar(GuiGraphics g,Player player,int x,int feet,int size,float yaw) {
        float body=player.yBodyRot,head=player.yHeadRot,rotation=player.getYRot(),pitch=player.getXRot();
        var pose=player.getPose();
        try {
            player.yBodyRot=player.yHeadRot=180;player.setYRot(180);player.setXRot(0);player.setPose(net.minecraft.world.entity.Pose.STANDING);
            InventoryScreen.renderEntityInInventory(g,x,feet,size,Axis.ZP.rotationDegrees(180).mul(Axis.YP.rotationDegrees(yaw)),new org.joml.Quaternionf(),player);
        } finally { player.yBodyRot=body;player.yHeadRot=head;player.setYRot(rotation);player.setXRot(pitch);player.setPose(pose); }
    }
    private static String duration(long ticks) { long minutes=Math.max(0,ticks)/1200;return Component.translatable("menu.minecrafteft.duration",minutes/60,minutes%60).getString(); }
    private static String tr(String key) { return Component.translatable("menu.minecrafteft."+key).getString(); }
    private static void statistic(GuiGraphics g,String key,String value,int x,int y,int width) {
        g.fill(x-3,y-3,x+width+2,y+11,0xFF19242D);
        float size=.65F;String label=tr(key);
        int available=width-8-(int)Math.ceil(Minecraft.getInstance().font.width(value)*size);
        text(g,label,x,y,Math.min(size,available/(float)Math.max(1,Minecraft.getInstance().font.width(label))),0xFFC6D3DE);
        right(g,value,x+width,y,size);
    }
    private static void text(GuiGraphics g,String value,int x,int y,float size,int color) { LoadoutPanel.small(g,value,x,y,size,color); }
    private static void right(GuiGraphics g,String value,int x,int y,float size) { text(g,value,Math.round(x-Minecraft.getInstance().font.width(value)*size),y,size,0xFFE4EDF1); }
    private static void centered(GuiGraphics g,String value,int x,int y,int width,float size) {
        size=Math.min(size,width/(float)Math.max(1,Minecraft.getInstance().font.width(value)));
        text(g,value,Math.round(x-Minecraft.getInstance().font.width(value)*size/2),y,size,0xFFE4EDF1);
    }
    private static void button(GuiGraphics g,String value,int x,int y,int mx,int my) {
        LoadoutPanel.box(g,x,y,13,13);if(mx>=x && mx<x+13 && my>=y && my<y+13) g.fill(x+1,y+1,x+12,y+12,0xFF456071);
        text(g,value,x+4,y+3,.8F,0xFFE4EDF1);
    }
}
