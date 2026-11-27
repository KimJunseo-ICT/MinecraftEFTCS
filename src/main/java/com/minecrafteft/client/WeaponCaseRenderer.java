package com.minecrafteft.client;

import com.minecrafteft.block.WeaponCaseBlock;
import com.minecrafteft.block.WeaponCaseBlockEntity;
import com.minecrafteft.block.WeaponCaseKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Shared hard-case geometry for the world renderer and the fitted inventory preview. */
public final class WeaponCaseRenderer implements BlockEntityRenderer<WeaponCaseBlockEntity> {
    static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecrafteft", "textures/block/weapon_case_atlas.png");
    public WeaponCaseRenderer(BlockEntityRendererProvider.Context context) { }
    @Override public void render(WeaponCaseBlockEntity entity, float partial, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        WeaponCaseKind kind=((WeaponCaseBlock)entity.getBlockState().getBlock()).kind();
        float angle = switch (entity.getBlockState().getValue(WeaponCaseBlock.FACING)) {
            case EAST -> -90; case SOUTH -> 180; case WEST -> 90; default -> 0;
        };
        pose.pushPose();
        pose.translate(.5, 0, .5);
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.translate(-.5, 0, -.5);
        if (entity.getBlockState().getValue(WeaponCaseBlock.CENTERED)) pose.translate(kind.small()?-.35F:-.85F, 0, 0);
        renderCaseModel(pose, buffers, kind, entity.lid(partial), light, entity.displayGun());
        pose.popPose();
    }

    static void renderCaseModel(PoseStack pose,MultiBufferSource buffers,WeaponCaseKind kind,float lid,int light) {
        renderCaseModel(pose,buffers,kind,lid,light,ItemStack.EMPTY);
    }

    static void renderCaseModel(PoseStack pose,MultiBufferSource buffers,WeaponCaseKind kind,float lid,int light,ItemStack displayGun) {
        if(kind.small()) SmallWeaponCaseModel.render(pose,buffers,kind,lid,light);
        else renderModel(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),lid,light);
        if(!displayGun.isEmpty() && lid > .02F) CaseStoredGunRenderer.render(pose,buffers,kind,displayGun,light);
    }

    static void renderModel(PoseStack pose, VertexConsumer v, float lid, int light) {
        // Hollow lower shell: the foam bed becomes visible as the lid moves aside.
        box(pose,v,.04F,.04F,.04F,2.66F,.20F,1.56F,1,light);
        box(pose,v,.06F,.20F,.06F,2.64F,.88F,.17F,0,light);
        box(pose,v,.06F,.20F,1.43F,2.64F,.88F,1.54F,0,light);
        box(pose,v,.06F,.20F,.17F,.17F,.88F,1.43F,0,light);
        box(pose,v,2.53F,.20F,.17F,2.64F,.88F,1.43F,0,light);
        box(pose,v,.17F,.22F,.17F,2.53F,.30F,1.43F,3,light);
        // Reinforced corners and front/back ribs keep the reference hard-case silhouette.
        for (float x : new float[]{0,2.48F}) for (float z : new float[]{0,1.38F})
            box(pose,v,x,0,z,x+.22F,.94F,z+.22F,0,light);
        for (float x : new float[]{.30F,1.25F,2.20F}) for (float z : new float[]{.015F,1.49F})
            box(pose,v,x,.08F,z,x+.16F,.93F,z+.095F,0,light);
        box(pose,v,.10F,.87F,.10F,2.60F,.96F,.18F,1,light);
        box(pose,v,.10F,.87F,1.42F,2.60F,.96F,1.50F,1,light);
        box(pose,v,.10F,.87F,.18F,.18F,.96F,1.42F,1,light);
        box(pose,v,2.52F,.87F,.18F,2.60F,.96F,1.42F,1,light);
        box(pose,v,.20F,.92F,.096F,2.50F,.945F,.099F,2,light);
        // Inset black handle with thin gold frame.
        box(pose,v,1.10F,.35F,.005F,1.60F,.64F,.045F,2,light);
        box(pose,v,1.14F,.39F,0,1.56F,.60F,.05F,1,light);
        box(pose,v,1.18F,.43F,-.005F,1.52F,.56F,.045F,0,light);
        for (float x : new float[]{.35F,1.30F,2.25F}) {
            pose.pushPose();
            pose.translate(x,.85,.06);
            pose.mulPose(Axis.XP.rotationDegrees(lid * 110));
            box(pose,v,-.065F,-.12F,-.055F,.065F,.10F,.015F,2,light);
            box(pose,v,-.025F,-.07F,-.065F,.025F,-.01F,-.045F,1,light);
            pose.popPose();
        }
        // Slide along the rim, without lifting or tilting the lid.
        pose.pushPose();
        pose.translate(.22F * lid, 1.0F, 0);
        box(pose,v,0,-.045F,0,2.70F,.12F,1.60F,0,light);
        box(pose,v,.09F,-.06F,.09F,2.61F,-.035F,1.51F,1,light);
        for (float x : new float[]{.02F,.84F,1.66F,2.48F})
            box(pose,v,x,.11F,.185F,x+.20F,.20F,1.415F,0,light);
        for (float z : new float[]{.03F,1.42F})
            box(pose,v,.02F,.11F,z,2.68F,.20F,z+.15F,0,light);
        box(pose,v,.26F,.122F,.69F,2.44F,.132F,.77F,1,light);
        box(pose,v,1.16F,.133F,.685F,1.54F,.14F,.775F,2,light);
        pose.popPose();
    }
    private static void box(PoseStack p, VertexConsumer v, float x1,float y1,float z1,
                            float x2,float y2,float z2,int material,int light) {
        ContainerStashRenderer.cuboid(p,v,x1,y1,z1,x2,y2,z2,material,0,light);
    }
    @Override public int getViewDistance() { return 96; }
}
