import com.minecrafteft.client.InventoryVitalWaveform;
import java.nio.file.*;
import java.util.*;

public final class InventoryVitalWaveformCheck {
    private static int checks;
    private static void check(boolean value,String message) { checks++;if(!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        check(InventoryVitalWaveform.severity(.51)==0,"51 percent is stable");
        check(InventoryVitalWaveform.severity(.5)==1,"50 percent is caution");
        check(InventoryVitalWaveform.severity(.21)==1,"21 percent is caution");
        check(InventoryVitalWaveform.severity(.2)==2,"20 percent is urgent");
        int previousPeaks=0;double stableMaximum=0,stableMinimum=0;
        for(double health:new double[]{1,.4,.1}) {
            int peaks=0,quiet=0;double previous=InventoryVitalWaveform.sample(health,0),minimum=0,maximum=0;
            double beforePrevious=previous,weakestPeak=Double.POSITIVE_INFINITY,strongestPeak=0;
            for(int i=1;i<=10000;i++) {
                double value=InventoryVitalWaveform.sample(health,i*.001);
                check(Double.isFinite(value) && value>=-.6 && value<=1.25,"bounded trace");
                check(Math.abs(value-previous)<.16,"trace remains continuous at high sampling rates");
                minimum=Math.min(minimum,value);maximum=Math.max(maximum,value);
                if(previous>.65 && previous>beforePrevious && previous>=value) {
                    weakestPeak=Math.min(weakestPeak,previous);strongestPeak=Math.max(strongestPeak,previous);
                }
                if(value>.65 && previous<=.65) peaks++;
                if(Math.abs(value)<.12) quiet++;
                beforePrevious=previous;previous=value;
            }
            check(quiet>5000,"baseline stays quiet for most of each beat");
            check(peaks>=previousPeaks+5 && peaks<=30,"severity changes pulse rate substantially without dense noise");previousPeaks=peaks;
            if(health==1) { stableMaximum=maximum;stableMinimum=minimum; }
            if(health==.4) {
                check(maximum>stableMaximum+.1,"caution has taller peaks than stable");
                check(minimum<stableMinimum-.1,"caution has deeper troughs than stable");
            }
            if(health==.1) check(strongestPeak-weakestPeak>.16,"danger visibly alternates beat strength");
        }
        for(int frame=0;frame<100;frame++) {
            int[] visiblePeaks=new int[3];double[] health={1,.4,.1};
            for(int state=0;state<3;state++) {
                boolean above=false;
                for(int pixel=0;pixel<74;pixel++) {
                    double from=frame*.037+InventoryVitalWaveform.WINDOW_SECONDS*pixel/74;
                    var range=InventoryVitalWaveform.range(health[state],from,from+InventoryVitalWaveform.WINDOW_SECONDS/74);
                    check(range.maximum()>=range.last() && range.minimum()<=range.last(),"pixel envelope includes its ending sample");
                    boolean next=range.maximum()>.65;if(next && !above) visiblePeaks[state]++;above=next;
                }
            }
            check(visiblePeaks[2]>=visiblePeaks[0]+2,"danger remains faster than stable at actual inventory resolution");
        }
        List<String> csv=new ArrayList<>();csv.add("seconds,stable,caution,urgent");
        for(int i=0;i<=2000;i++) {
            double t=i*.001;csv.add(String.format(Locale.ROOT,"%.3f,%.8f,%.8f,%.8f",t,
                    InventoryVitalWaveform.sample(1,t),InventoryVitalWaveform.sample(.4,t),InventoryVitalWaveform.sample(.1,t)));
        }
        if(args.length>0) Files.write(Path.of(args[0]),csv);
        if(args.length>1) {
            List<String> pixels=new ArrayList<>();pixels.add("pixel,stable_min,stable_max,caution_min,caution_max,danger_min,danger_max");
            for(int pixel=0;pixel<74;pixel++) {
                String line=Integer.toString(pixel);
                for(double health:new double[]{1,.4,.1}) {
                    var range=InventoryVitalWaveform.range(health,pixel*2D/74,(pixel+1)*2D/74);
                    line+=String.format(Locale.ROOT,",%.8f,%.8f",range.minimum(),range.maximum());
                }
                pixels.add(line);
            }
            Files.write(Path.of(args[1]),pixels);
        }
        System.out.println("Inventory vital waveform checks passed: "+checks);
    }
}
