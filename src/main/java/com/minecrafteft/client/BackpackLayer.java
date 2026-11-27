package com.minecrafteft.client;

import com.minecrafteft.MinecraftEFTMod;
import com.minecrafteft.inventory.LoadoutState;
import com.minecrafteft.registry.ModContent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=MinecraftEFTMod.MOD_ID, value=Dist.CLIENT)
public final class BackpackLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final boolean YSM_265 = ModList.get().getModContainerById("yes_steve_model")
            .map(mod -> "2.6.5-forge+mc1.20.1".equals(mod.getModInfo().getVersion().toString())).orElse(false);
    private static ItemStack exterior;

    private BackpackLayer(PlayerRenderer renderer) { super(renderer); }

    @Mod.EventBusSubscriber(modid=MinecraftEFTMod.MOD_ID, value=Dist.CLIENT, bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers event) {
            for (String skin : event.getSkins()) {
                PlayerRenderer renderer = event.getSkin(skin);
                if (renderer != null) renderer.addLayer(new BackpackLayer(renderer));
            }
        }
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void beforePlayer(RenderPlayerEvent.Pre event) {
        if (YSM_265 && visible(event.getEntity())) Ysm265BackpackLayer.install();
    }

    public static void update(int id, boolean visible) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(id) instanceof Player player)
            player.getCapability(com.minecrafteft.inventory.LoadoutProvider.CAPABILITY).ifPresent(state -> state.trackedBackpackVisible = visible);
    }
    static boolean visible(Player player) {
        if (player instanceof CorpseRenderer.Avatar || !player.isAlive() || player.isInvisible() || player.isSpectator()) return false;
        return player.getCapability(com.minecrafteft.inventory.LoadoutProvider.CAPABILITY)
                .map(state -> player == Minecraft.getInstance().player ? state.backpackVisible() : state.trackedBackpackVisible).orElse(false);
    }
    @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                 float swing, float amount, float partial, float age, float yaw, float pitch) {
        if (!visible(player)) return;
        pose.pushPose();
        try {
            getParentModel().body.translateAndRotate(pose);
            draw(pose,buffers,light,player);
        } finally { pose.popPose(); }
    }
    /** Input frame has its origin at shoulder height and positive Z pointing behind the torso. */
    static void draw(PoseStack pose, MultiBufferSource buffers, int light, Player player) {
        if (exterior == null) exterior = new ItemStack(ModContent.DAY_PACK.get());
        double armorOffset = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).isEmpty() ? 0 : .045;
        pose.translate(0,.37,.235 + armorOffset);
        pose.mulPose(Axis.ZP.rotationDegrees(180));
        pose.scale(.88F,.88F,.88F);
        Minecraft.getInstance().getItemRenderer().renderStatic(exterior,ItemDisplayContext.NONE,light,
                OverlayTexture.NO_OVERLAY,pose,buffers,player.level(),player.getId());
    }
}
