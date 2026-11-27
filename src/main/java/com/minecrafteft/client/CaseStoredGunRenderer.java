package com.minecrafteft.client;

import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.capture.GeometryCollector;
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.capture.MeshSink;
import com.minecrafteft.block.WeaponCaseKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.client.renderer.block.DisplayGunGeometry;
import com.tacz.guns.client.renderer.external.ExternalGunAppearance;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import java.util.LinkedHashMap;
import java.util.Map;

/** Captures the selected stored gun's real TACZ geometry and attachments, then lays it flat.
 * No GUI icon, framebuffer or synthetic extra inventory item is used for the display model. */
final class CaseStoredGunRenderer {
    // These pack models bind their FIXED pose with the opposite face uppermost.
    // A roll around the barrel axis keeps the muzzle left while bringing the grip forward.
    private static final java.util.Set<String> FACE_UP_ROLL_CORRECTIONS=java.util.Set.of(
            "tacz:glock_17","tacz:cz75","tacz:m1911","tacz:lonetrail","tacz:taurus943","tacz:b93r","tacz:tec_9",
            "tacz:ai_awp","tacz:springfield1873","tacz:svd","tacz:mrad","tacz:m95","tacz:m700","tacz:scar_mk20","tacz:spr15hb",
            "tacz:m16a1","tacz:m16a4","tacz:m4a1","tacz:g36k","tacz:fn_fal","tacz:mcx_spear_lt","tacz:qr_24",
            "tacz:mk18","tacz:urgi","tacz:hcar","tacz:ak74m","tacz:m870","tacz:m1014","tacz:fn_evolys",
            "tacz:m249","tacz:aa12","tacz:spas_12","tacz:rpk");
    private static final Map<Object,Model> MODELS=new LinkedHashMap<>(16,.75F,true);

    static boolean ready(ItemStack stack) { return model(stack)!=null; }

    private static Model model(ItemStack stack) {
        if(stack.isEmpty()) return null;
        var resolution=ExternalGunAppearance.resolve(stack);
        if(resolution.variants()==null || resolution.variants().near()==null) return null;
        var snapshot=resolution.variants().near();
        Model previous=MODELS.get(snapshot.key());
        if(previous!=null) return previous;
        GeometryCollector collector=new GeometryCollector();
        if(!DisplayGunGeometry.capture(snapshot,ItemDisplayContext.FIXED,collector)) return null;
        var passes=collector.snapshot();
        AABB bounds=null;
        for(var sink:passes.values()) {
            if(sink.isEmpty()) continue;
            bounds=bounds==null?sink.bounds():bounds.minmax(sink.bounds());
        }
        if(bounds==null || bounds.getSize()<.00001) return null;
        double[] spans={bounds.getXsize(),bounds.getYsize(),bounds.getZsize()};
        Integer[] axes={0,1,2};
        java.util.Arrays.sort(axes,java.util.Comparator.comparingDouble(axis->spans[axis]));
        int thickness=axes[0], depth=axes[1], length=axes[2];
        // Map the longest dimension to case X and the thinnest to case Y.
        Matrix4f flat=new Matrix4f().zero().m33(1);
        for(int axis=0;axis<3;axis++) flat.setColumn(axis,new org.joml.Vector4f(
                axis==length?1:0,axis==thickness?1:0,axis==depth?1:0,0));
        if(flat.determinant()<0) flat.m02(-flat.m02()).m12(-flat.m12()).m22(-flat.m22());
        // In the north-facing case, +X is the viewer's left. Use TACZ's actual muzzle
        // locator so replacing the stored weapon cannot reverse the barrel direction.
        boolean turnAround=true;
        var instance=snapshot.model().displayInstance();
        int muzzle=instance.muzzleFlashPosition();
        if(muzzle>=0) {
            PoseStack fixed=new PoseStack();
            if(DisplayGunGeometry.applyTransform(snapshot,ItemDisplayContext.FIXED,false,fixed)) {
                org.joml.Vector3f tip=instance.getGlobalTransform(muzzle).getTranslation(new org.joml.Vector3f());
                tip.mulPosition(fixed.last().pose());
                var center=bounds.getCenter();
                tip.sub((float)center.x,(float)center.y,(float)center.z);
                flat.transformDirection(tip);
                if(Math.abs(tip.x)>.0001F) turnAround=tip.x<0;
            }
        }
        if(turnAround) flat=new Matrix4f().rotationY((float)Math.PI).mul(flat);
        var gun=com.tacz.guns.api.item.IGun.getIGunOrNull(stack);
        if(gun!=null && FACE_UP_ROLL_CORRECTIONS.contains(String.valueOf(gun.getGunId(stack))))
            flat=new Matrix4f().rotationX((float)Math.PI).mul(flat);
        Model result=new Model(snapshot.key(),Map.copyOf(passes),bounds,flat,
                spans[length],spans[depth],spans[thickness]);
        MODELS.put(snapshot.key(),result);
        while(MODELS.size()>64) MODELS.remove(MODELS.keySet().iterator().next());
        return result;
    }

    static void render(PoseStack pose,MultiBufferSource buffers,WeaponCaseKind kind,ItemStack stack,int light) {
        Model model=model(stack);
        if(model==null) return; // Retry when TACZ finishes loading/reloading its display assets.
        var gun=com.tacz.guns.api.item.IGun.getIGunOrNull(stack);
        boolean machineGun=gun!=null && !"tacz:rpk".equals(String.valueOf(gun.getGunId(stack)))
                && com.tacz.guns.api.TimelessAPI.getCommonGunIndex(gun.getGunId(stack))
                .map(index->"mg".equals(index.getType())).orElse(false);
        // Belt/drum-fed guns need more clearance than the thin rifle foam allowance.
        double clearance=machineGun?(kind.small()?.30:.57):(kind.small()?.125:.44);
        double scale=Math.min((kind.small()?1.44:2.15)/Math.max(.00001,model.length()),
                Math.min((kind.small()?.56:1.05)/Math.max(.00001,model.depth()),
                        clearance/Math.max(.00001,model.thickness())));
        pose.pushPose();
        try {
            pose.translate(kind.small()?.85:1.35,
                    (kind.small()?.108:.305)+model.thickness()*scale/2,kind.small()?.40:.80);
            pose.scale((float)scale,(float)scale,(float)scale);
            pose.mulPoseMatrix(model.flat());
            var center=model.bounds().getCenter();
            pose.translate(-center.x,-center.y,-center.z);
            for(var pass:model.passes().entrySet()) {
                pass.getValue().emitTo(new LitConsumer(buffers.getBuffer(pass.getKey().material()),light),
                        pose.last().pose(),0,0,0);
            }
        } finally { pose.popPose(); }
    }

    private record Model(Object key,Map<GeometryCollector.Pass,MeshSink> passes,AABB bounds,Matrix4f flat,
                         double length,double depth,double thickness) { }

    /** Captured normal surfaces use light=0; retain the gun's explicit emissive passes. */
    private record LitConsumer(VertexConsumer target,int light) implements VertexConsumer {
        @Override public VertexConsumer vertex(double x,double y,double z){target.vertex(x,y,z);return this;}
        @Override public VertexConsumer color(int r,int g,int b,int a){target.color(r,g,b,a);return this;}
        @Override public VertexConsumer uv(float u,float v){target.uv(u,v);return this;}
        @Override public VertexConsumer overlayCoords(int u,int v){target.overlayCoords(u,v);return this;}
        @Override public VertexConsumer uv2(int u,int v){target.uv2(u==0&&v==0?light&65535:u,u==0&&v==0?light>>>16:v);return this;}
        @Override public VertexConsumer normal(float x,float y,float z){target.normal(x,y,z);return this;}
        @Override public void endVertex(){target.endVertex();}
        @Override public void defaultColor(int r,int g,int b,int a){target.defaultColor(r,g,b,a);}
        @Override public void unsetDefaultColor(){target.unsetDefaultColor();}
    }
}
