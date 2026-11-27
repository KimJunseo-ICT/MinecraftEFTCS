package com.minecrafteft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import static com.minecrafteft.client.KitMesh.*;

final class IfakKitModel {
    private static final int CAMO=0, FABRIC=1, BINDING=2, LINER=3, WEB=4, LOOP=5, LABEL=6,
            CROSS=7, METAL=8, GAUZE=9, CORE=10, PACKET=11, INJECTOR=12;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecrafteft","textures/item/ifak_atlas.png");
    private static final Vertex[] BACK=back(), FRONT=front(), ZIP=zip(), ROLL=roll(), NEEDLE=syringe(),
            STEM=piston(), DRESSING=dressing();
    private static final float HINGE=IfakMotion.POUCH_HINGE_Y;

    private IfakKitModel() {}

    static void performance(PoseStack p,MultiBufferSource buffers,int light,IfakMotion.Pose motion,
                            IfakMotion.Variant variant,boolean active,boolean mirror,float radius) {
        p.pushPose();
        if (mirror) p.scale(-1,1,1);
        p.pushPose();
        translate(p,motion.pouch());
        p.mulPose(motion.rotation());
        pouch(p,buffers,light,motion.open());
        p.popPose();
        var v=buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        if (active && motion.wrapCoverage()>.001F) {
            p.pushPose();
            translate(p,motion.contact());
            p.mulPose(motion.armRotation());
            // The square inner section fits over a block-model arm as well as rounded sleeves.
            float outer=radius+.004F;
            p.scale(outer,.008F+.085F*motion.wrapCoverage(),outer);
            draw(p,v,DRESSING,light);
            p.popPose();
            if (motion.treatment()<1) ribbon(p,v,light,motion,radius);
        }
        if (active && motion.toolVisible()) {
            p.pushPose();
            translate(p,motion.tool());
            p.mulPose(motion.toolRotation());
            if (variant==IfakMotion.Variant.WRAP) {
                p.mulPose(Axis.ZP.rotationDegrees(90));
                draw(p,v,ROLL,light);
            } else {
                draw(p,v,NEEDLE,light);
                p.translate(-.030F*motion.treatment(),0,0);
                draw(p,v,STEM,light);
            }
            p.popPose();
        }
        p.popPose();
    }

    static void pouch(PoseStack p,MultiBufferSource buffers,int light,float open) {
        VertexConsumer v=buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        draw(p,v,BACK,light);
        float angle=24F*open;
        if (open>.001F) {
            double a=Math.toRadians(angle);
            float y=HINGE+.297F*(float)Math.cos(a), z=.024F+.297F*(float)Math.sin(a);
            for (int side=-1;side<=1;side+=2) {
                float x=side*.125F;
                sideVertex(p,v,light,side,x,HINGE,.025F,0,1);
                sideVertex(p,v,light,side,x,.123F,.008F,0,0);
                sideVertex(p,v,light,side,x*.88F,y-.017F,(z+.024F)*.5F,.5F,.1F);
                sideVertex(p,v,light,side,x,y,z,1,0);
            }
        }
        p.pushPose();
        p.translate(0,HINGE,.024F);
        p.mulPose(Axis.XP.rotationDegrees(angle));
        p.translate(0,-HINGE,-.024F);
        draw(p,v,FRONT,light);
        p.translate(.107F-.214F*open,.148F,.052F);
        p.mulPose(Axis.ZP.rotationDegrees(-23+39*open));
        draw(p,v,ZIP,light);
        p.popPose();
    }

    private static void translate(PoseStack p,Vector3f point) { p.translate(point.x,point.y,point.z); }

    private static void ribbon(PoseStack p,VertexConsumer v,int light,IfakMotion.Pose motion,float radius) {
        float angle=(float)(Math.PI*6)*motion.treatment();
        var axis=motion.armRotation();
        var normal=axis.transform(new Vector3f((float)Math.cos(angle),0,(float)Math.sin(angle)));
        var width=axis.transform(new Vector3f(0,.021F,0));
        float x=(float)Math.cos(angle-.58F), z=(float)Math.sin(angle-.58F);
        float edge=(radius+.005F)/Math.max(Math.abs(x),Math.abs(z));
        var start=axis.transform(new Vector3f(x*edge,0,z*edge)).add(motion.contact());
        var end=new Vector3f(motion.tool()).fma(-.021F,normal);
        ribbonVertex(p,v,light,start,width,-1,0,normal);
        ribbonVertex(p,v,light,start,width,1,0,normal);
        ribbonVertex(p,v,light,end,width,1,1,normal);
        ribbonVertex(p,v,light,end,width,-1,1,normal);
    }

    private static void ribbonVertex(PoseStack p,VertexConsumer v,int light,Vector3f point,Vector3f width,
                                    int side,int along,Vector3f normal) {
        v.vertex(p.last().pose(),point.x+side*width.x,point.y+side*width.y,point.z+side*width.z)
                .color(246,246,246,255).uv((1.03F+.94F*along)/4,(2.5F+.46F*side)/4)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(p.last().normal(),normal.x,normal.y,normal.z).endVertex();
    }

    private static void sideVertex(PoseStack p,VertexConsumer v,int light,int side,float x,float y,float z,float u,float w) {
        v.vertex(p.last().pose(),x,y,z).color(220,220,220,255).uv((3+.02F+.96F*u)/4,(.02F+.96F*w)/4)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.last().normal(),side,0,0).endVertex();
    }

    // Clockwise superellipse sections taper towards each padded face.
    private static float[][] section(float x,float y) {
        float[][] ring=new float[16][2];
        for (int i=0;i<ring.length;i++) {
            double a=Math.PI*2*i/ring.length, sx=Math.sin(a), sy=Math.cos(a);
            ring[i][0]=(float)(Math.copySign(Math.pow(Math.abs(sx),.48),sx)*x);
            ring[i][1]=(float)(Math.copySign(Math.pow(Math.abs(sy),.48),sy)*y);
        }
        return ring;
    }

    private static void face(Mesh m,float[][] ring,float x,float y,float z,int tile,int normal) {
        for (int i=0;i<ring.length;i+=2) {
            m.cap(new float[]{0,0},x,y,z,tile,normal);
            for (int j=0;j<3;j++) m.cap(ring[(i+(normal==1?2-j:j))%ring.length],x,y,z,tile,normal);
        }
    }

    // Lay the weave along the perimeter. A thin zipper welt uses a narrow texture
    // strip so that its thread density matches the front panel at a distance.
    private static void seam(Mesh m,float[][] from,float z0,float[][] to,float z1,int tile) {
        for (int i=0;i<from.length;i++) {
            int j=(i+1)%from.length;
            float[] a=from[i], b=from[j], c=to[i], d=to[j];
            var across=new Vector3f(c[0]-a[0],c[1]-a[1],z1-z0);
            float v0=.22F, v1=v0+Math.min(.64F,across.length()/.27F);
            var n=across.cross(b[0]-a[0],b[1]-a[1],0).normalize();
            float u0=(i%4)*.25F, u1=u0+.25F;
            m.vertex(tile,a[0],a[1],z0,u0,v0,n.x,n.y,n.z);
            m.vertex(tile,c[0],c[1],z1,u0,v1,n.x,n.y,n.z);
            m.vertex(tile,d[0],d[1],z1,u1,v1,n.x,n.y,n.z);
            m.vertex(tile,b[0],b[1],z0,u1,v0,n.x,n.y,n.z);
        }
    }

    private static Vertex[] back() {
        Mesh m=new Mesh();
        float[][] back=section(.119F,.145F), belly=section(.143F,.166F), rim=section(.139F,.163F);
        face(m,back,.119F,.145F,-.076F,FABRIC,-1);
        seam(m,back,-.076F,belly,-.052F,CAMO);
        seam(m,belly,-.052F,belly,.013F,FABRIC);
        seam(m,belly,.013F,rim,.023F,BINDING);
        face(m,rim,.139F,.163F,.023F,LINER,1);
        for(float x:new float[]{-.069F,.069F}) {
            m.box(x-.021F,-.117F,-.087F,x+.021F,.115F,-.075F,WEB);
            m.box(x-.025F,-.082F,-.091F,x+.025F,-.065F,-.085F,BINDING);
        }
        m.box(-.094F,.053F,.024F,-.025F,.119F,.040F,PACKET);
        m.box(.008F,.050F,.024F,.091F,.128F,.037F,GAUZE);
        m.box(-.102F,.039F,.035F,.101F,.064F,.042F,WEB);
        return m.finish();
    }

    private static Vertex[] front() {
        Mesh m=new Mesh();
        float[][] seal=section(.140F,.164F), welt=section(.144F,.167F), nose=section(.132F,.154F),
                panel=section(.120F,.142F);
        face(m,seal,.140F,.164F,.025F,LINER,-1);
        seam(m,seal,.025F,welt,.033F,BINDING);
        seam(m,welt,.033F,welt,.041F,WEB);
        seam(m,welt,.041F,nose,.062F,CAMO);
        seam(m,nose,.062F,panel,.070F,CAMO);
        face(m,panel,.120F,.142F,.070F,CAMO,1);
        m.box(-.046F,-.039F,.069F,.046F,.047F,.075F,BINDING);
        m.box(-.042F,-.035F,.074F,.042F,.043F,.078F,LOOP);
        m.box(-.011F,-.024F,.077F,.011F,.032F,.081F,CROSS);
        m.box(-.028F,-.007F,.077F,.028F,.015F,.081F,CROSS);
        m.box(-.048F,.120F,.063F,.048F,.146F,.074F,LABEL);
        for (int i=-7;i<=7;i++) {
            float x=i*.014F;
            m.box(x-.003F,.158F,.035F,x+.003F,.164F,.042F,METAL);
        }
        return m.finish();
    }

    private static Vertex[] zip() {
        Mesh m=new Mesh();
        m.box(-.009F,-.006F,-.004F,.009F,.012F,.005F,METAL);
        m.box(-.006F,-.028F,.001F,.006F,-.002F,.009F,WEB);
        m.box(-.009F,-.050F,.003F,.009F,-.024F,.011F,BINDING);
        return m.finish();
    }

    private static Vertex[] roll() {
        Mesh m=new Mesh();
        m.tube(-.032F,.032F,0,0,.032F,.032F,GAUZE,12);
        m.tube(-.033F,.033F,0,0,.011F,.011F,CORE,8);
        m.box(.016F,-.028F,-.013F,.048F,-.024F,.018F,GAUZE);
        return m.finish();
    }

    private static Vertex[] dressing() {
        Mesh m=new Mesh();
        // Three overlapping turns break up the silhouette of the gauze cuff.
        for (int i=0;i<3;i++) {
            float y=-.5F+i*.32F, w=i==1?1.035F:1F;
            m.box(-w,y,-w,w,y+.36F,w,GAUZE);
        }
        return m.finish();
    }

    private static Vertex[] syringe() {
        Mesh m=new Mesh();
        m.tube(-IfakMotion.NEEDLE,-.045F,0,0,.0017F,.0017F,METAL,4);
        m.tube(-.045F,-.032F,0,0,.005F,.005F,BINDING,6);
        m.tube(-.032F,.045F,0,0,.010F,.010F,INJECTOR,8);
        m.tube(.042F,.048F,0,0,.016F,.012F,CROSS,8);
        return m.finish();
    }

    private static Vertex[] piston() {
        Mesh m=new Mesh();
        m.tube(.045F,.082F,0,0,.004F,.004F,CORE,6);
        m.tube(.081F,.087F,0,0,.013F,.012F,BINDING,8);
        return m.finish();
    }
}
