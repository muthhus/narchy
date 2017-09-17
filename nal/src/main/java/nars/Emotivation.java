package nars;

import com.netflix.servo.monitor.BasicGauge;
import com.netflix.servo.monitor.LongGauge;
import jcog.Util;
import nars.concept.Concept;
import nars.control.Cause;
import nars.task.ITask;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public synchronized void cycle() {
        long deltaSinceLastCycle = -(lastCycleTime - (lastCycleTime = nar.time()));
        long deltaRealtimeSinceLastCycle = -(this.lastRealTime - (this.lastRealTime = System.currentTimeMillis()));
        cycleDT.set(deltaSinceLastCycle);
        cycleDTReal.addValue(deltaRealtimeSinceLastCycle/1000.0);

        super.cycle();
    }



    @Override
    public void onAnswer(Task question, @Nullable Task answer) {
        super.onAnswer(question, answer);

        //reward answer for answering the question
        float str = answer.conf() * question.priSafe(0);
        //value(Cause.Purpose.Answer, question.cause(), -str);
        value(Cause.Purpose.Answer, answer.cause(), str);
    }

    /**
     * adjusts the task priority
     */
    public void evaluate(Task x) {

        float gain = nar.privaluate(x, x.cause());
        assert (gain == gain);
        if (gain != 0) {

            float amp =
                    //Util.tanhFast(gain) + 1f; //[0..+2]
                    //0.5f + (Util.tanhFast(gain)/2f);
                    1f + Util.tanhFast(gain)*0.75f;

            //amp = Math.max(amp, 0.1f);

            x.priMult(
                //amp
                //amp*amp
                amp
            );
        }
    }

    @Override
    public @Nullable ITask onInput(@NotNull ITask x) {


        if (x instanceof Task && !((Task) x).isCommand()) {
            Task t = (Task) x;

            //float p0 = t.priSafe(0);
            float cost = Param.inputCost(t, nar);

            //((NALTask)t).causeAppend(nar.taskCauses.get(t));

            if (cost != 0) {
                value(Cause.Purpose.Input, t.cause(), cost);
            }

            evaluate(t);
        }

        return x;
    }

    @Override
    public void onActivate(@NotNull Task t, float activation, Concept origin, NAR n) {
        super.onActivate(t, activation, origin, n);

        short[] x = t.cause();
        int xl = x.length;
        if (xl > 0) {
            float conceptValue = origin.value(t, activation, n.time(), n);
            if (conceptValue!=0)
                value(Cause.Purpose.Process, x, conceptValue);
        }

    }

//    public static float preferConfidentAndRelevant(@NotNull Task t, float activation, long when, NAR n) {
//        return 0.001f * activation * (t.isBeliefOrGoal() ? t.conf(when, n.dur()) : 0.5f);
//    }

}
