import com.minecrafteft.vitals.VitalState;
import com.minecrafteft.client.VitalGaugeAnimation;

/** Standalone arithmetic/state-transition checks; never launches Minecraft. */
public final class VitalStateCheck {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void close(double a, double b, String message) { check(Math.abs(a-b)<.000001,message); }
    private static void tick(VitalState state, double seconds, boolean sprint, boolean moving) {
        state.tick(seconds,sprint,moving,1.92,2.17,8,8,12);
    }
    public static void main(String[] args) {
        var state=new VitalState();
        close(state.health(),100,"initial independent health");
        tick(state,60,false,false);
        close(state.energy(),98.08,"energy drains per minute");
        close(state.hydration(),97.83,"water drains per minute");
        state.damage(40);state.heal(4);
        close(state.health(),64,"damage and healing use HP, not hunger");
        state.heal(1000);close(state.health(),100,"healing cap");
        state.energy(-1000);state.hydrate(-1000);
        close(state.deprivationDamage(1,2),3,"both empty resources damage");
        state.energy(10);close(state.deprivationDamage(1,2),2,"only water empty");
        state.hydrate(20);state.energy(-10);close(state.deprivationDamage(1,2),1,"only energy empty");
        state.energy(500);state.hydrate(500);
        close(state.energy(),100,"energy cannot exceed full");
        close(state.hydration(),100,"hydration cannot exceed full");
        state.damage(100);state.heal(50);close(state.health(),0,"normal heal cannot revive dead player");
        state.revive(1);close(state.health(),1,"totem can revive explicitly");

        var running=new VitalState();
        running.update(100,100,100,16,false);
        tick(running,.25,true,true);
        close(running.stamina(),14,"sprint enters red");
        check(!running.sprintLocked(),"may keep current sprint going in red");
        tick(running,.05,false,true);
        check(running.sprintLocked(),"stopping sprint in red locks re-entry");
        tick(running,.05,true,true);
        close(running.stamina(),14.8,"locked sprint attempt recovers instead of consuming");
        check(running.sprintLocked(),"still locked below fifteen");
        tick(running,.025,false,true);
        close(running.stamina(),15,"boundary is exactly fifteen");
        check(running.sprintLocked(),"exactly fifteen is still red/locked");
        tick(running,.05,false,true);
        check(!running.sprintLocked(),"unlocks only above fifteen");
        tick(running,2,true,true);
        close(running.stamina(),0,"exhausted stamina clamps to zero");
        check(running.sprintLocked(),"exhaustion locks sprint");
        var stopped=new VitalState();
        stopped.update(100,100,100,16,false);tick(stopped,.25,true,true);tick(stopped,.05,false,false);
        check(stopped.sprintLocked(),"releasing sprint in red locks re-entry");
        var jitter=new VitalState();
        jitter.update(100,100,100,16,false);tick(jitter,.25,true,true);tick(jitter,.05,true,false);
        check(!jitter.sprintLocked(),"stationary/network tick must not end a continuing red sprint");
        double beforeRun=jitter.stamina();tick(jitter,.05,true,true);
        check(jitter.stamina()<beforeRun && !jitter.sprintLocked(),"continuing red sprint still consumes normally");
        var lowStart=new VitalState();lowStart.update(100,100,100,14,false);tick(lowStart,.05,true,true);
        check(lowStart.sprintLocked(),"cannot begin a new sprint while already red");
        var walk=new VitalState();var idle=new VitalState();
        walk.update(100,100,100,50,false);idle.update(100,100,100,50,false);
        tick(walk,1,false,true);tick(idle,1,false,false);
        check(idle.stamina()>walk.stamina(),"idle recovers faster than walking");
        var repeat=new VitalState();
        for(int i=0;i<1200;i++) tick(repeat,.05,false,false);
        close(repeat.energy(),98.08,"tick-sized time gives same energy drain");
        close(repeat.hydration(),97.83,"tick-sized time gives same water drain");

        close(VitalState.savedHealth(200,200),100,"full legacy health migrates to full new health");
        close(VitalState.savedHealth(160,200),80,"injured legacy health preserves percentage");
        close(VitalState.savedHealth(60,100),60,"new health saves must not be halved again");
        close(VitalState.savedHealth(0,200),0,"dead legacy health remains zero");
        var depleted=new VitalState();depleted.energy(-100);
        for(int i=0;i<59;i++) close(depleted.tickDeprivation(.05,3,1,1),0,"no damage during initial grace");
        close(depleted.tickDeprivation(.05,3,1,1),1,"first hit after full three seconds");
        for(int i=0;i<59;i++) close(depleted.tickDeprivation(.05,3,1,1),0,"pause between later hits");
        close(depleted.tickDeprivation(.05,3,1,1),1,"second spaced hit");
        depleted.hydrate(-100);
        close(depleted.tickDeprivation(3,3,1,1),2,"both depleted resources share one combined pulse");
        depleted.tickDeprivation(2,3,1,1);
        depleted.energy(5);depleted.hydrate(5);
        close(depleted.tickDeprivation(.05,3,1,1),0,"drinking/eating stops damage");
        close(depleted.deprivationElapsed(),0,"recovering resources resets grace");
        depleted.energy(-5);
        close(depleted.tickDeprivation(1,3,1,1),0,"depleting again gets a fresh grace period");
        var resume=new VitalState();resume.energy(-100);resume.restoreDeprivationElapsed(2.95);
        close(resume.tickDeprivation(.05,3,1,1),1,"saved timer resumes without a free reset");

        var animation=new VitalGaugeAnimation();animation.snap(1,0);animation.target(.5,250_000_000L);
        close(animation.value(250_000_000L),1,"new snapshot does not jump display");
        close(animation.value(375_000_000L),.75,"intermediate frame interpolates smoothly");
        animation.target(.9,375_000_000L);
        close(animation.value(375_000_000L),.75,"direction change starts at visible value");
        close(animation.value(500_000_000L),.825,"recovery also interpolates");
        close(animation.value(625_000_000L),.9,"interpolation reaches target without overshoot");
        animation.target(2,2_000_000_000L);close(animation.value(2_000_000_000L),1,"stale HUD snaps to bounded current value");
        for(int i=1;i<=20;i++) animation.target(1,2_000_000_000L+i*100_000_000L);
        animation.target(.5,4_050_000_000L);
        close(animation.value(4_050_000_000L),1,"damage after idle starts interpolation rather than snapping");
        System.out.println("VitalState checks passed: "+checks);
    }
}
