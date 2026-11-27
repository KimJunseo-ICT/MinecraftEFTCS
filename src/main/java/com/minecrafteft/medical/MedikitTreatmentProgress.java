package com.minecrafteft.medical;

/** One fixed treatment target per use. Bleeding commits only after the complete animation. */
public final class MedikitTreatmentProgress {
    private final MedikitType type;
    private final TreatmentPlan plan;
    private final Ai2TreatmentProgress healing;
    private boolean entered,ended;
    public MedikitTreatmentProgress(MedikitType type,TreatmentPlan plan) { this.type=type;this.plan=plan;healing=new Ai2TreatmentProgress(type); }
    public Ai2TreatmentProgress.Step advance(int elapsed) {
        if(ended) return new Ai2TreatmentProgress.Step(false,0);
        if(elapsed>=type.prepareTicks) entered=true;
        return plan.bleeding()?new Ai2TreatmentProgress.Step(false,0):healing.advance(elapsed);
    }
    public int finish() { if(ended) return 0;ended=true;return plan.bleeding()?plan.cost():0; }
    public int cancel() { if(ended) return 0;ended=true;healing.cancel();return plan.bleeding() && entered?1:0; }
}
