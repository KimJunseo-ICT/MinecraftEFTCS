package com.minecrafteft.medical;

public enum MedikitType {
    AI2(Ai2TreatmentProgress.PREPARE_TICKS, Ai2TreatmentProgress.HEAL_TICKS,
            Ai2TreatmentProgress.USE_TICKS, Ai2TreatmentProgress.TOTAL_HEAL, Ai2TreatmentProgress.MAX_USES),
    IFAK(48, 60, 124, 40, 8);

    public final int prepareTicks, healTicks, useTicks, charges;
    public final float totalHeal;

    MedikitType(int prepareTicks, int healTicks, int useTicks, float totalHeal, int charges) {
        this.prepareTicks = prepareTicks;
        this.healTicks = healTicks;
        this.useTicks = useTicks;
        this.totalHeal = totalHeal;
        this.charges = charges;
    }

    public float readySeconds() { return (useTicks + Ai2TreatmentProgress.REUSE_DELAY_TICKS) / 20F; }
}
