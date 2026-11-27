package com.minecrafteft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import java.util.ArrayList;

/** Cached meshes use a four-by-four material atlas. */
final class KitMesh {
    private KitMesh() {}

    static float[][] outline(float x, float y, float corner) {
        return new float[][]{{-x+corner,y},{x-corner,y},{x,y-corner},{x,-y+corner},
                {x-corner,-y},{-x+corner,-y},{-x,-y+corner},{-x,y-corner}};
    }

    record Vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz, int shade) {}

    static void draw(PoseStack p, VertexConsumer consumer, Vertex[] mesh, int light) {
        PoseStack.Pose pose = p.last();
        for (Vertex v : mesh)
            consumer.vertex(pose.pose(), v.x, v.y, v.z).color(v.shade,v.shade,v.shade,255)
                    .uv(v.u,v.v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                    .normal(pose.normal(),v.nx,v.ny,v.nz).endVertex();
    }

    static final class Mesh {
        private final ArrayList<Vertex> vertices = new ArrayList<>();

        Vertex[] finish() { return vertices.toArray(Vertex[]::new); }

        // Both contours run clockwise viewed from the front. Each sloped side
        // gets its own geometric normal so the edges respond to light as the kit turns.
        void band(float[][] from, float z0, float[][] to, float z1, float cy, int material) {
            for (int i = 0; i < from.length; i++) {
                int j = (i + 1) % from.length;
                float[] a = from[i], b = from[j], c = to[i], d = to[j];
                float ux = c[0]-a[0], uy = c[1]-a[1], uz = z1-z0;
                float ex = b[0]-a[0], ey = b[1]-a[1];
                float nx = -uz*ey, ny = uz*ex, nz = ux*ey-uy*ex;
                float n = (float)Math.sqrt(nx*nx+ny*ny+nz*nz);
                quad(material,nx/n,ny/n,nz/n,a[0],cy+a[1],z0,c[0],cy+c[1],z1,
                        d[0],cy+d[1],z1,b[0],cy+b[1],z0);
            }
        }

        void raised(float[][] base, float[][] top, float cy, float z0, float z1, int side, int face) {
            band(base,z0,top,z1,cy,side);
            for (int i = 0; i < top.length; i += 2) {
                float[] a = top[i], b = top[i+1], c = top[(i+2)%top.length];
                quad(face,0,0,1,0,cy,z1,c[0],cy+c[1],z1,b[0],cy+b[1],z1,a[0],cy+a[1],z1);
            }
        }

        void bevel(float x,float y,float r,float lo,float hi,int front,int back,int side) {
            float[][] ring = outline(x,y,r);
            for (int[] q : new int[][]{{7,2,1,0},{6,3,2,7},{5,4,3,6}}) {
                for (int i : q) cap(ring[i],x,y,hi,front,1);
                for (int i = 3; i >= 0; i--) cap(ring[q[i]],x,y,lo,back,-1);
            }
            for (int i = 0; i < 8; i++) {
                var a = ring[i]; var b = ring[(i+1)%8];
                float dx = b[0]-a[0], dy = b[1]-a[1], len = (float)Math.hypot(dx,dy);
                quad(side,-dy/len,dx/len,0,a[0],a[1],hi,b[0],b[1],hi,b[0],b[1],lo,a[0],a[1],lo);
            }
        }

        void cap(float[] a,float x,float y,float z,int mat,int n) {
            vertex(mat,a[0],a[1],z,(1+n*a[0]/x)*.5F,(1-a[1]/y)*.5F,0,0,n);
        }

        void tube(float x1,float x2,float y,float z,float ry,float rz,int mat,int sides) {
            for (int i = 0; i < sides; i++) {
                double a = 2*Math.PI*i/sides, b = 2*Math.PI*(i+1)/sides;
                float ya = y+ry*(float)Math.cos(a), za = z+rz*(float)Math.sin(a);
                float yb = y+ry*(float)Math.cos(b), zb = z+rz*(float)Math.sin(b);
                float ny = zb-za, nz = ya-yb, len = (float)Math.hypot(ny,nz);
                quad(mat,0,ny/len,nz/len,x1,ya,za,x1,yb,zb,x2,yb,zb,x2,ya,za);
                // Pair adjacent end triangles into quads meeting at the centre.
                if (i % 2 == 0) {
                    double c = 2*Math.PI*(i+2)/sides;
                    float yc = y+ry*(float)Math.cos(c), zc = z+rz*(float)Math.sin(c);
                    quad(mat,1,0,0,x2,y,z,x2,ya,za,x2,yb,zb,x2,yc,zc);
                    quad(mat,-1,0,0,x1,y,z,x1,yc,zc,x1,yb,zb,x1,ya,za);
                }
            }
        }

        void box(float x,float y,float z,float X,float Y,float Z,int m) {
            quad(m,0,0,1, x,Y,Z,x,y,Z,X,y,Z,X,Y,Z);
            quad(m,0,0,-1, X,Y,z,X,y,z,x,y,z,x,Y,z);
            quad(m,-1,0,0, x,Y,z,x,y,z,x,y,Z,x,Y,Z);
            quad(m,1,0,0, X,Y,Z,X,y,Z,X,y,z,X,Y,z);
            quad(m,0,1,0, x,Y,z,x,Y,Z,X,Y,Z,X,Y,z);
            quad(m,0,-1,0, x,y,Z,x,y,z,X,y,z,X,y,Z);
        }

        void quad(int mat,float nx,float ny,float nz,float... coords) {
            for (int i=0; i<4; i++)
                vertex(mat,coords[3*i],coords[3*i+1],coords[3*i+2], i<2?0:1, i==0||i==3?0:1, nx,ny,nz);
        }

        void vertex(int mat,float x,float y,float z,float u,float v,float nx,float ny,float nz) {
            vertices.add(new Vertex(x,y,z,((mat%4)*32+.5F+u*31)/128F,
                    ((mat/4)*32+.5F+v*31)/128F,nx,ny,nz,(int)(255*(.92F+.08F*ny))));
        }
    }
}
