package com.minecrafteft.inventory;

/** Pure preflight planner; inputs never change when equipment capacity is exceeded. */
public final class WeaponSlotAllocation {
    public static final int OTHER=0,PISTOL=1,PRIMARY=2,MELEE=3;
    public record Plan(boolean fits,int pistols,int primaries,int melee,int[] sources) { }
    private WeaponSlotAllocation() { }
    public static Plan plan(int[] kinds,int[] counts,int meleeSource) {
        if(kinds.length!=counts.length) throw new IllegalArgumentException("Mismatched inventory arrays");
        int[] total=new int[4];
        for(int i=0;i<kinds.length;i++) if(kinds[i]>0 && kinds[i]<4) total[kinds[i]]+=Math.max(0,counts[i]);
        int[] result={-1,-1,-1,-1};
        if(total[PISTOL]>1 || total[PRIMARY]>2 || total[MELEE]>1)
            return new Plan(false,total[PISTOL],total[PRIMARY],total[MELEE],result);
        int[] remaining=counts.clone(),required={PISTOL,PRIMARY,PRIMARY,MELEE},preferred={0,1,2,meleeSource};
        for(int slot=0;slot<4;slot++) {
            int source=preferred[slot];
            if(source>=0 && source<kinds.length && kinds[source]==required[slot] && remaining[source]>0) { result[slot]=source;remaining[source]--; }
        }
        for(int slot=0;slot<4;slot++) if(result[slot]<0) for(int source=0;source<kinds.length;source++)
            if(kinds[source]==required[slot] && remaining[source]>0) { result[slot]=source;remaining[source]--;break; }
        return new Plan(true,total[PISTOL],total[PRIMARY],total[MELEE],result);
    }
}
