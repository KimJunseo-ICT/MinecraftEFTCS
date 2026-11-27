package com.minecrafteft.client;

import com.minecrafteft.MinecraftEFTMod;
import com.minecrafteft.registry.ModContent;
import com.minecrafteft.status.BloodMarkEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class BloodMarkRenderer extends EntityRenderer<BloodMarkEntity> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("minecraft:textures/block/redstone_dust_dot.png");
    public BloodMarkRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(BloodMarkEntity mark) { return TEXTURE; }
    @Override public void render(BloodMarkEntity mark,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        var random=new java.util.Random(mark.getUUID().getLeastSignificantBits());
        var consumer=buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));var matrix=pose.last().pose();
        for(int i=0;i<7;i++) {
            float x=(random.nextFloat()-.5F)*.6F,z=(random.nextFloat()-.5F)*.6F,r=.035F+random.nextFloat()*.11F;
            for(int corner=0;corner<4;corner++) {
                float u=corner==1 || corner==2?1:0,v=corner>=2?1:0;
                consumer.vertex(matrix,x+(u*2-1)*r,.001F*i,z+(v*2-1)*r).color(100,5,8,255)
                        .uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),0,1,0).endVertex();
            }
        }
    }
    @Mod.EventBusSubscriber(modid=MinecraftEFTMod.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class BloodRendererRegistration {
        @SubscribeEvent public static void registerBloodMarkRenderer(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModContent.BLOOD_MARK.get(),BloodMarkRenderer::new);
        }
    }
}
