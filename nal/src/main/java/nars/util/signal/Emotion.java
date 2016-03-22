package nars.util.signal;

import com.codahale.metrics.*;
import com.codahale.metrics.ConsoleReporter.Builder;
import nars.Memory;
import nars.task.Task;
import nars.util.event.FrameReaction;
import nars.util.meter.event.DoubleMeter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;

/**
 * emotion state: self-felt internal mental states; variables used to record emotional values
 */
public final class Emotion extends MetricRegistry {

    /** priority rate of Task processing attempted */
    public final Meter busy;

    /** priority rate of Task processing which had no effect */
    public final Meter frustration;

    /** task priority overflow rate */
    public final Meter stress;

    /** happiness rate */
    public final Meter happy;

    public final Logger logger;



    public Emotion() {
        super();

        logger = LoggerFactory.getLogger(Emotion.class);

        this.busy = meter("busy");
        this.happy = meter("happy");
        this.stress = meter("stress");
        this.frustration = meter("frustration");

    }


    public void print(PrintStream output) {

        ConsoleReporter.forRegistry(this).outputTo(output).build()
            .report(getGauges(), getCounters(), getHistograms(), getMeters(), getTimers());

    }

    @Override
    public String toString() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);
        print(ps);
        try {
            return os.toString("UTF8");
        } catch (UnsupportedEncodingException e) {
            return e.toString();
        }
    }

    //    @Override
//    public final void onFrame() {
//        commitHappy();
//
//        commitBusy();
//    }

//    public float happy() {
//        return (float)happyMeter.get();
//    }
//
//    public float busy() {
//        return (float)busyMeter.get();
//    }


    //TODO use Meter subclass that will accept and transform these float parameters

    @Deprecated public void happy(float delta) {
        happy.mark( f2l(delta) );
    }
    @Deprecated public void busy(float pri) {
        busy.mark( f2l(pri) );
    }
    @Deprecated public void stress(float pri) {
        stress.mark( f2l(pri) );
    }
    @Deprecated public void frustration(float pri) {
        frustration.mark( f2l(pri) );
    }

/*    public void busy(@NotNull Task cause, float activation) {
        busy += cause.pri() * activation;
    }*/


    /** float to long at the default conversion precision */
    private static long f2l(float f) {
        return (long)(f * 1000f); //0.001 precision
    }
    /** float to long at the default conversion precision */
    private static float l2f(long l) {
        return l / 1000f; //0.001 precision
    }

//    public void happy(float solution, @NotNull Task task) {
//        happy += ( task.getBudget().summary() * solution );
//    }

//    protected void commitHappy() {
//
//
////        if (lasthappy != -1) {
////            //float frequency = changeSignificance(lasthappy, happy, Global.HAPPY_EVENT_CHANGE_THRESHOLD);
//////            if (happy > Global.HAPPY_EVENT_HIGHER_THRESHOLD && lasthappy <= Global.HAPPY_EVENT_HIGHER_THRESHOLD) {
//////                frequency = 1.0f;
//////            }
//////            if (happy < Global.HAPPY_EVENT_LOWER_THRESHOLD && lasthappy >= Global.HAPPY_EVENT_LOWER_THRESHOLD) {
//////                frequency = 0.0f;
//////            }
////
//////            if ((frequency != -1) && (memory.nal(7))) { //ok lets add an event now
//////
//////                Inheritance inh = Inheritance.make(memory.self(), satisfiedSetInt);
//////
//////                memory.input(
//////                        TaskSeed.make(memory, inh).judgment()
//////                                .truth(frequency, Global.DEFAULT_JUDGMENT_CONFIDENCE)
//////                                .occurrNow()
//////                                .budget(Global.DEFAULT_JUDGMENT_PRIORITY, Global.DEFAULT_JUDGMENT_DURABILITY)
//////                                .reason("Happy Metabelief")
//////                );
//////
//////                if (Global.REFLECT_META_HAPPY_GOAL) { //remind on the goal whenever happyness changes, should suffice for now
//////
//////                    //TODO convert to fluent format
//////
//////                    memory.input(
//////                            TaskSeed.make(memory, inh).goal()
//////                                    .truth(frequency, Global.DEFAULT_GOAL_CONFIDENCE)
//////                                    .occurrNow()
//////                                    .budget(Global.DEFAULT_GOAL_PRIORITY, Global.DEFAULT_GOAL_DURABILITY)
//////                                    .reason("Happy Metagoal")
//////                    );
//////
//////                    //this is a good candidate for innate belief for consider and remind:
//////
//////                    if (InternalExperience.enabled && Global.CONSIDER_REMIND) {
//////                        Operation op_consider = Operation.op(Product.only(inh), consider.consider);
//////                        Operation op_remind = Operation.op(Product.only(inh), remind.remind);
//////
//////                        //order important because usually reminding something
//////                        //means it has good chance to be considered after
//////                        for (Operation o : new Operation[]{op_remind, op_consider}) {
//////
//////                            memory.input(
//////                                    TaskSeed.make(memory, o).judgment()
//////                                            .occurrNow()
//////                                            .truth(1.0f, Global.DEFAULT_JUDGMENT_CONFIDENCE)
//////                                            .budget(Global.DEFAULT_JUDGMENT_PRIORITY * InternalExperience.INTERNAL_EXPERIENCE_PRIORITY_MUL,
//////                                                    Global.DEFAULT_JUDGMENT_DURABILITY * InternalExperience.INTERNAL_EXPERIENCE_DURABILITY_MUL)
//////                                            .reason("Happy Remind/Consider")
//////                            );
//////                        }
//////                    }
//////                }
//////            }
//////        }
////        }
//
//        happyMeter.set(happy);
//
//        /*if (happy > 0)
//            happy *= happinessFade;*/
//    }

//    /** @return -1 if no significant change, 0 if decreased, 1 if increased */
//    private static float changeSignificance(float prev, float current, float proportionChangeThreshold) {
//        float range = Math.max(prev, current);
//        if (range == 0) return -1;
//        if (prev - current > range * proportionChangeThreshold)
//            return -1;
//        if (current - prev > range * proportionChangeThreshold)
//            return 1;
//
//        return -1;
//    }




//    protected void commitBusy() {
//
//        if (lastbusy != -1) {
//            //float frequency = -1;
//            //float frequency = changeSignificance(lastbusy, busy, Global.BUSY_EVENT_CHANGE_THRESHOLD);
//            //            if (busy > Global.BUSY_EVENT_HIGHER_THRESHOLD && lastbusy <= Global.BUSY_EVENT_HIGHER_THRESHOLD) {
////                frequency = 1.0f;
////            }
////            if (busy < Global.BUSY_EVENT_LOWER_THRESHOLD && lastbusy >= Global.BUSY_EVENT_LOWER_THRESHOLD) {
////                frequency = 0.0f;
////            }
//
//
//            /*
//            if (Global.REFLECT_META_BUSY_BELIEF && (frequency != -1) && (memory.nal(7))) { //ok lets add an event now
//                final Inheritance busyTerm = Inheritance.make(memory.self(), BUSYness);
//
//                memory.input(
//                        TaskSeed.make(memory, busyTerm).judgment()
//                                .truth(frequency, Global.DEFAULT_JUDGMENT_CONFIDENCE)
//                                .occurrNow()
//                                .budget(Global.DEFAULT_JUDGMENT_PRIORITY, Global.DEFAULT_JUDGMENT_DURABILITY)
//                                .reason("Busy")
//                );
//            }
//            */
//        }
//
//        busyMeter.set(lastbusy = busy);
//
//        busy = 0;
//
//
//    }
//
//    public void clear() {
//        busy = 0;
//        happy = 0;
//    }
}
