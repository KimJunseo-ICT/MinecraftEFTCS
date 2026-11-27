package com.minecrafteft.client;

import com.minecrafteft.MinecraftEFTMod;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Keep the supplied full-resolution artwork; remove only dark outline pixels at load time. */
@Mod.EventBusSubscriber(modid=MinecraftEFTMod.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class CategoryIconRenderer {
    private record Icon(ResourceLocation texture,int width,int height,int left,int top,int spanWidth,int spanHeight,boolean dynamic) { }
    private static final Map<String,Icon> CACHE=new HashMap<>();
    private CategoryIconRenderer() { }
    public static void draw(GuiGraphics graphics,String name,int x,int y,int width,int height) {
        Icon icon=CACHE.computeIfAbsent(name,CategoryIconRenderer::load);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        float fit=Math.min(width/(float)icon.spanWidth(),height/(float)icon.spanHeight());
        int w=Math.max(1,Math.round(icon.spanWidth()*fit)),h=Math.max(1,Math.round(icon.spanHeight()*fit));
        graphics.blit(icon.texture(),x+(width-w)/2,y+(height-h)/2,w,h,icon.left(),icon.top(),icon.spanWidth(),icon.spanHeight(),icon.width(),icon.height());
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }
    private static Icon load(String name) {
        var minecraft=Minecraft.getInstance();
        var source=ResourceLocation.fromNamespaceAndPath(MinecraftEFTMod.MOD_ID,"textures/gui/categories/"+name+".png");
        try(var input=minecraft.getResourceManager().open(source)) {
            NativeImage image=NativeImage.read(input);
            for(int y=0;y<image.getHeight();y++) for(int x=0;x<image.getWidth();x++) {
                int pixel=image.getPixelRGBA(x,y),alpha=pixel>>>24;
                if(alpha==0) continue;
                int brightness=Math.max(pixel&255,Math.max(pixel>>>8&255,pixel>>>16&255));
                if(brightness>=200) continue;
                float edge=Math.max(0,Math.min(1,(brightness-40)/160F));
                edge=edge*edge*(3-2*edge);
                image.setPixelRGBA(x,y,(pixel&0x00FFFFFF)|(Math.round(alpha*edge)<<24));
            }
            int w=image.getWidth(),h=image.getHeight();
            // The supplied backpack sheet also contains a caption and a corner watermark.
            // Select only its icon region; never stretch that whole sheet into a square.
            boolean sheet=name.equals("backpacks") && w==1024 && h==572;
            int minX=w,minY=h,maxX=-1,maxY=-1;
            for(int y=sheet?70:0;y<(sheet?410:h);y++) for(int x=sheet?350:0;x<(sheet?670:w);x++) {
                if((image.getPixelRGBA(x,y)>>>24)<32) continue;
                minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);
            }
            if(maxX<minX || maxY<minY) { minX=0;minY=0;maxX=w-1;maxY=h-1; }
            DynamicTexture texture=new DynamicTexture(image);texture.setFilter(true,false);
            var id=ResourceLocation.fromNamespaceAndPath(MinecraftEFTMod.MOD_ID,"category_clean/"+name);
            minecraft.getTextureManager().register(id,texture);
            return new Icon(id,w,h,minX,minY,maxX-minX+1,maxY-minY+1,true);
        } catch(IOException error) { return new Icon(source,1,1,0,0,1,1,false); }
    }
    @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener)manager->Minecraft.getInstance().execute(()-> {
            for(var icon:CACHE.values()) if(icon.dynamic()) Minecraft.getInstance().getTextureManager().release(icon.texture());
            CACHE.clear();
        }));
    }
}
