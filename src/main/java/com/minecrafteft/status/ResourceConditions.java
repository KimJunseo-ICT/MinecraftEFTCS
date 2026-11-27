package com.minecrafteft.status;

import com.minecrafteft.vitals.VitalState;

/** Resource-triggered conditions and hidden fatigue history, all measured in simulation ticks. */
public final class ResourceConditions {
    public static final int SNAPSHOT_SIZE=15;
    private final Depletion dehydration=new Depletion(),malnutrition=new Depletion();
    private int fatigue,fatigueTicks,hidden,hiddenAge;
    private boolean seenDanger,leftDanger,recoveredPastThirty;
    private static final class Depletion {
        int zeroTicks,grace,damageClock;
        boolean active;
        double tick(double resource,boolean activate) {
            boolean started=false;
            if(resource<=0) {
                if(!active && activate && ++zeroTicks>=1200) { active=true;damageClock=0;started=true; }
                if(active) { zeroTicks=1200;grace=200; }
            } else {
                zeroTicks=0;
                if(active && --grace<=0) { active=false;damageClock=0;grace=0; }
            }
            if(active && activate && !started && ++damageClock>=300) { damageClock=0;return 2.5; }
            return 0;
        }
        int remaining() { return active?(zeroTicks>0?-1:grace):0; }
        void clientTick() { if(active && zeroTicks==0 && grace>0 && --grace==0) active=false; }
        void restore(int zero,int recovery,int clock,int enabled) {
            zeroTicks=clamp(zero,1200);grace=clamp(recovery,200);damageClock=clamp(clock,299);active=enabled!=0;
        }
    }
    public boolean dehydrated() { return dehydration.active; }
    public boolean malnourished() { return malnutrition.active; }
    public int dehydrationTicks() { return dehydration.remaining(); }
    public int malnutritionTicks() { return malnutrition.remaining(); }
    public int fatigue() { return fatigue; }
    public int fatigueTicks() { return fatigueTicks; }
    public double energyMultiplier() { return 1+.10*fatigue; }
    public record Damage(double dehydration,double malnutrition) { }
    public Damage tick(double energy,double water,double before,double after,boolean spent) {
        tickFatigueTimers();
        boolean danger=after<=VitalState.RED_STAMINA;
        if(!seenDanger && danger) { seenDanger=true;leftDanger=false;recoveredPastThirty=false; }
        else if(seenDanger && spent && before>VitalState.RED_STAMINA && danger) {
            if(leftDanger && !recoveredPastThirty) {
                hidden++;hiddenAge=0;
                if(hidden==4) { hidden=0;fatigue=fatigue==Integer.MAX_VALUE?fatigue:fatigue+1;fatigueTicks=3600; }
            }
            leftDanger=false;recoveredPastThirty=false;
        }
        if(seenDanger && !danger) leftDanger=true;
        if(seenDanger && after>VitalState.MAX_STAMINA*.30+1e-9) recoveredPastThirty=true;
        return new Damage(dehydration.tick(water,true),malnutrition.tick(energy,true));
    }
    public void inactiveTick(double energy,double water) {
        tickFatigueTimers();dehydration.tick(water,false);malnutrition.tick(energy,false);
    }
    public void clientTick() { tickFatigueTimers();dehydration.clientTick();malnutrition.clientTick(); }
    private void tickFatigueTimers() {
        if(fatigueTicks>0 && --fatigueTicks==0) fatigue=0;
        if(hidden>0 && ++hiddenAge>=400) { hidden=0;hiddenAge=0; }
    }
    public int[] snapshot() {
        return new int[]{dehydration.zeroTicks,dehydration.grace,dehydration.damageClock,dehydration.active?1:0,
                malnutrition.zeroTicks,malnutrition.grace,malnutrition.damageClock,malnutrition.active?1:0,
                fatigue,fatigueTicks,hidden,hiddenAge,seenDanger?1:0,leftDanger?1:0,recoveredPastThirty?1:0};
    }
    public void restore(int[] values,int offset) {
        if(values.length<offset+SNAPSHOT_SIZE) { restore(new int[SNAPSHOT_SIZE],0);return; }
        dehydration.restore(values[offset],values[offset+1],values[offset+2],values[offset+3]);
        malnutrition.restore(values[offset+4],values[offset+5],values[offset+6],values[offset+7]);
        fatigue=Math.max(0,values[offset+8]);fatigueTicks=clamp(values[offset+9],3600);if(fatigueTicks==0) fatigue=0;
        hidden=clamp(values[offset+10],3);hiddenAge=clamp(values[offset+11],399);
        seenDanger=values[offset+12]!=0;leftDanger=values[offset+13]!=0;recoveredPastThirty=values[offset+14]!=0;
    }
    private static int clamp(int value,int maximum) { return Math.max(0,Math.min(maximum,value)); }
}
