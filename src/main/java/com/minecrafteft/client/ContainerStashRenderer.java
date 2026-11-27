package com.minecrafteft.client;

import com.minecrafteft.block.PersonalStashBlock;
import com.minecrafteft.block.PersonalStashBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Solid decorative container cluster; its linked blocks supply collision and interaction. */
public final class ContainerStashRenderer implements BlockEntityRenderer<PersonalStashBlockEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "minecrafteft", "textures/block/container_atlas.png");

    public ContainerStashRenderer(BlockEntityRendererProvider.Context context) { }

    @Override public void render(PersonalStashBlockEntity entity, float partialTick, PoseStack pose,
                                  MultiBufferSource buffers, int light, int overlay) {
        if (!(entity.getBlockState().getBlock() instanceof PersonalStashBlock block)) return;
        var facing = entity.getBlockState().getValue(PersonalStashBlock.FACING);
        float angle = switch (facing) { case EAST -> -90; case SOUTH -> 180; case WEST -> 90; default -> 0; };
        boolean open = entity.getBlockState().getValue(PersonalStashBlock.OPEN);
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.translate(-0.5, 0, -0.5);
        for (int i = 0; i < Math.min(3, block.tier().level()); i++) {
            pose.pushPose();
            pose.translate(i * 3, 0, 0);
            container(pose, vertices, i, open, light);
            pose.popPose();
        }
        if (block.tier().level() == 4) {
            pose.pushPose();
            pose.translate(5, 3, 1);
            pose.mulPose(Axis.YP.rotationDegrees(-90));
            container(pose, vertices, 3, open, light);
            pose.popPose();
            stairs(pose, vertices, light);
            if(entity.getBlockState().getValue(PersonalStashBlock.REST_AREA))
                StashRestAreaRenderer.render(entity,pose,buffers,light);
        }
        pose.popPose();
    }

    private static void container(PoseStack pose, VertexConsumer v, int material, boolean open, int light) {
        cuboid(pose, v, .04F, .04F, .04F, 2.96F, 2.96F, 4.96F, material, open ? 3 : 2, light);
        // Raised corner posts, roof rails and vertical corrugations give the silhouette depth.
        for (float x : new float[]{.02F, 2.86F}) for (float z : new float[]{.02F, 4.86F})
            cuboid(pose, v, x, 0, z, x + .12F, 3, z + .12F, material, 0, light);
        for (float y : new float[]{0, 2.86F}) {
            for (float x : new float[]{.02F, 2.86F})
                cuboid(pose, v, x, y, .14F, x + .12F, y + .14F, 4.86F, material, 0, light,3);
            for (float z : new float[]{.02F, 4.86F})
                cuboid(pose, v, .14F, y, z, 2.86F, y + .14F, z + .12F, material, 0, light,12);
        }
        for (int i = 0; i < 14; i++) {
            float z = .28F + i * .33F;
            for (float x : new float[]{.025F, 2.92F})
                cuboid(pose, v, x, .18F, z, x + .055F, 2.82F, z + .09F, material, 0, light);
        }
        for (int i = 0; i < 9; i++) {
            float x = .22F + i * .30F;
            cuboid(pose, v, x, 2.93F, .20F, x + .08F, 2.99F, 4.80F, material, 1, light);
        }
    }

    private static void stairs(PoseStack pose, VertexConsumer v, int light) {
        for (int z = 0; z < 3; z++) {
            cuboid(pose, v, 9.02F, z + .38F, z, 9.98F, z + .5F, z + .5F, 2, 0, light);
            cuboid(pose, v, 9.02F, z + .88F, z + .5F, 9.98F, z + 1F, z + 1F, 2, 0, light);
        }
        // Continuous sloped handrails. Slightly wider sockets cover the post/rail join
        // without either coplanar exterior faces or visible gaps.
        for(float x:new float[]{9.06F,9.87F}) {
            pose.pushPose();
            pose.translate(0,1.12F,.12F);
            pose.mulPose(Axis.XP.rotationDegrees(-45));
            cuboid(pose,v,x,-.04F,0,x+.07F,.04F,(float)(2.90*Math.sqrt(2)),2,0,light);
            pose.popPose();
            for(float z:new float[]{.35F,1.35F,2.35F}) {
                float top=z+1;
                cuboid(pose,v,x,0,z-.035F,x+.07F,top-.015F,z+.035F,3,0,light,16);
                cuboid(pose,v,x-.015F,top-.09F,z-.09F,x+.085F,top+.09F,z+.09F,2,0,light);
            }
        }
        cuboid(pose, v, 9, 2.85F, 3, 10, 3, 5, 2, 0, light);
        cuboid(pose, v, 9.87F, 3.96F, 3.00F, 9.94F, 4.04F, 5, 2, 0, light);
        // Smooth connection from the sloped left handrail to the container roof.
        cuboid(pose,v,9.06F,3.96F,3.00F,9.13F,4.04F,3.32F,2,0,light);
        cuboid(pose,v,9.06F,3,3.045F,9.13F,3.985F,3.115F,3,0,light,48);
        cuboid(pose,v,9.045F,3.91F,2.99F,9.145F,4.09F,3.17F,2,0,light);
        for (float z : new float[]{3.08F,4.88F}) {
            cuboid(pose,v,9.87F,3,z-.035F,9.94F,3.985F,z+.035F,3,0,light,48);
            cuboid(pose,v,9.855F,3.91F,z-.09F,9.955F,4.09F,z+.09F,2,0,light);
        }
    }

    static void cuboid(PoseStack p, VertexConsumer v, float x1, float y1, float z1,
                               float x2, float y2, float z2, int material, int front, int light) {
        cuboid(p,v,x1,y1,z1,x2,y2,z2,material,front,light,0);
    }

    private static void cuboid(PoseStack p, VertexConsumer v, float x1,float y1,float z1,
                               float x2,float y2,float z2,int material,int front,int light,int omit) {
        if((omit&1)==0) quad(p,v,material,front,light,0,0,-1,1F, x1,y2,z1, x2,y2,z1, x2,y1,z1, x1,y1,z1);
        if((omit&2)==0) quad(p,v,material,0,light,0,0,1,.80F, x2,y2,z2, x1,y2,z2, x1,y1,z2, x2,y1,z2);
        if((omit&4)==0) quad(p,v,material,0,light,-1,0,0,.85F, x1,y2,z2, x1,y2,z1, x1,y1,z1, x1,y1,z2);
        if((omit&8)==0) quad(p,v,material,0,light,1,0,0,.9F, x2,y2,z1, x2,y2,z2, x2,y1,z2, x2,y1,z1);
        if((omit&16)==0) quad(p,v,material,1,light,0,1,0,1F, x1,y2,z2, x2,y2,z2, x2,y2,z1, x1,y2,z1);
        if((omit&32)==0) quad(p,v,material,0,light,0,-1,0,.6F, x1,y1,z1, x2,y1,z1, x2,y1,z2, x1,y1,z2);
    }

    private static void quad(PoseStack p, VertexConsumer v, int material, int tile, int light,
                             float nx, float ny, float nz, float shade, float... xyz) {
        float u0 = (tile * 64 + .5F) / 256F;
        float u1 = (tile * 64 + 63.5F) / 256F;
        float v0 = (material * 64 + .5F) / 256F;
        float v1 = (material * 64 + 63.5F) / 256F;
        int color = (int) (255 * shade);
        for (int i = 0; i < 4; i++) {
            float u = i == 0 || i == 3 ? u0 : u1;
            float uvY = i < 2 ? v0 : v1;
            v.vertex(p.last().pose(), xyz[i*3], xyz[i*3+1], xyz[i*3+2])
                    .color(color,color,color,255).uv(u,uvY).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(light).normal(p.last().normal(),nx,ny,nz).endVertex();
        }
    }

    @Override public int getViewDistance() { return 128; }
}
