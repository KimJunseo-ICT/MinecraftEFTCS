package com.minecrafteft.client;

/** A readable, stylized P-QRS-T trace for game feedback, without random high-frequency noise. */
public final class InventoryVitalWaveform {
    public static final double WINDOW_SECONDS=2;
    private InventoryVitalWaveform() { }
    public static int severity(double fraction) { return fraction>.5?0:fraction>.2?1:2; }
    public static int nominalBpm(double fraction) { return switch(severity(fraction)) { case 0 -> 66;case 1 -> 108;default -> 156; }; }
    public static double sample(double fraction,double seconds) {
        if(!Double.isFinite(seconds)) return 0;
        int severity=severity(fraction);
        double rate=nominalBpm(fraction)/60D;
        // Urgent beats vary in spacing and strength, while retaining a readable quiet baseline.
        double variation=switch(severity) {
            case 0 -> 0;case 1 -> .025*Math.sin(seconds*2.1);
            default -> .11*Math.sin(seconds*3.1)+.035*Math.sin(seconds*5.7);
        };
        double cycles=seconds*rate+variation,beat=Math.floor(cycles),phase=cycles-beat;
        double p,q,r,s,t;
        if(severity==0) {
            p=.10*bump(phase,.15,.045);q=-.11*bump(phase,.275,.018);
            r=.92*bump(phase,.315,.028);s=-.22*bump(phase,.365,.025);t=.20*bump(phase,.59,.085);
        } else if(severity==1) {
            p=.065*bump(phase,.14,.035);q=-.18*bump(phase,.28,.016);
            r=1.12*bump(phase,.315,.022);s=-.38*bump(phase,.37,.028);t=.13*bump(phase,.59,.06);
        } else {
            double strength=((long)beat&1)==0?1.04:.76;
            p=.025*bump(phase,.13,.025);q=-.23*bump(phase,.275,.015);
            r=strength*bump(phase,.305,.019)+.22*bump(phase,.335,.015);
            s=-.43*bump(phase,.375,.025);t=.075*bump(phase,.57,.05);
        }
        return p+q+r+s+t+.012*Math.sin(seconds*2.2);
    }
    public record Range(double minimum,double maximum,double last) { }
    /** A narrow fast QRS peak must not vanish between two screen pixels. */
    public static Range range(double fraction,double from,double to) {
        double first=sample(fraction,from),minimum=first,maximum=first,last=first;
        for(int i=1;i<=8;i++) {
            last=sample(fraction,from+(to-from)*i/8);
            minimum=Math.min(minimum,last);maximum=Math.max(maximum,last);
        }
        return new Range(minimum,maximum,last);
    }
    private static double bump(double x,double center,double width) { double d=(x-center)/width;return Math.exp(-.5*d*d); }
}
