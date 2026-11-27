package com.minecrafteft.status;

import java.util.*;
import java.util.function.DoubleSupplier;

/** Simulation ticks only: logging out/pausing cannot expire injuries or timers. */
public final class StatusState {
    public static final int WOUND_TICKS=6000, PAINKILLER_TICKS=1600;
    private int light,heavy,pain;
    private boolean arm,leg;
    private int lightClock,lightEnergy,heavyClock,heavyPulses,heavyEnergy,bloodClock,runClock,jumps,legRun;
    private final List<Integer> wounds=new ArrayList<>();
    private final ResourceConditions resources=new ResourceConditions();
    public ResourceConditions resources() { return resources; }
    public int light() { return light; }
    public int heavy() { return heavy; }
    public boolean arm() { return arm; }
    public boolean leg() { return leg; }
    public int painTicks() { return pain; }
    public List<Integer> wounds() { return List.copyOf(wounds); }
    public double useMultiplier() { return arm?(pain>0?1.25:1.5):1; }
    public boolean sprintBlocked() { return leg && pain==0; }
    public boolean addLight() {
        if(light==3) return false;
        if(light++==0) { lightClock=0;lightEnergy=1200; } return true;
    }
    public boolean addHeavy() {
        if(heavy==3) return false;
        if(heavy++==0) { heavyClock=0;heavyPulses=0;heavyEnergy=1200;bloodClock=60; } return true;
    }
    public boolean fracture(double roll) {
        if(arm && leg) return false;
        if(arm) leg=true;else if(leg) arm=true;else if(roll<.6) arm=true;else leg=true;
        return true;
    }
    /** A landing cannot injure an arm or turn an existing leg fracture into an arm fracture. */
    public boolean fractureLeg() {
        if(leg) return false;
        leg=true;
        return true;
    }
    public boolean splint() { if(leg) { leg=false;return true; }if(arm) { arm=false;return true; }return false; }
    public void painkillers() { pain=PAINKILLER_TICKS; }
    public boolean cureLight() { if(light==0) return false;light--;return true; }
    public boolean cureHeavy() { if(heavy==0) return false;heavy--;wounds.add(WOUND_TICKS);return true; }
    public void reopen(boolean severe,DoubleSupplier random) {
        double chance=severe?.03:.05;
        for(int i=wounds.size()-1;i>=0;i--)
            if(random.getAsDouble()<chance && (severe?addHeavy():addLight())) wounds.remove(i);
        if(wounds.isEmpty()) { runClock=0;jumps=0; }
    }
    public void jump(DoubleSupplier random) {
        if(wounds.isEmpty()) { jumps=0;return; }
        if(++jumps==5) { jumps=0;reopen(false,random); }
    }
    public static double fallChance(double height) {
        return height<3?0:height<5?.01:height<8?.10:height<12?.35:.70;
    }
    public record Pulse(double bleedingDamage,double fractureDamage,int energyLoss,boolean hurtCue,boolean blood) {
        public double damage() { return bleedingDamage+fractureDamage; }
    }
    public Pulse tick(boolean sprinting,DoubleSupplier random) {
        double damage=0,fractureDamage=0;int energyLoss=0;boolean cue=false,blood=false;
        if(light>0) {
            if(lightClock--<=0) { damage+=4+light;cue=heavy==0;lightClock=199; }
            if(--lightEnergy<=0) { energyLoss+=5;lightEnergy=1200; }
        }
        if(heavy>0) {
            if(heavyClock--<=0) { damage+=1+(heavy-1)/8D;heavyClock=19; }
            // Visual/audio feedback follows elapsed eight-second cycles, not the immediate first pulse.
            if(++heavyPulses>=160) { heavyPulses=0;cue=true; }
            if(--heavyEnergy<=0) { energyLoss+=6;heavyEnergy=1200; }
            if(--bloodClock<=0) { blood=true;bloodClock=60; }
        }
        // Expiration precedes movement rolls: an already healed wound cannot reopen.
        for(int i=wounds.size()-1;i>=0;i--) {
            int left=wounds.get(i)-1;if(left<=0) wounds.remove(i);else wounds.set(i,left);
        }
        if(sprinting && !wounds.isEmpty()) {
            if(++runClock==200) { runClock=0;reopen(false,random); }
        } else runClock=0;
        if(wounds.isEmpty()) jumps=0;
        if(sprinting && leg && pain>0) {
            // Half-tick units preserve the exact 2.5x interval: 38 and 37 ticks alternate.
            if((legRun+=2)>=75) { fractureDamage+=.5;legRun-=75; }
        } else legRun=0;
        if(pain>0) pain--;
        return new Pulse(damage,fractureDamage,energyLoss,cue,blood);
    }
    /** Persist counters as well as effects, so reconnecting cannot reset bleeding costs. */
    public int[] snapshot() {
        int[] values=new int[15+ResourceConditions.SNAPSHOT_SIZE+wounds.size()];
        values[0]=-2;
        int[] head={light,heavy,arm?1:0,leg?1:0,pain,lightClock,lightEnergy,heavyClock,heavyPulses,heavyEnergy,bloodClock,runClock,jumps,legRun};
        System.arraycopy(head,0,values,1,14);
        System.arraycopy(resources.snapshot(),0,values,15,ResourceConditions.SNAPSHOT_SIZE);
        for(int i=0;i<wounds.size();i++) values[15+ResourceConditions.SNAPSHOT_SIZE+i]=wounds.get(i);
        return values;
    }
    public void restore(int[] v) {
        if(v.length>=15+ResourceConditions.SNAPSHOT_SIZE && v[0]==-2) {
            resources.restore(v,15);
            int woundStart=15+ResourceConditions.SNAPSHOT_SIZE;
            int[] legacy=new int[14+Math.min(1024,v.length-woundStart)];
            System.arraycopy(v,1,legacy,0,14);
            System.arraycopy(v,woundStart,legacy,14,legacy.length-14);
            v=legacy;
        } else resources.restore(new int[ResourceConditions.SNAPSHOT_SIZE],0);
        if(v.length<14) return;
        light=limit(v[0],3);heavy=limit(v[1],3);arm=v[2]!=0;leg=v[3]!=0;pain=limit(v[4],PAINKILLER_TICKS);
        lightClock=limit(v[5],199);lightEnergy=limit(v[6],1200);heavyClock=limit(v[7],19);heavyPulses=limit(v[8],159);
        heavyEnergy=limit(v[9],1200);bloodClock=limit(v[10],60);runClock=limit(v[11],199);jumps=limit(v[12],4);legRun=limit(v[13],74);
        wounds.clear();for(int i=14;i<Math.min(v.length,1038);i++) if(v[i]>0) wounds.add(limit(v[i],WOUND_TICKS));
    }
    private static int limit(int value,int maximum) { return Math.max(0,Math.min(maximum,value)); }
}
