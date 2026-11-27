package com.minecrafteft.client;

import com.minecrafteft.MinecraftEFTMod;
import com.minecrafteft.corpse.CorpseEntity;
import com.minecrafteft.inventory.LoadoutProvider;
import com.minecrafteft.registry.ModContent;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid=MinecraftEFTMod.MOD_ID,value=Dist.CLIENT)
public final class CorpseRenderer extends EntityRenderer<CorpseEntity> {
    private static final boolean YSM=ModList.get().getModContainerById("yes_steve_model")
            .map(m->m.getModInfo().getVersion().toString().startsWith("2.6.5")).orElse(false);
    private static final WeakHashMap<CorpseEntity,Avatar> world=new WeakHashMap<>(),previews=new WeakHashMap<>();
    public CorpseRenderer(EntityRendererProvider.Context context) { super(context); }
    static final class Avatar extends RemotePlayer {
        private ResourceLocation skin;
        private String skinModel;
        private int equipmentMask=-1;
        WorldPose worldPose;
        void syncEquipment(CorpseEntity body) {
            if (equipmentMask==0) return;
            for (EquipmentSlot slot:EquipmentSlot.values()) {
                setItemSlot(slot,ItemStack.EMPTY);
            }
            // This avatar is visual only. The real equipment remains in body.loot.
            getCapability(LoadoutProvider.CAPABILITY).ifPresent(s->s.trackedBackpackVisible=false);
            equipmentMask=0;
            worldPose=null;
        }
        Avatar(CorpseEntity body,boolean preview) {
            super((ClientLevel)body.level(),body.profile());
            var look=body.appearance();
            setPos(body.position());
            setOnGround(true);
            getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION,look.getByte("SkinParts"));
            setMainArm(look.getBoolean("LeftHanded")?HumanoidArm.LEFT:HumanoidArm.RIGHT);
            syncEquipment(body);
            xOld=xo=getX();yOld=yo=getY();zOld=zo=getZ();
            setYRot(0);setXRot(0);yRotO=yBodyRot=yBodyRotO=yHeadRot=yHeadRotO=xRotO=0;
            if (getPlayerInfo()==null && getGameProfile().getProperties().containsKey("textures"))
                Minecraft.getInstance().getSkinManager().registerSkins(body.profile(),(type,location,texture)->{
                    if (type==MinecraftProfileTexture.Type.SKIN) {
                        skin=location;skinModel="slim".equals(texture.getMetadata("model"))?"slim":"default";
                    }
                },false);
            if (preview) { setYRot(180);yRotO=yBodyRot=yBodyRotO=yHeadRot=yHeadRotO=180; }
            if (YSM) Ysm265Corpse.select(this,preview?look.getString("Model"):"",look.getString("Texture"));
        }
        @Override public ResourceLocation getSkinTextureLocation() { return skin==null?super.getSkinTextureLocation():skin; }
        @Override public String getModelName() { return skinModel==null?super.getModelName():skinModel; }
        @Override public boolean isSpectator() { return false; }
    }
    @Mod.EventBusSubscriber(modid=MinecraftEFTMod.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class CorpseClientRegistration {
        @SubscribeEvent public static void registerCorpseRenderer(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModContent.CORPSE.get(),CorpseRenderer::new);
        }
        // Forge 6.0.x wrapper names omit the enclosing type; keep both names distinct from LoadoutClient.
        @SubscribeEvent public static void registerCorpseInteractionOverlay(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("corpse_interaction",(gui,g,partial,width,height)-> {
                var mc=Minecraft.getInstance();
                if(mc.player==null || mc.screen!=null || mc.options.hideGui
                        || !(mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit)
                        || !(hit.getEntity() instanceof CorpseEntity body) || !body.canLoot(mc.player)) return;
                var label=net.minecraft.network.chat.Component.translatable("corpse.minecrafteft.interact");
                g.pose().pushPose();
                try {
                    g.pose().translate(width/2F,height/2F+12,150);
                    g.pose().scale(.8F,.8F,1);
                    g.drawString(mc.font,label,-mc.font.width(label)/2,0,0xFFE8EDF2,true);
                    g.flush();
                } finally { g.pose().popPose(); }
            });
        }
    }
    @SubscribeEvent public static void onUnload(net.minecraftforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel) { world.clear();previews.clear(); }
    }
    @SubscribeEvent public static void name(RenderNameTagEvent event) {
        if (event.getEntity() instanceof Avatar) event.setResult(Event.Result.DENY);
    }
    public static boolean ownerOutline(CorpseEntity body) {
        var viewer=Minecraft.getInstance().player;
        return viewer!=null && viewer.getUUID().equals(body.owner()) && viewer.distanceToSqr(body)<=10000;
    }
    public static void preview(GuiGraphics g,CorpseEntity body,int x,int feet,int scale) {
        if (body==null) return;
        Avatar avatar=previews.computeIfAbsent(body,b->new Avatar(b,true));
        avatar.syncEquipment(body);
        InventoryScreen.renderEntityInInventory(g,x,feet,scale,Axis.ZP.rotationDegrees(180),new org.joml.Quaternionf(),avatar);
    }
    @Override public ResourceLocation getTextureLocation(CorpseEntity body) {
        return world.computeIfAbsent(body,b->new Avatar(b,false)).getSkinTextureLocation();
    }
    interface WorldPose {
        boolean draw(PoseStack pose,MultiBufferSource buffers,int light);
    }
    @Override public void render(CorpseEntity body,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        Avatar avatar=world.computeIfAbsent(body,b->new Avatar(b,false));
        avatar.syncEquipment(body);
        pose.pushPose();
        try {
            pose.mulPose(Axis.YP.rotationDegrees(-body.getYRot()));
            var look=body.appearance();
            if (YSM && !look.getString("Model").isEmpty()) {
                if (avatar.worldPose==null) avatar.worldPose=Ysm265Corpse.create(avatar,look.getString("Model"),look.getString("Texture"));
                if (avatar.worldPose!=null && avatar.worldPose.draw(pose,buffers,light)) return;
            }
            pose.translate(0,.26,-.9);
            pose.mulPose(Axis.XP.rotationDegrees(90));
            entityRenderDispatcher.getRenderer(avatar).render(avatar,0,0,pose,buffers,light);
        } finally { pose.popPose(); }
    }
}
