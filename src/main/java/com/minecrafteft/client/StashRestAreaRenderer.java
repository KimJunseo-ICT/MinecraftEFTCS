package com.minecrafteft.client;

import com.minecrafteft.block.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import static com.minecrafteft.block.ContainerStructureLayout.Decoration;

/** Linked benches, craft tables, timber deck and two actual light fixtures. */
final class StashRestAreaRenderer {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("minecrafteft","textures/block/rest_area_atlas.png");
    static void render(PersonalStashBlockEntity entity,PoseStack pose,MultiBufferSource buffers,int fallbackLight) {
        var v=buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        var facing=entity.getBlockState().getValue(PersonalStashBlock.FACING);
        for(var cell:ContainerStructureLayout.restAreaCells()) {
            if(entity.getLevel()!=null) {
                var pos=ContainerStructure.position(entity.getBlockPos(),facing,cell.x(),cell.y(),cell.z());
                var state=entity.getLevel().getBlockState(pos);
                if(!(state.getBlock() instanceof ContainerPartBlock) || !ContainerPartBlock.anchor(pos,state).equals(entity.getBlockPos())) continue;
            }
            var type=ContainerStructureLayout.decorationAt(cell.x(),cell.y(),cell.z());
            int light=entity.getLevel()==null?fallbackLight:LevelRenderer.getLightColor(entity.getLevel(),
                    ContainerStructure.position(entity.getBlockPos(),facing,cell.x(),cell.y(),cell.z()));
            pose.pushPose();
            pose.translate(cell.x(),cell.y(),cell.z());
            boolean deck=type.deck();
            if(deck) for(int plank=0;plank<4;plank++)
                box(pose,v,0,0,plank*.25F,1,.12F,plank*.25F+.244F,0,light);
            float base=deck?.12F:0;
            switch(type) {
                case CRATE_ON_DECK -> {
                    box(pose,v,.10F,base,.10F,.90F,base+.65F,.90F,0,light);
                    for(float x:new float[]{.18F,.74F}) box(pose,v,x,base,.09F,x+.08F,base+.67F,.91F,1,light);
                }
                case PLANTER_ON_DECK -> {
                    box(pose,v,.12F,base,.12F,.88F,base+.48F,.88F,0,light);
                    box(pose,v,.10F,base+.43F,.10F,.90F,base+.55F,.90F,1,light);
                    box(pose,v,.20F,base+.55F,.20F,.80F,base+.59F,.80F,2,light);
                    for(float x:new float[]{.30F,.61F}) {
                        box(pose,v,x,base+.59F,.44F,x+.05F,base+.91F,.49F,0,light);
                        box(pose,v,x-.07F,base+.72F,.37F,x+.12F,base+.83F,.56F,2,light);
                    }
                }
                case BENCH,BENCH_ON_DECK -> bench(pose,v,base,light);
                case WORKBENCH,WORKBENCH_ON_DECK -> workbench(pose,v,base,light);
                case LAMP_ON_DECK,LAMP_BASE,LAMP_POST -> {
                    box(pose,v,.45F,base,.45F,.55F,1,.55F,1,light);
                    if(type!=Decoration.LAMP_POST) box(pose,v,.34F,base,.34F,.66F,base+.045F,.66F,1,light);
                }
                case LAMP_HEAD,ROOF_LAMP_HEAD -> lamp(pose,v,type==Decoration.LAMP_HEAD?.25F:.43F,light);
                default -> { }
            }
            pose.popPose();
        }
    }
    private static void bench(PoseStack p,VertexConsumer v,float b,int light) {
        for(float x:new float[]{.10F,.82F}) {
            for(float z:new float[]{.20F,.74F}) box(p,v,x,b,z,x+.08F,b+.44F,z+.08F,1,light);
            // Join inside the legs instead of sharing their exterior planes.
            box(p,v,x+.01F,b+.20F,.21F,x+.07F,b+.26F,.81F,1,light);
            box(p,v,x,b+.40F,.87F,x+.08F,b+.86F,.95F,1,light);
        }
        for(int board=0;board<3;board++)
            box(p,v,0,b+.44F,.16F+board*.23F,1,b+.54F,.378F+board*.23F,0,light);
        for(int board=0;board<2;board++)
            box(p,v,0,b+.56F+board*.15F,.84F,1,b+.70F+board*.15F,.90F,0,light);
    }
    private static void workbench(PoseStack p,VertexConsumer v,float b,int light) {
        // The shelf overhangs the legs slightly: no coplanar shelf/leg faces.
        for(float x:new float[]{.11F,.83F}) for(float z:new float[]{.15F,.79F})
            box(p,v,x,b,z,x+.06F,b+.70F,z+.06F,1,light);
        box(p,v,.10F,b+.23F,.14F,.90F,b+.28F,.86F,0,light);
        box(p,v,.03F,b+.70F,.04F,.97F,b+.78F,.96F,0,light);
        box(p,v,.58F,b+.785F,.53F,.90F,b+.855F,.85F,2,light);
        box(p,v,.58F,b+.825F,.53F,.90F,b+.84F,.534F,1,light);
        box(p,v,.72F,b+.81F,.524F,.76F,b+.84F,.529F,3,light);
        box(p,v,.70F,b+.855F,.65F,.78F,b+.875F,.69F,1,light);
        // Mug and a small hand tool make both spaces look used.
        box(p,v,.23F,b+.785F,.66F,.32F,b+.865F,.75F,2,light);
        box(p,v,.32F,b+.81F,.68F,.345F,b+.85F,.73F,1,light);
        box(p,v,.25F,b+.785F,.26F,.48F,b+.802F,.29F,1,light);
        box(p,v,.23F,b+.782F,.24F,.28F,b+.81F,.31F,2,light);
    }
    private static void lamp(PoseStack p,VertexConsumer v,float bottom,int light) {
        box(p,v,.45F,0,.45F,.55F,bottom,.55F,1,light);
        box(p,v,.27F,bottom,.27F,.73F,bottom+.045F,.73F,1,light);
        box(p,v,.34F,bottom+.05F,.34F,.66F,bottom+.33F,.66F,3,0x00F000F0);
        for(float x:new float[]{.29F,.68F}) for(float z:new float[]{.29F,.68F})
            box(p,v,x,bottom+.045F,z,x+.03F,bottom+.35F,z+.03F,1,light);
        box(p,v,.25F,bottom+.35F,.25F,.75F,bottom+.40F,.75F,2,light);
    }
    private static void box(PoseStack p,VertexConsumer v,float x1,float y1,float z1,float x2,float y2,float z2,int material,int light) {
        ContainerStashRenderer.cuboid(p,v,x1,y1,z1,x2,y2,z2,material,0,light);
    }
}
