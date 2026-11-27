import com.minecrafteft.inventory.WeaponSlotAllocation;
import java.util.Arrays;

public final class WeaponSlotAllocationCheck {
    private static int checks;
    private static void check(boolean ok,String reason) { checks++;if(!ok) throw new AssertionError(reason); }
    private static void verify(int[] kinds,int[] counts,int meleeSource) {
        int[] originalKinds=kinds.clone(),originalCounts=counts.clone(),totals=new int[4];
        for(int i=0;i<kinds.length;i++) totals[kinds[i]]+=counts[i];
        var plan=WeaponSlotAllocation.plan(kinds,counts,meleeSource);
        boolean fits=totals[1]<=1 && totals[2]<=2 && totals[3]<=1;
        check(plan.fits()==fits,"capacity preflight");
        check(Arrays.equals(kinds,originalKinds) && Arrays.equals(counts,originalCounts),"preflight never mutates source");
        check(plan.pistols()==totals[1] && plan.primaries()==totals[2] && plan.melee()==totals[3],"message reports exact totals");
        int[] remaining=counts.clone(),assigned=new int[4],required={1,2,2,3};
        for(int slot=0;slot<4;slot++) {
            int source=plan.sources()[slot];
            if(source<0) continue;
            check(fits && source<kinds.length && kinds[source]==required[slot] && remaining[source]>0,"assignment matches role without duplication");
            remaining[source]--;assigned[required[slot]]++;
        }
        if(fits) for(int kind=1;kind<4;kind++) check(assigned[kind]==totals[kind],"every loose weapon gets an equipment slot");
    }
    public static void main(String[] args) {
        // Every combination of six loose inventory cells, including invalid excess weapons.
        for(int code=0;code<4096;code++) {
            int[] kinds=new int[6],counts=new int[6];int bits=code;
            for(int i=0;i<6;i++) { kinds[i]=bits&3;counts[i]=kinds[i]==0?0:1;bits>>=2; }
            verify(kinds,counts,5);
        }
        verify(new int[]{2,0,0,0},new int[]{2,0,0,0},3);
        verify(new int[]{2,0,0,0},new int[]{3,0,0,0},3);
        verify(new int[]{1,0,0,0},new int[]{2,0,0,0},3);
        verify(new int[]{3,0,0,0},new int[]{2,0,0,0},3);
        var stable=WeaponSlotAllocation.plan(new int[]{1,2,2,3},new int[]{1,1,1,1},3);
        check(Arrays.equals(stable.sources(),new int[]{0,1,2,3}),"correctly equipped items keep their slots");
        int[] kinds=new int[38],counts=new int[38];kinds[37]=3;counts[37]=1;kinds[4]=2;counts[4]=1;kinds[5]=2;counts[5]=1;kinds[6]=1;counts[6]=1;
        verify(kinds,counts,36);
        check(Arrays.equals(WeaponSlotAllocation.plan(kinds,counts,36).sources(),new int[]{6,4,5,37}),"misplaced and offhand weapons relocate predictably");
        System.out.println("WeaponSlotAllocationCheck: "+checks+" checks passed");
    }
}
