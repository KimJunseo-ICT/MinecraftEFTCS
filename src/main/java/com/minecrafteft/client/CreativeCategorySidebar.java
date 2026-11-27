package com.minecrafteft.client;

import com.minecrafteft.MinecraftEFTMod;
import com.minecrafteft.registry.ModContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import java.lang.reflect.Field;

/** A screen-local filter; the tab's full search index and other mods' tabs stay intact. */
@Mod.EventBusSubscriber(modid=MinecraftEFTMod.MOD_ID,bus=Mod.EventBusSubscriber.Bus.FORGE,value=Dist.CLIENT)
public final class CreativeCategorySidebar extends AbstractWidget {
    private static final Field SELECTED_TAB=ObfuscationReflectionHelper.findField(CreativeModeInventoryScreen.class,"f_98507_");
    private static CreativeCategorySidebar current;
    private final CreativeModeInventoryScreen screen;
    private int selected=-1,firstRow;
    private boolean wasActive,dragging;
    private final Component[] labels={Component.translatable("category.minecrafteft.structures"),
            Component.translatable("category.minecrafteft.storage"),Component.translatable("category.minecrafteft.special"),
            Component.translatable("category.minecrafteft.medical"),Component.translatable("category.minecrafteft.backpacks")};
    private static final String[] ICON_NAMES={"structures","storage","special","medical","backpacks"};
    private static final ResourceLocation[] ICONS=java.util.Arrays.stream(ICON_NAMES)
            .map(name -> ResourceLocation.fromNamespaceAndPath(MinecraftEFTMod.MOD_ID,"textures/gui/categories/"+name+".png"))
            .toArray(ResourceLocation[]::new);

    private CreativeCategorySidebar(CreativeModeInventoryScreen screen) {
        super(screen.getGuiLeft()-45,screen.getGuiTop(),44,screen.getYSize(),Component.translatable("category.minecrafteft.title"));
        this.screen=screen;
        visible=false;
    }
    @SubscribeEvent public static void init(ScreenEvent.Init.Post event) {
        if(event.getScreen() instanceof CreativeModeInventoryScreen screen) {
            current=new CreativeCategorySidebar(screen);
            event.addListener(current);
        }
    }
    @SubscribeEvent public static void beforeRender(ScreenEvent.Render.Pre event) {
        if(current!=null && current.screen==event.getScreen()) current.updateTab();
    }
    @SubscribeEvent public static void afterRender(ScreenEvent.Render.Post event) {
        if(current==null || current.screen!=event.getScreen()) return;
        int category=current.categoryAt(event.getMouseX(),event.getMouseY());
        if(category>=0) event.getGuiGraphics().renderTooltip(Minecraft.getInstance().font,
                current.labels[category],event.getMouseX(),event.getMouseY());
    }
    @SubscribeEvent public static void closing(ScreenEvent.Closing event) {
        if(current!=null && current.screen==event.getScreen()) current=null;
    }
    @SubscribeEvent public static void wheel(ScreenEvent.MouseScrolled.Pre event) {
        // CreativeModeInventoryScreen consumes wheel events before child widgets see them.
        if(current!=null && current.screen==event.getScreen()
                && current.mouseScrolled(event.getMouseX(),event.getMouseY(),event.getScrollDelta())) event.setCanceled(true);
    }
    private void updateTab() {
        boolean active;
        try { active=SELECTED_TAB.get(null)==ModContent.MAIN_TAB.get() && screen.getCurrentPage().getVisibleTabs().contains(ModContent.MAIN_TAB.get()); }
        catch(IllegalAccessException error) { throw new IllegalStateException("Cannot read creative tab",error); }
        visible=active;
        if(active && !wasActive) { selected=-1;firstRow=0;applyFilter(); }
        if(!active) dragging=false;
        wasActive=active;
    }
    private void applyFilter() {
        var menu=screen.getMenu();
        menu.items.clear();
        for(ItemStack stack:ModContent.MAIN_TAB.get().getDisplayItems()) {
            int category=stack.getItem() instanceof net.minecraft.world.item.BlockItem item
                    && item.getBlock() instanceof com.minecrafteft.block.PersonalStashBlock?0:
                    (stack.getItem() instanceof com.minecrafteft.block.WeaponCaseItem item && !item.kind().marking().isEmpty())
                    || stack.getItem() instanceof com.minecrafteft.inventory.DogtagItem?2:
                    stack.getItem() instanceof com.minecrafteft.medical.MedikitItem || stack.getItem() instanceof com.minecrafteft.status.SimpleTreatmentItem?3:stack.is(ModContent.DAY_PACK.get())?4:1;
            if(selected<0 || selected==category) menu.items.add(stack.copy());
        }
        menu.scrollTo(0);
    }
    private int rows() { return Math.max(1,Math.min(labels.length,(height-12)/28)); }
    private int maxRow() { return labels.length-rows(); }
    private boolean inside(double x,double y) { return visible && x>=getX() && x<getX()+width && y>=getY() && y<getY()+height; }
    private int categoryAt(double x,double y) {
        if(!inside(x,y) || x<getX()+11 || x>=getX()+width-4 || y<getY()+6) return -1;
        int row=(int)(y-getY()-6)/28;
        return row<rows() && y<getY()+6+row*28+27?firstRow+row:-1;
    }
    @Override protected void renderWidget(GuiGraphics graphics,int mouseX,int mouseY,float partial) {
        int x=getX(),y=getY();
        graphics.fill(x,y,x+width,y+height,0xFF373737);
        graphics.fill(x+1,y+1,x+width-1,y+height-1,0xFFF4F4F4);
        graphics.fill(x+3,y+3,x+width-3,y+height-3,0xFFC6C6C6);
        int trackX=x+4,trackY=y+6,trackH=height-12;
        graphics.fill(trackX,trackY,trackX+4,trackY+trackH,0xFF777777);
        int thumb=Math.max(8,trackH*rows()/labels.length);
        int thumbY=trackY+(maxRow()==0?0:(trackH-thumb)*firstRow/maxRow());
        graphics.fill(trackX,thumbY,trackX+4,thumbY+thumb,maxRow()==0?0xFF999999:0xFFDDDDDD);
        for(int row=0;row<rows();row++) {
            int category=firstRow+row,by=y+6+row*28;
            boolean hover=mouseX>=x+11 && mouseX<x+width-4 && mouseY>=by && mouseY<by+27;
            graphics.fill(x+11,by,x+width-4,by+27,selected==category?0xFF638DA4:hover?0xFFAAAAAA:0xFF888888);
            graphics.fill(x+11,by,x+width-4,by+1,0xFFF0F0F0);
            graphics.fill(x+11,by+26,x+width-4,by+27,0xFF444444);
            graphics.setColor(1,1,1,1);
            CategoryIconRenderer.draw(graphics,ICON_NAMES[category],x+14,by+3,22,22);
        }
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        if(!inside(x,y)) return false;
        if(button!=0) return true;
        if(x<getX()+10) { dragging=maxRow()>0;scrollAt(y);return true; }
        int category=categoryAt(x,y);
        if(category>=0) {
            selected=selected==category?-1:category;
            setMessage(selected<0?Component.translatable("category.minecrafteft.title"):labels[selected]);
            playDownSound(Minecraft.getInstance().getSoundManager());
            applyFilter();
        }
        return true;
    }
    private void scrollAt(double y) {
        if(maxRow()>0) firstRow=net.minecraft.util.Mth.clamp((int)Math.round((y-getY()-6)/(height-12)*maxRow()),0,maxRow());
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy) {
        if(!dragging) return false;
        scrollAt(y);return true;
    }
    @Override public boolean mouseReleased(double x,double y,int button) {
        boolean handled=dragging || inside(x,y);dragging=false;return handled;
    }
    @Override public boolean mouseScrolled(double x,double y,double delta) {
        if(!inside(x,y)) return false;
        firstRow=net.minecraft.util.Mth.clamp(firstRow-(int)Math.signum(delta),0,maxRow());return true;
    }
    @Override public void onClick(double x,double y) { }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
}
