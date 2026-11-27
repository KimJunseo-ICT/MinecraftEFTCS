package com.minecrafteft.client;

import com.minecrafteft.block.WeaponCaseKind;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Uses the same lid, hardware and tilted marking for held and inventory case items. */
public final class WeaponCaseItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static WeaponCaseItemRenderer instance;
    private WeaponCaseItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());
    }
    public static WeaponCaseItemRenderer instance() {
        if(instance==null) instance=new WeaponCaseItemRenderer();
        return instance;
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose();
        pose.translate(.024,.388,.276);
        pose.scale(.56F,.56F,.56F);
        WeaponCaseRenderer.renderCaseModel(pose,buffers,WeaponCaseKind.of(stack),0,light);
        pose.popPose();
    }
}
