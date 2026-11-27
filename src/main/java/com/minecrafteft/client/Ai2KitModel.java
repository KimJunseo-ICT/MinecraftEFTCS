package com.minecrafteft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import static com.minecrafteft.client.KitMesh.*;

/** All dimensions and material coordinates are baked once. Moving parts only change their pose. */
final class Ai2KitModel {
    private static final int COVER = 0, LINING = 1, PLASTIC = 2, TRAY = 3, IVORY = 4,
            RED = 5, STEEL = 6, RUBBER = 7, TEAL = 8, AMBER = 9, HONEY = 10;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecrafteft", "textures/item/ai2_atlas.png");
    private static final Vertex[] SHELL = shell(), LID = lid(), CONTENTS = contents(),
            INJECTOR = injector(), PISTON = piston();

    private Ai2KitModel() {}

    static void kit(PoseStack p, MultiBufferSource buffers, float open, int light) {
        var v = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        draw(p, v, SHELL, light);
        if (open > 0) draw(p, v, CONTENTS, light);
        p.pushPose();
        if (open > 0) {
            p.translate(0, Ai2Motion.HINGE_Y, Ai2Motion.HINGE_Z);
            p.mulPose(Ai2Motion.lidRotation(open));
            p.translate(0, -Ai2Motion.HINGE_Y, -Ai2Motion.HINGE_Z);
        }
        draw(p, v, LID, light);
        p.popPose();
    }

    static void syringe(PoseStack p, MultiBufferSource buffers, float plunger, int light) {
        var v = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        draw(p, v, INJECTOR, light);
        p.pushPose();
        p.translate(-.038F * plunger, 0, 0);
        draw(p, v, PISTON, light);
        p.popPose();
    }

    private static Vertex[] shell() {
        Mesh m = new Mesh();
        m.bevel(.144F,.166F,.014F,-.034F,.016F, PLASTIC,PLASTIC,PLASTIC);
        float[][] outer = outline(.144F,.166F,.014F), inner = outline(.131F,.152F,.008F);
        m.band(outer,.016F,outer,.054F,0,PLASTIC);
        m.band(outer,.054F,inner,.054F,0,PLASTIC);
        m.band(inner,.054F,inner,.016F,0,LINING);
        for (float x : new float[]{-.072F,.072F}) {
            m.box(x-.018F,.157F,.031F,x+.018F,.175F,.054F,AMBER);
            m.tube(x-.018F,x+.018F,Ai2Motion.HINGE_Y,Ai2Motion.HINGE_Z,.009F,.009F,AMBER,8);
        }
        m.box(-.028F,-.171F,.033F,.028F,-.165F,.049F,AMBER);
        return m.finish();
    }

    private static Vertex[] lid() {
        Mesh m = new Mesh();
        m.bevel(.146F,.168F,.012F,.062F,.071F,COVER,LINING,PLASTIC);
        // A raised, stepped perimeter encloses the recessed face of the cover.
        float[][] foot = outline(.146F,.168F,.012F), crest = outline(.139F,.160F,.009F);
        float[][] innerLip = outline(.119F,.140F,.005F), panel = outline(.110F,.130F,.003F);
        float[][] seal = outline(.135F,.157F,.008F), well = outline(.128F,.150F,.006F);
        m.band(foot,.055F,foot,.062F,0,PLASTIC);
        m.band(seal,.055F,foot,.055F,0,PLASTIC);
        m.band(well,.062F,seal,.055F,0,AMBER);
        m.band(foot,.071F,crest,.088F,0,PLASTIC);
        m.band(crest,.088F,innerLip,.088F,0,HONEY);
        m.band(innerLip,.088F,panel,.071F,0,AMBER);

        float badgeY = .048F;
        m.raised(diamond(.064F,.074F),diamond(.056F,.066F),badgeY,.071F,.083F,PLASTIC,HONEY);
        m.raised(cross(.034F,.012F),cross(.031F,.010F),badgeY,.083F,.095F,AMBER,COVER);
        for (float y : new float[]{-.040F,-.076F,-.112F})
            m.raised(outline(.104F,.010F,.003F),outline(.101F,.006F,.002F),y,.071F,.083F,AMBER,HONEY);

        m.box(-.050F,.158F,.058F,.050F,.174F,.071F,PLASTIC);
        m.tube(-.050F,.050F,Ai2Motion.HINGE_Y,Ai2Motion.HINGE_Z,.010F,.010F,PLASTIC,8);
        m.box(-.031F,Ai2Motion.LATCH_Y-.008F,.048F,.031F,Ai2Motion.LATCH_Y+.007F,.073F,PLASTIC);
        return m.finish();
    }

    private static float[][] diamond(float x, float y) {
        return new float[][]{{0,y},{x,0},{0,-y},{-x,0}};
    }

    private static float[][] cross(float r, float w) {
        return new float[][]{{-w,r},{w,r},{w,w},{r,w},{r,-w},{w,-w},
                {w,-r},{-w,-r},{-w,-w},{-r,-w},{-r,w},{-w,w}};
    }

    private static Vertex[] contents() {
        Mesh m = new Mesh();
        m.bevel(.130F,.151F,.008F,.016F,.020F,TRAY,TRAY,PLASTIC);
        // Two white ampoules, a full-width injector cradle and three paired medicine bays.
        for (int i = 0; i < 5; i++) {
            float y = new float[]{.127F,.086F,-.010F,-.063F,-.116F}[i];
            m.box(-.119F,y-.014F,.020F,.119F,y+.014F,.022F,RUBBER);
            m.tube(-.114F,-.021F,y,.035F,.012F,.012F,IVORY,6);
            m.tube(-.045F,-.013F,y,.035F,.014F,.014F,i < 2 ? RED : AMBER,8);
            m.tube(.010F,.104F,y,.035F,.012F,.012F,i == 4 ? TEAL : IVORY,6);
            m.tube(.085F,.114F,y,.035F,.014F,.014F,i == 3 ? RED : TEAL,8);
        }
        m.box(-.121F,.025F,.020F,.123F,.053F,.022F,RUBBER);
        for (float x : new float[]{-.018F,.055F}) {
            m.box(x,.022F,.021F,x+.008F,.029F,.042F,AMBER);
            m.box(x,.050F,.021F,x+.008F,.057F,.042F,AMBER);
        }
        return m.finish();
    }

    private static Vertex[] injector() {
        Mesh m = new Mesh();
        m.tube(-.044F,.060F,0,0,.011F,.010F,RED,8);
        m.tube(-.059F,-.044F,0,0,.006F,.006F,IVORY,6);
        m.tube(-.066F,-.058F,0,0,.0036F,.0036F,STEEL,6);
        m.tube(-Ai2Motion.NEEDLE_LENGTH,-.064F,0,0,.0018F,.0018F,STEEL,4);
        m.tube(-.042F,-.038F,0,0,.0116F,.0106F,IVORY,8);
        m.tube(.053F,.063F,0,0,.017F,.012F,RED,8);
        for (int i = 0; i < 4; i++) {
            float x = -.023F + .015F * i;
            m.box(x, 0, .0092F, x+.0025F, i % 2 == 0 ? .005F : .003F, .0102F, IVORY);
        }
        return m.finish();
    }

    private static Vertex[] piston() {
        Mesh m = new Mesh();
        m.tube(.060F,.111F,0,0,.0045F,.0045F,IVORY,6);
        m.tube(.110F,.117F,0,0,.014F,.012F,RED,8);
        return m.finish();
    }

}
