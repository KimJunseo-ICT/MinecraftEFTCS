import com.minecrafteft.status.StatusState;
import com.minecrafteft.medical.*;
import java.util.*;

public final class StatusSystemCheck {
    private static int checks;
    private static void check(boolean value,String message) { checks++;if(!value) throw new AssertionError(message); }
    private static void close(double value,double expected,String message) { check(Math.abs(value-expected)<1e-6,message+": "+value); }
    private static StatusState bleeding(int light,int heavy) {
        var state=new StatusState();for(int i=0;i<light;i++) state.addLight();for(int i=0;i<heavy;i++) state.addHeavy();return state;
    }
    public static void main(String[] args) {
        for(int stacks=1;stacks<=3;stacks++) {
            var light=bleeding(stacks,0);double damage=0;int energy=0,cues=0;
            for(int t=0;t<1200;t++) { var pulse=light.tick(false,()->1);damage+=pulse.damage();energy+=pulse.energyLoss();if(pulse.hurtCue()) cues++;
                if(t==0) close(damage,4+stacks,"light bleeding starts immediately");
                if(t<1199) check(energy==0,"no premature energy loss");
            }
            close(damage,6*(4+stacks),"six light pulses per minute");check(cues==6 && energy==5,"light energy cost independent of stack count");
            var heavy=bleeding(0,stacks);damage=0;energy=0;cues=0;int blood=0;
            for(int t=0;t<1200;t++) {
                var pulse=heavy.tick(false,()->1);damage+=pulse.damage();energy+=pulse.energyLoss();if(pulse.hurtCue()) cues++;if(pulse.blood()) blood++;
                if(t==0) close(damage,1+(stacks-1)/8D,"first heavy damage is gradual");
                if(t==159) { close(damage,7+stacks,"eight-second total");check(cues==1,"one cue for the full heavy cycle"); }
                if(t<1199) check(energy==0,"heavy energy starts after one minute");
                if(t<59) check(blood==0,"blood starts after three seconds");
            }
            check(energy==6 && blood==20 && cues==7,"heavy energy, decal interval and batched cues");
            close(damage,60*(1+(stacks-1)/8D),"heavy per-second loss continues");
        }
        var state=bleeding(3,3);check(!state.addLight() && !state.addHeavy(),"both bleeding types cap at three");
        check(state.cureLight() && state.light()==2 && state.wounds().isEmpty(),"light cure removes only one without wound");
        state.cureHeavy();check(state.heavy()==2 && state.wounds().equals(List.of(6000)),"one heavy cure creates exactly one fresh wound");
        for(int t=0;t<1000;t++) state.tick(false,()->1);
        state.cureHeavy();check(state.wounds().equals(List.of(5000,6000)),"wounds retain independent lifetimes");
        for(int t=0;t<5000;t++) state.tick(false,()->1);
        check(state.wounds().equals(List.of(1000)),"first wound expires without expiring the second");
        for(int t=0;t<1000;t++) state.tick(false,()->1);check(state.wounds().isEmpty(),"second wound ends at its own five minutes");
        var moving=bleeding(0,2);moving.cureHeavy();moving.cureHeavy();
        for(int t=0;t<199;t++) moving.tick(true,()->0);check(moving.light()==0,"no sprint roll before ten seconds");
        moving.tick(false,()->0);moving.tick(true,()->0);check(moving.light()==0,"stopping resets consecutive sprint time");
        for(int t=1;t<200;t++) moving.tick(true,()->0);
        check(moving.light()==2 && moving.wounds().isEmpty(),"each wound independently reopens after ten seconds of sprinting");
        var jumping=bleeding(0,1);jumping.cureHeavy();
        for(int i=0;i<4;i++) jumping.jump(()->0);check(jumping.light()==0,"first four jumps do not roll");
        jumping.jump(()->0);check(jumping.light()==1 && jumping.wounds().isEmpty(),"fifth jump can reopen a wound");
        var hit=bleeding(0,2);hit.cureHeavy();hit.cureHeavy();
        hit.reopen(true,()->.031);check(hit.heavy()==0,"fresh wound hit chance is three percent");
        var rolls=new ArrayDeque<>(List.of(.029,.031));hit.reopen(true,()->rolls.removeFirst());
        check(hit.heavy()==1 && hit.wounds().size()==1,"separate wounds make independent hit rolls");
        var capped=bleeding(3,1);capped.cureHeavy();capped.reopen(false,()->0);check(capped.wounds().size()==1,"full bleeding cap does not destroy a wound");
        for(double[] pair:new double[][]{{2.99,0},{3,.01},{4.99,.01},{5,.1},{7.99,.1},{8,.35},{11.99,.35},{12,.7},{100,.7}})
            close(StatusState.fallChance(pair[0]),pair[1],"fall probability boundary");
        var broken=new StatusState();broken.fracture(.59);check(broken.arm() && !broken.leg(),"first arm fracture probability");
        close(broken.useMultiplier(),1.5,"arm delay");broken.fracture(0);check(broken.leg() && !broken.fracture(1),"second fracture is other limb and capped");
        check(broken.sprintBlocked(),"leg blocks sprint without painkillers");broken.painkillers();close(broken.useMultiplier(),1.25,"painkiller arm mitigation");
        check(!broken.sprintBlocked(),"painkillers unlock sprint");
        for(int t=0;t<14;t++) close(broken.tick(true,()->1).damage(),0,"no early leg running damage");
        close(broken.tick(true,()->1).damage(),.5,"leg damage at fifteen ticks");
        broken.tick(false,()->1);for(int t=0;t<14;t++) close(broken.tick(true,()->1).damage(),0,"leg running timer resets on stop");
        broken.painkillers();check(broken.painTicks()==1600,"redose refreshes rather than stacks");
        broken.splint();check(!broken.leg() && broken.arm(),"splint prioritizes leg");broken.splint();check(!broken.arm() && !broken.splint(),"splint removes only existing fractures");
        for(int t=0;t<1600;t++) broken.tick(false,()->1);check(broken.painTicks()==0,"painkillers expire in eighty seconds");
        var original=bleeding(2,3);original.cureHeavy();original.fracture(0);original.painkillers();
        for(int t=0;t<117;t++) original.tick(true,()->1);
        var copy=new StatusState();copy.restore(original.snapshot());
        for(int t=0;t<7000;t++) { check(original.tick(t%80<60,()->1).equals(copy.tick(t%80<60,()->1)),"restored counters remain synchronized");check(Arrays.equals(original.snapshot(),copy.snapshot()),"snapshot roundtrip preserves all state"); }
        for(MedikitType type:MedikitType.values()) {
            var both=bleeding(1,1);
            var plan=TreatmentPlan.choose(type,3,both,true);
            check(plan.target()==(type==MedikitType.IFAK?TreatmentPlan.Target.HEAVY:TreatmentPlan.Target.LIGHT),"priority and kit capability");
            check(TreatmentPlan.choose(type,0,both,true).target()==TreatmentPlan.Target.NONE,"empty kit cannot be used");
            for(int elapsed=0;elapsed<type.useTicks;elapsed++) {
                var progress=new MedikitTreatmentProgress(type,plan);
                for(int tick=0;tick<=elapsed;tick++) { var step=progress.advance(tick);check(!step.spendCharge() && step.heal()==0,"bleeding cannot consume or heal before final cleanup"); }
                check(progress.cancel()==(elapsed>=type.prepareTicks?1:0),"bleeding cancel cost at phase boundary");
                check(progress.cancel()==0 && progress.finish()==0,"ending a canceled session cannot charge twice");
            }
            var progress=new MedikitTreatmentProgress(type,plan);progress.advance(type.useTicks-1);
            check(progress.finish()==plan.cost() && progress.cancel()==0,"completed bleeding commits exact cost once");
            var healthPlan=TreatmentPlan.choose(type,1,new StatusState(),true);progress=new MedikitTreatmentProgress(type,healthPlan);
            double healed=0;int costs=0;
            for(int t=0;t<type.useTicks;t++) { var step=progress.advance(t);healed+=step.heal();if(step.spendCharge()) costs++; }
            close(healed,type.totalHeal,"arm timing must not alter total HP");check(costs==1 && progress.finish()==0,"HP path spends once on treatment entry");
        }
        check(TreatmentPlan.choose(MedikitType.IFAK,2,bleeding(1,1),true).target()==TreatmentPlan.Target.LIGHT,"insufficient heavy charges falls back to light");
        check(TreatmentPlan.choose(MedikitType.AI2,1,bleeding(1,1),true).target()==TreatmentPlan.Target.HEALTH,"insufficient bleed charges falls back to HP");
        check(TreatmentPlan.choose(MedikitType.AI2,1,bleeding(1,1),false).target()==TreatmentPlan.Target.NONE,"cannot waste a charge at full HP without a treatable target");
        System.out.println("Status system checks passed: "+checks);
    }
}
