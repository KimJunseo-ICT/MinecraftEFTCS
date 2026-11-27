package com.minecrafteft.client;

import com.minecrafteft.MinecraftEFTMod;
import com.minecrafteft.inventory.*;
import com.minecrafteft.network.*;
import com.minecrafteft.registry.ModContent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid=MinecraftEFTMod.MOD_ID,value=Dist.CLIENT)
public final class LoadoutClient {
    private static final java.lang.reflect.Field TAB=ObfuscationReflectionHelper.findField(CreativeModeInventoryScreen.class,"f_98507_");
    public static final KeyMapping MELEE=new KeyMapping("key.minecrafteft.melee",KeyConflictContext.IN_GAME,InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_U,"key.categories.minecrafteft");
    private static Button creativeButton;
    private static CreativeBackpackSlot creativeBag;
    private static CreativeModeInventoryScreen creativeScreen;
    public static void accept(CompoundTag data) { var player=Minecraft.getInstance().player;if(player!=null && data!=null) player.getCapability(LoadoutProvider.CAPABILITY).ifPresent(state -> state.load(data)); }
    public static void vanilla() {
        var mc=Minecraft.getInstance();if(mc.player==null || !mc.player.isCreative()) return;
        LoadoutState.of(mc.player).creativeCustom=false;ModNetwork.CHANNEL.sendToServer(new LoadoutActionPacket(LoadoutActionPacket.VANILLA,0));
        mc.player.closeContainer();mc.setScreen(new InventoryScreen(mc.player));
    }
    @SubscribeEvent public static void opening(ScreenEvent.Opening event) {
        var mc=Minecraft.getInstance();if(mc.player==null || mc.player.isSpectator()) return;
        if(event.getNewScreen() instanceof InventoryScreen && LoadoutState.custom(mc.player)) {
            event.setCanceled(true);ModNetwork.CHANNEL.sendToServer(new LoadoutActionPacket(LoadoutActionPacket.OPEN,0));
        }
    }
    @SubscribeEvent public static void init(ScreenEvent.Init.Post event) {
        if(!(event.getScreen() instanceof CreativeModeInventoryScreen screen)) return;
        creativeScreen=screen;
        creativeBag=new CreativeBackpackSlot(screen);event.addListener(creativeBag);
        creativeButton=Button.builder(Component.literal("E"),button->ModNetwork.CHANNEL.sendToServer(new LoadoutActionPacket(LoadoutActionPacket.OPEN,0)))
                .bounds(screen.getGuiLeft()+7,screen.getGuiTop()+20,18,18).tooltip(Tooltip.create(Component.translatable("loadout.minecrafteft.switch_custom"))).build();
        creativeButton.visible=false;event.addListener(creativeButton);
    }
    @SubscribeEvent public static void screen(ScreenEvent.Render.Pre event) {
        if(creativeButton==null || event.getScreen()!=creativeScreen) return;
        try { creativeBag.visible=creativeButton.visible=((CreativeModeTab)TAB.get(null)).getType()==CreativeModeTab.Type.INVENTORY; }
        catch(IllegalAccessException error) { throw new IllegalStateException(error); }
    }
    @SubscribeEvent public static void creativeTooltip(ScreenEvent.Render.Post event) {
        if (event.getScreen() == creativeScreen && creativeBag != null)
            creativeBag.drawTooltip(event.getGuiGraphics(),event.getMouseX(),event.getMouseY());
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        var mc=Minecraft.getInstance();if(event.phase!=TickEvent.Phase.END || mc.player==null || mc.screen!=null) return;
        while(MELEE.consumeClick()) if(LoadoutState.custom(mc.player)) ModNetwork.CHANNEL.sendToServer(new LoadoutActionPacket(LoadoutActionPacket.MELEE,0));
    }
    @SubscribeEvent public static void key(InputEvent.Key event) {
        var mc=Minecraft.getInstance();if(event.getAction()!=GLFW.GLFW_PRESS || mc.player==null || mc.screen!=null
                || !mc.player.isAlive() || !mc.player.getCapability(LoadoutProvider.CAPABILITY).isPresent()) return;
        if(LoadoutState.custom(mc.player) && mc.options.keySwapOffhand.matches(event.getKey(),event.getScanCode())
                && !LoadoutRules.offhand(mc.player.getMainHandItem())) {
            while(mc.options.keySwapOffhand.consumeClick()) { }
            mc.options.keySwapOffhand.setDown(false);
            mc.player.displayClientMessage(Component.translatable("message.minecrafteft.offhand_no_weapons"),true);
            return;
        }
        if(LoadoutState.of(mc.player).meleeHost<0) return;
        if(mc.options.keySwapOffhand.matches(event.getKey(),event.getScanCode())) ModNetwork.CHANNEL.sendToServer(new LoadoutActionPacket(LoadoutActionPacket.SELECT,mc.player.getInventory().selected));
        for(int i=0;i<9;i++) if(mc.options.keyHotbarSlots[i].matches(event.getKey(),event.getScanCode())) ModNetwork.CHANNEL.sendToServer(new LoadoutActionPacket(LoadoutActionPacket.SELECT,i));
    }
    @SubscribeEvent public static void closing(ScreenEvent.Closing event) {
        if(event.getScreen()==creativeScreen) { creativeScreen=null;creativeButton=null;creativeBag=null; }
    }
    @SubscribeEvent public static void hideHotbar(RenderGuiOverlayEvent.Pre event) {
        var mc=Minecraft.getInstance();if(mc.player!=null && !mc.player.isSpectator() && LoadoutState.custom(mc.player) && event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) event.setCanceled(true);
    }
    @Mod.EventBusSubscriber(modid=MinecraftEFTMod.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(MELEE); }
        @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
            event.enqueueWork(()-> {
                MenuScreens.register(ModContent.LOADOUT_MENU.get(),LoadoutScreen::new);
                MenuScreens.register(ModContent.CORPSE_MENU.get(),CorpseScreen::new);
                MenuScreens.register(ModContent.PORTABLE_BACKPACK_MENU.get(),PortableBackpackScreen::new);
            });
        }
        @SubscribeEvent public static void overlay(RegisterGuiOverlaysEvent event) {
            event.registerAbove(VanillaGuiOverlay.HOTBAR.id(),"loadout_hotbar",(gui,g,partial,width,height)-> {
                var mc=Minecraft.getInstance();if(mc.player==null || mc.options.hideGui || mc.player.isSpectator() || !LoadoutState.custom(mc.player)) return;
                var state=LoadoutState.of(mc.player);int left=width/2-90,top=height-21;
                for(int i=0;i<9;i++) {
                    int x=left+i*20;LoadoutPanel.box(g,x,top,19,20);
                    var stack=state.meleeHost==i?state.equipment.getStackInSlot(LoadoutState.MELEE):mc.player.getInventory().getItem(i);
                    g.renderItem(stack,x+1,top+2);g.renderItemDecorations(mc.font,stack,x+1,top+2);
                    LoadoutPanel.small(g,Integer.toString(i+1),x+2,top+1,.5F,0xFFCFD9E0);
                    if(state.meleeHost<0 && mc.player.getInventory().selected==i) g.fill(x,top+18,x+19,top+20,0xFFB8D9BD);
                }
                int x=left+186;LoadoutPanel.box(g,x,top,19,20);
                var melee=state.meleeHost>=0?mc.player.getInventory().getItem(state.meleeHost):state.equipment.getStackInSlot(LoadoutState.MELEE);
                g.renderItem(melee,x+1,top+2);LoadoutPanel.small(g,"U",x+2,top+1,.5F,0xFFCFD9E0);
                if(state.meleeHost>=0) g.fill(x,top+18,x+19,top+20,0xFFB8D9BD);
                if(!mc.player.getOffhandItem().isEmpty()) { LoadoutPanel.box(g,left-25,top,19,20);g.renderItem(mc.player.getOffhandItem(),left-24,top+2);LoadoutPanel.small(g,"F",left-23,top+1,.5F,0xFFCFD9E0); }
            });
        }
    }
}
