package com.minecrafteft.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.minecrafteft.MinecraftEFTMod;
import com.minecrafteft.network.ModNetwork;
import com.minecrafteft.network.OpenStashPacket;
import com.minecrafteft.registry.ModContent;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = MinecraftEFTMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class StashClientEvents {
    public static final KeyMapping OPEN_STASH = new KeyMapping(
            "key.minecrafteft.open_stash", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, "key.categories.minecrafteft");

    private StashClientEvents() {}

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_STASH);
    }

    @SubscribeEvent
    public static void registerRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModContent.STASH_SEAT.get(),context->new net.minecraft.client.renderer.entity.EntityRenderer<com.minecrafteft.block.StashSeatEntity>(context) {
            @Override public net.minecraft.resources.ResourceLocation getTextureLocation(com.minecrafteft.block.StashSeatEntity entity) {
                return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
            }
        });
        event.registerBlockEntityRenderer(ModContent.STASH_BLOCK_ENTITY.get(), ContainerStashRenderer::new);
        event.registerBlockEntityRenderer(ModContent.WEAPON_CASE_ENTITY.get(), WeaponCaseRenderer::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(
                ModContent.STASH_MENU.get(), StashScreen::new));
    }

    @Mod.EventBusSubscriber(modid = MinecraftEFTMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ClientForgeEvents {
        private ClientForgeEvents() {}

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END && OPEN_STASH.consumeClick()
                    && Minecraft.getInstance().screen == null) {
                ModNetwork.CHANNEL.sendToServer(new OpenStashPacket());
            }
        }
    }
}
