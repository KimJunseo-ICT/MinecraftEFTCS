import com.minecrafteft.medical.Ai2TreatmentProgress;
import com.minecrafteft.client.Ai2Motion;

/** Standalone checks for the requested timing/cancel contract; no game process. */
public final class Ai2TreatmentCheck {
    private static int assertions;
    private static void check(boolean condition,String message) { assertions++;if(!condition) throw new AssertionError(message); }
    private static void close(double a,double b,String message) { check(Math.abs(a-b)<.00001,message); }
    public static void main(String[] args) {
        var full=new Ai2TreatmentProgress();
        int spent=0;float healed=0;
        for(int tick=0;tick<120;tick++) {
            var step=full.advance(tick);
            if(step.spendCharge()) { spent++;check(tick==60,"charge consumed precisely at treatment entry"); }
            if(tick<60 || tick>=100) close(step.heal(),0,"prepare and cleanup never heal");
            else close(step.heal(),.5,"each of forty treatment ticks heals half a point");
            healed+=step.heal();
        }
        check(spent==1,"one complete use costs exactly one charge");
        close(healed,20,"complete treatment restores twenty points");
        close(full.advance(119).heal(),0,"duplicate tick cannot heal twice");
        check(!full.advance(119).spendCharge(),"duplicate tick cannot consume another charge");
        for(int stop:new int[]{0,30,59}) {
            var early=new Ai2TreatmentProgress();
            for(int tick=0;tick<=stop;tick++) early.advance(tick);
            early.cancel();
            check(!early.spent(),"cancel before treatment costs no durability");
            close(early.advance(99).heal(),0,"cancelled preparation never heals later");
        }
        var partial=new Ai2TreatmentProgress();
        for(int tick=0;tick<80;tick++) partial.advance(tick);
        partial.cancel();
        check(partial.spent(),"mid-treatment cancellation keeps spent charge");
        close(partial.healed(),10,"one second of treatment keeps only ten restored points");
        close(partial.advance(99).heal(),0,"no recovery after interruption");
        var retry=new Ai2TreatmentProgress();
        check(!retry.advance(59).spendCharge(),"next use starts with a new preparation stage");
        check(retry.advance(60).spendCharge(),"next treatment consumes its own charge once");
        check(full.finishedHealing() && !partial.finishedHealing(),"completed and interrupted healing remain distinguishable");
        var start=Ai2Motion.sample(0);var end=Ai2Motion.sample(Ai2TreatmentProgress.READY_SECONDS);
        close(start.kit().x(),end.kit().x(),"kit returns to idle X");
        close(start.kit().y(),end.kit().y(),"kit returns to idle Y");
        close(start.kit().z(),end.kit().z(),"kit returns to idle Z");
        close(start.right().distance(end.right()),0,"working hand returns to its idle grip");
        var lowered=Ai2Motion.sample(Ai2TreatmentProgress.USE_SECONDS);
        check(lowered.kit().y()<-.85 && lowered.right().y()<-.80 && !lowered.syringeVisible(),
                "kit and working hand leave the view after discarding the injector");
        check(Ai2Motion.sample(5.76F).kit().y()>-.25,"grip is visible before lowering");
        var rising=Ai2Motion.sample((Ai2TreatmentProgress.USE_SECONDS+Ai2TreatmentProgress.READY_SECONDS)/2);
        check(lowered.kit().y()<rising.kit().y() && rising.kit().y()<end.kit().y(),"kit rises during the delay");
        check(Ai2Motion.sample(1.35F).lid()>.99,"case opens to expose contents");
        check(Ai2Motion.sample(3).lid()==0,"case closed during injection");
        for(int frame=0;frame<=Math.round(Ai2TreatmentProgress.READY_SECONDS*240);frame++) {
            var pose=Ai2Motion.sample(frame/240F);
            check(Float.isFinite(pose.left().x()) && Float.isFinite(pose.right().y()) && Float.isFinite(pose.kit().z()),"finite animation transform");
            check(pose.kit().z()<-.2,"kit remains in front of the camera");
            check(pose.lid()>=0 && pose.lid()<=1 && pose.plunger()>=0 && pose.plunger()<=1,"bounded animation channels");
            if(frame>0) {
                var previous=Ai2Motion.sample((frame-1)/240F);
                check(Math.abs(pose.right().x()-previous.right().x())<.03,"no hand teleport between high-rate frames");
            }
        }
        System.out.println("AI-2 timing, cancellation and motion checks passed: "+assertions);
    }
}
