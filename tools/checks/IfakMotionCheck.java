package com.minecrafteft.client;

import com.minecrafteft.medical.Ai2TreatmentProgress;
import com.minecrafteft.medical.MedikitType;
import org.joml.Vector3f;

/** Offline checks against the release jar for the IFAK's two performances and common use timer. */
public final class IfakMotionCheck {
    private static void require(boolean condition,String reason) {
        if (!condition) throw new AssertionError(reason);
    }

    public static void main(String[] args) {
        var type=MedikitType.IFAK;
        require(type.charges==8,"IFAK uses per kit");
        int charges=0, healedTicks=0;
        double appliedHeal=0;
        var use=new Ai2TreatmentProgress(type);
        for (int tick=0;tick<=type.useTicks;tick++) {
            var step=use.advance(tick);
            if (step.spendCharge()) charges++;
            if (step.heal()>0) healedTicks++;
            appliedHeal+=step.heal();
            require((step.heal()>0)==(tick>=48 && tick<108),"healing phase boundary");
            require(step.spendCharge()==(tick==48),"charge boundary");
            require(use.advance(tick).heal()==0,"duplicate tick");
        }
        require(healedTicks==60 && charges==1 && use.healed()==40 && appliedHeal==40,"complete treatment");
        for (int stop:new int[]{47,48,78,107}) {
            var cancel=new Ai2TreatmentProgress(type);
            cancel.advance(stop);
            float before=cancel.healed();
            cancel.cancel();
            require(cancel.advance(type.useTicks).heal()==0 && cancel.healed()==before,"cancel");
        }
        for (var variant:IfakMotion.Variant.values()) for(boolean ysm:new boolean[]{false,true}) {
            for (float radius:new float[]{.052F,.075F,.106F}) {
                var idle=IfakMotion.sample(0,variant,radius,ysm);
                var ready=IfakMotion.sample(type.readySeconds(),variant,radius,ysm);
                require(ready.pouch().distance(idle.pouch())<.0001 && ready.right().distance(idle.right())<.0001,"return to idle");
                require(IfakMotion.sample(type.useTicks/20F,variant,radius,ysm).pouch().y<-.9F,"kit lowered");
                var previous=idle;
                for(int frame=0;frame<=1560;frame++) {
                    float t=frame/240F;
                    var p=IfakMotion.sample(t,variant,radius,ysm);
                    require(p.tool().isFinite() && p.left().isFinite() && p.right().isFinite(),"finite pose");
                    require(previous.left().distance(p.left())<.055F && previous.right().distance(p.right())<.055F,"hand jumps");
                    if(variant==IfakMotion.Variant.WRAP && t>=.5F && t<=5.83F) {
                        var grip=p.rotation().transform(new Vector3f(-.115F,-.045F,.005F)).add(p.pouch());
                        require(grip.distance(p.left())<.0001F,"pouch leaves support hand");
                    }
                    if(t>=2.4F && t<=5.4F) {
                        require(p.toolVisible(),"treatment prop hidden");
                        if (variant==IfakMotion.Variant.INJECT) {
                            var tip=p.toolRotation().transform(new Vector3f(-IfakMotion.NEEDLE,0,0)).add(p.tool());
                            var local=p.armRotation().transformInverse(tip.sub(p.contact()));
                            require(Math.abs(local.x-radius+.008F)<.0001 && Math.abs(local.y)+Math.abs(local.z)<.0001,"needle misses");
                        } else {
                            require(Math.abs(p.pouch().y)<-p.pouch().z*.5F && Math.abs(p.pouch().x)<-p.pouch().z*.65F,
                                    "held pouch leaves view during treatment");
                            var local=p.armRotation().transformInverse(new Vector3f(p.tool()).sub(p.contact()));
                            require(local.length()>=radius+.030F && Math.abs(local.y)<.0001,"roll cuts through wrist");
                        }
                    }
                    previous=p;
                }
            }
        }
        System.out.println("IFAK timing, cancellation, both motion tracks, arm contact and re-equip passed.");
    }
}
