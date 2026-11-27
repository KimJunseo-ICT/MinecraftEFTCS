package com.minecrafteft.medical;

import com.minecrafteft.status.StatusState;

public record TreatmentPlan(Target target,int cost) {
    public enum Target { NONE,HEAVY,LIGHT,HEALTH }
    public static TreatmentPlan choose(MedikitType type,int charges,StatusState status,boolean needsHealth) {
        if(status!=null) {
            if(status.heavy()>0 && type==MedikitType.IFAK && charges>=3) return new TreatmentPlan(Target.HEAVY,3);
            int lightCost=type==MedikitType.IFAK?1:2;
            if(status.light()>0 && charges>=lightCost) return new TreatmentPlan(Target.LIGHT,lightCost);
        }
        return new TreatmentPlan(needsHealth && charges>0?Target.HEALTH:Target.NONE,1);
    }
    public boolean bleeding() { return target==Target.HEAVY || target==Target.LIGHT; }
}
