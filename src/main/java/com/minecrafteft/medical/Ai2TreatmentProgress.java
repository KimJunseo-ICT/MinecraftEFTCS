package com.minecrafteft.medical;

/** Prepare [0,60), heal [60,100), discard and lower [100,120), re-equip [120,126). */
public final class Ai2TreatmentProgress {
    public static final int PREPARE_TICKS=60, HEAL_TICKS=40, USE_TICKS=120, MAX_USES=5;
    public static final int REUSE_DELAY_TICKS=6;
    public static final float USE_SECONDS=USE_TICKS/20F, READY_SECONDS=(USE_TICKS+REUSE_DELAY_TICKS)/20F;
    public static final float TOTAL_HEAL=20;
    private final MedikitType type;
    public Ai2TreatmentProgress() { this(MedikitType.AI2); }
    public Ai2TreatmentProgress(MedikitType type) { this.type=type; }
    private boolean spent, cancelled;
    private int healedTicks, lastElapsed=-1;
    public record Step(boolean spendCharge,float heal) { }
    public Step advance(int elapsed) {
        if(cancelled || elapsed<=lastElapsed) return new Step(false,0);
        lastElapsed=elapsed;
        int ticks=Math.max(0,Math.min(type.healTicks,elapsed-type.prepareTicks+1));
        boolean spend=ticks>0 && !spent;
        if(spend) spent=true;
        float previousHeal=healed();
        healedTicks=ticks;
        return new Step(spend,healed()-previousHeal);
    }
    public void cancel() { cancelled=true; }
    public boolean spent() { return spent; }
    public float healed() { return type.totalHeal*healedTicks/type.healTicks; }
    public boolean finishedHealing() { return healedTicks==type.healTicks; }
}
