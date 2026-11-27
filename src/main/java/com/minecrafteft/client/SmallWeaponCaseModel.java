package com.minecrafteft.client;

import com.minecrafteft.block.WeaponCaseKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** 1.7 x .8 x .4 rifle case; the rear hinge carries the entire lid and its marking. */
final class SmallWeaponCaseModel {
    static void render(PoseStack p, MultiBufferSource buffers, WeaponCaseKind kind, float open, int light) {
        VertexConsumer v=buffers.getBuffer(RenderType.entityCutoutNoCull(WeaponCaseRenderer.TEXTURE));
        box(p,v,.025F,0,.025F,1.675F,.055F,.775F,1,light);
        box(p,v,.025F,.055F,.025F,1.675F,.265F,.085F,0,light);
        box(p,v,.025F,.055F,.715F,1.675F,.265F,.775F,0,light);
        box(p,v,.025F,.055F,.085F,.085F,.265F,.715F,0,light);
        box(p,v,1.615F,.055F,.085F,1.675F,.265F,.715F,0,light);
        box(p,v,.085F,.075F,.085F,1.615F,.105F,.715F,3,light);
        // Black rim, reinforced end caps, raised ribs and four gold latch catches.
        // Four rim strips leave a real opening above the foam and stored model.
        box(p,v,.015F,.255F,.015F,1.685F,.28F,.085F,1,light);
        box(p,v,.015F,.255F,.715F,1.685F,.28F,.785F,1,light);
        box(p,v,.015F,.255F,.085F,.085F,.28F,.715F,1,light);
        box(p,v,1.615F,.255F,.085F,1.685F,.28F,.715F,1,light);
        // Inset frame and proud white panel have distinct exterior planes.
        box(p,v,.004F,.01F,.01F,.10F,.275F,.79F,1,light);
        box(p,v,0,.04F,.045F,.02F,.25F,.755F,0,light);
        box(p,v,1.60F,.01F,.01F,1.696F,.275F,.79F,1,light);
        box(p,v,1.68F,.04F,.045F,1.70F,.25F,.755F,0,light);
        for(float x:new float[]{.09F,.45F,1.15F,1.53F}) {
            box(p,v,x,0,0,x+.065F,.27F,.045F,1,light);
            box(p,v,x,0,.755F,x+.065F,.27F,.80F,1,light);
        }
        // A hollow, recessed carry handle rather than a solid rectangular badge.
        box(p,v,.69F,.115F,-.012F,.72F,.22F,.025F,2,light);
        box(p,v,.98F,.115F,-.012F,1.01F,.22F,.025F,2,light);
        box(p,v,.71F,.10F,-.016F,.99F,.13F,.02F,1,light);
        for(float x:new float[]{.20F,.80F,1.40F})
            box(p,v,x,.235F,.75F,x+.10F,.29F,.80F,1,light);
        p.pushPose();
        p.translate(0,.28F,.80F);
        p.mulPose(Axis.XP.rotationDegrees(open*100));
        p.translate(0,0,-.80F);
        box(p,v,0,0,0,1.70F,.10F,.80F,0,light);
        box(p,v,.035F,-.012F,.035F,1.665F,0,.765F,1,light);
        box(p,v,.08F,-.019F,.08F,1.62F,-.013F,.72F,3,light);
        for(float x:new float[]{.07F,.43F,1.13F,1.52F})
            box(p,v,x,.10F,.047F,x+.085F,.12F,.753F,1,light);
        for(float z:new float[]{.015F,.755F})
            box(p,v,.015F,.10F,z,1.685F,.12F,z+.03F,0,light);
        // The catches belong to the lid. Their free ends flip above the raised lid edge.
        for(float x:new float[]{.09F,.45F,1.15F,1.53F}) {
            p.pushPose();
            p.translate(x+.0325F,0,.015F);
            p.mulPose(Axis.XP.rotationDegrees(open*65));
            box(p,v,-.026F,-.11F,-.018F,.026F,.035F,.015F,2,light);
            box(p,v,-.012F,-.075F,-.023F,.012F,-.025F,-.018F,1,light);
            p.popPose();
        }
        // Original transparent PNGs are rotated on the 3D lid, preserving their artwork.
        if(!kind.marking().isEmpty()) {
            VertexConsumer mark=buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath(
                    "minecrafteft","textures/block/markings/"+kind.marking()+".png")));
            p.pushPose();
            p.translate(.85F,.103F,.40F);
            p.mulPose(Axis.YP.rotationDegrees(15));
            vertex(p,mark,-.18F,-.18F,1,1,light);
            vertex(p,mark,.18F,-.18F,0,1,light);
            vertex(p,mark,.18F,.18F,0,0,light);
            vertex(p,mark,-.18F,.18F,1,0,light);
            p.popPose();
        }
        p.popPose();
    }
    private static void vertex(PoseStack p,VertexConsumer v,float x,float z,float u,float uvY,int light) {
        v.vertex(p.last().pose(),x,0,z).color(255,255,255,255).uv(u,uvY)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.last().normal(),0,1,0).endVertex();
    }
    private static void box(PoseStack p,VertexConsumer v,float x1,float y1,float z1,float x2,float y2,float z2,int material,int light) {
        ContainerStashRenderer.cuboid(p,v,x1,y1,z1,x2,y2,z2,material,0,light);
    }
}
