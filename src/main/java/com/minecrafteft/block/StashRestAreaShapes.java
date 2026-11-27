package com.minecrafteft.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import static com.minecrafteft.block.ContainerStructureLayout.Decoration;

/** Furniture occupies partial blocks, so the deck and rooftop remain walkable. */
final class StashRestAreaShapes {
    static VoxelShape shape(Decoration type,Direction facing) {
        boolean deck=type.deck();
        double base=deck?.12:0;
        VoxelShape result=deck?Shapes.box(0,0,0,1,.12,1):Shapes.empty();
        switch(type) {
            case CRATE_ON_DECK -> result=Shapes.or(result,Shapes.box(.10,base,.10,.90,base+.65,.90));
            case PLANTER_ON_DECK -> result=Shapes.or(result,Shapes.box(.12,base,.12,.88,base+.55,.88));
            case BENCH,BENCH_ON_DECK -> result=Shapes.or(result,
                    Shapes.box(0,base+.44,.16,1,base+.54,.84),
                    Shapes.box(0,base+.55,.84,1,base+.86,.95),
                    Shapes.box(.10,base,.20,.20,base+.44,.82),
                    Shapes.box(.80,base,.20,.90,base+.44,.82));
            case WORKBENCH,WORKBENCH_ON_DECK -> result=Shapes.or(result,
                    Shapes.box(.03,base+.70,.04,.97,base+.78,.96),
                    Shapes.box(.10,base,.12,.20,base+.70,.88),
                    Shapes.box(.80,base,.12,.90,base+.70,.88),
                    Shapes.box(.58,base+.78,.53,.90,base+.86,.85));
            case LAMP_ON_DECK,LAMP_BASE,LAMP_POST -> result=Shapes.or(result,Shapes.box(.43,base,.43,.57,1,.57));
            case LAMP_HEAD -> result=Shapes.or(Shapes.box(.45,0,.45,.55,.30,.55),Shapes.box(.28,.25,.28,.72,.65,.72));
            case ROOF_LAMP_HEAD -> result=Shapes.or(Shapes.box(.45,0,.45,.55,.48,.55),Shapes.box(.28,.43,.28,.72,.83,.72));
            default -> { }
        }
        if(facing==Direction.NORTH) return result;
        VoxelShape[] rotated={Shapes.empty()};
        result.forAllBoxes((x1,y1,z1,x2,y2,z2)->{
            var box=switch(facing) {
                case EAST -> Shapes.box(1-z2,y1,x1,1-z1,y2,x2);
                case SOUTH -> Shapes.box(1-x2,y1,1-z2,1-x1,y2,1-z1);
                case WEST -> Shapes.box(z1,y1,1-x2,z2,y2,1-x1);
                default -> Shapes.box(x1,y1,z1,x2,y2,z2);
            };
            rotated[0]=Shapes.or(rotated[0],box);
        });
        return rotated[0];
    }
}
