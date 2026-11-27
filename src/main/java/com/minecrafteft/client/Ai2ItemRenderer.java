package com.minecrafteft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class Ai2ItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static Ai2ItemRenderer instance;
    private Ai2ItemRenderer() { super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()); }
    public static Ai2ItemRenderer instance() { if(instance==null) instance=new Ai2ItemRenderer();return instance; }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose();
        pose.translate(.5,.5,.5);
        float scale=context==ItemDisplayContext.GUI?2.45F:1.65F;
        pose.scale(scale,scale,scale);
        if(context==ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(10));
            pose.mulPose(Axis.YP.rotationDegrees(-22));
            pose.mulPose(Axis.ZP.rotationDegrees(-8));
        }
        if (stack.getItem() instanceof com.minecrafteft.medical.MedikitItem kit
                && kit.type == com.minecrafteft.medical.MedikitType.IFAK) {
            IfakKitModel.pouch(pose, buffers, light, 0);
        } else {
            Ai2KitModel.kit(pose,buffers,0,light);
        }
        pose.popPose();
    }
}
