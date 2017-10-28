package nars;

import com.netflix.servo.monitor.BasicGauge;
import com.netflix.servo.monitor.LongGauge;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import static nars.NInner.id;

/**
 * value-reinforceing emotion implementation
 * use:         n.setEmotion(new Emotivation(n));
 */
public class Emotivation extends Emotion {

    private final NAR nar;

    public final LongGauge cycleDT = new LongGauge(id("cycle time"));
    public final DescriptiveStatistics cycleDTReal = new DescriptiveStatistics(4 /* cycles */); //realtime

    public final BasicGauge<Float> cycleDTRealMean = new BasicGauge<>(id("cycle time real mean"), () -> (float) cycleDTReal.getMean());
    public final BasicGauge<Float> cycleDTRealVary = new BasicGauge<>(id("cycle time real vary"), () -> (float) cycleDTReal.getVariance());

    long lastCycleTime, lastRealTime;

    public Emotivation(NAR n) {
        super(n);
        this.nar = n;
        lastCycleTime = n.time();
        lastRealTime = System.currentTimeMillis();

//        final StatsConfig statsConfig = new StatsConfig.Builder()
//                .withSampleSize(100)
//                //.withPercentiles(percentiles)
//                .withComputeFrequencyMillis(2000)
//                .withPublishTotal(false)
//                .withPublishCount(false)
//                .withPublishMean(true)
//                .withPublishVariance(true)
//                //.withPublishStdDev(true)
//                .build();
//        cycleDTReal = new StatsTimer(id("cycle time real"), statsConfig);

        if (getClass()==Emotivation.class) //HACK
            registerFields(this);
    }


    @Override
    public void cycle() {
        long deltaSinceLastCycle = -(lastCycleTime - (lastCycleTime = nar.time()));
        long deltaRealtimeSinceLastCycle = -(this.lastRealTime - (this.lastRealTime = System.currentTimeMillis()));
        cycleDT.set(deltaSinceLastCycle);
        cycleDTReal.addValue(deltaRealtimeSinceLastCycle/1000.0);

        super.cycle();
    }


//    public static float preferConfidentAndRelevant(@NotNull Task t, float activation, long when, NAR n) {
//        return 0.001f * activation * (t.isBeliefOrGoal() ? t.conf(when, n.dur()) : 0.5f);
//    }

}

