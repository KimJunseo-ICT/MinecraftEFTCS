package com.minecrafteft.block;

import java.util.ArrayList;
import java.util.List;

/** Local north-facing coordinates; shared by placement, collision and the visual model. */
public final class ContainerStructureLayout {
    private ContainerStructureLayout() { }

    public record Cell(int x, int y, int z, boolean stair) { }
    public enum Decoration {
        NONE, DECK, BENCH_ON_DECK, WORKBENCH_ON_DECK, LAMP_ON_DECK,
        LAMP_POST, LAMP_HEAD, BENCH, WORKBENCH, LAMP_BASE, ROOF_LAMP_HEAD,
        CRATE_ON_DECK, PLANTER_ON_DECK;
        public boolean deck() { return this==DECK || name().endsWith("_ON_DECK"); }
        public boolean seat() { return this==BENCH || this==BENCH_ON_DECK; }
        public boolean workbench() { return this==WORKBENCH || this==WORKBENCH_ON_DECK; }
        public boolean light() { return this==LAMP_HEAD || this==ROOF_LAMP_HEAD; }
    }
    private static final List<Cell> REST_AREA_CELLS=createRestAreaCells();
    public static Decoration decorationAt(int x,int y,int z) {
        // Three side-by-side containers are nine blocks wide. Keep the rear deck
        // just three blocks deep, within the existing level-4 footprint.
        if(y==0 && x>=0 && x<=8 && z>=5 && z<=7) {
            if(x==0 && z==5) return Decoration.CRATE_ON_DECK;
            if(x==0 && z==7 || x==8 && z==7) return Decoration.PLANTER_ON_DECK;
            if((x==2 || x==3) && z==7) return Decoration.BENCH_ON_DECK;
            if(x==3 && z==5) return Decoration.WORKBENCH_ON_DECK;
            if(x==6 && z==5) return Decoration.LAMP_ON_DECK;
            if((x==6 || x==7) && z==7) return Decoration.BENCH_ON_DECK;
            if(x==8 && z==6) return Decoration.WORKBENCH_ON_DECK;
            return Decoration.DECK;
        }
        if(x==6 && z==5 && y==1) return Decoration.LAMP_POST;
        if(x==6 && z==5 && y==2) return Decoration.LAMP_HEAD;
        if(y==3 && (x==6 || x==7) && z==4) return Decoration.BENCH;
        if(y==3 && x==7 && z==0) return Decoration.WORKBENCH;
        if(y==3 && x==8 && z==0) return Decoration.LAMP_BASE;
        if(y==4 && x==8 && z==0) return Decoration.ROOF_LAMP_HEAD;
        return Decoration.NONE;
    }
    public static List<Cell> restAreaCells() {
        return REST_AREA_CELLS;
    }
    private static List<Cell> createRestAreaCells() {
        var cells=new ArrayList<Cell>();
        for(int x=0;x<=8;x++) for(int y=0;y<=4;y++) for(int z=0;z<=7;z++)
            if(decorationAt(x,y,z)!=Decoration.NONE) cells.add(new Cell(x,y,z,false));
        return List.copyOf(cells);
    }

    public static List<Cell> cells(int tier) {
        var cells = new ArrayList<Cell>();
        int containers = Math.min(3, tier);
        for (int x = 0; x < containers * 3; x++) for (int y = 0; y < 3; y++) for (int z = 0; z < 5; z++)
            cells.add(new Cell(x, y, z, false));
        if (tier == 4) {
            for (int x = 0; x < 5; x++) for (int y = 3; y < 6; y++) for (int z = 1; z < 4; z++)
                cells.add(new Cell(x, y, z, false));
            for (int z = 0; z < 5; z++) for (int y = 0; y <= Math.min(z, 2); y++)
                cells.add(new Cell(9, y, z, z < 3 && y == z));
            cells.addAll(restAreaCells());
        }
        return List.copyOf(cells);
    }

    public static boolean door(Cell cell, int localFace, int tier) {
        // Local faces: north=0, east=1, south=2, west=3.
        return (localFace == 0 && cell.z == 0 && cell.y < 3 && cell.x < Math.min(tier, 3) * 3)
                || (tier == 4 && localFace == 1 && cell.x == 4 && cell.y >= 3 && cell.z >= 1 && cell.z < 4);
    }
}
