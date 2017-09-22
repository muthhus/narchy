package nars;

import com.netflix.servo.monitor.BasicCounter;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.StepCounter;
import jcog.meter.event.BufferedFloatGuage;
import nars.concept.Concept;
import nars.control.MetaGoal;
import nars.task.ITask;
import nars.term.Compound;
import nars.util.ConcurrentMonitorRegistry;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.SortedMap;

import static jcog.Texts.n4;
import static nars.NInner.id;

/**
 * emotion state: self-felt internal mental states; variables used to record emotional values
 * <p>
 * https://prometheus.io/docs/practices/instrumentation/
 * <p>
 * default impl
 */
public class Emotion extends ConcurrentMonitorRegistry {

    /**
     * priority rate of Task processing attempted
     */
    public final Counter busyPri = new StepCounter(id("busyPri"));

    public final Counter conceptFires = new BasicCounter(id("concept fire count"));
    public final Counter taskActivations = new BasicCounter(id("concept fire activations"));
    public final Counter conceptFirePremises = new BasicCounter(id("concept fire premises"));

    public final Counter taskDerived = new BasicCounter(id("task derived"));
    public final Counter taskIgnored = new BasicCounter(id("task ignored"));


//    /**
//     * setup stage, where substitution is applied to generate a conclusion term from the pattern
//     */
//    public final Counter derivationTry = new BasicCounter(id("derivation try"));

    /**
     * a successful conclusion term gets evaluated and temporalized and formed into a Task
     */
    public final Counter derivationEval = new BasicCounter(id("derivation eval"));

//    /** count of times that deriver reached ttl=0. if this is high it means more TTL should
//     * be budgeted for each derivation */
//    public final Counter derivationDeath = new BasicCounter(id("derivation death"));


    @NotNull
    public final BufferedFloatGuage busyVol;

    /**
     * priority rate of Task processing which affected concepts
     */
    @NotNull
    public final BufferedFloatGuage learnPri, learnVol;

    /**
     * task priority overflow rate
     */
    @NotNull
    public final BufferedFloatGuage stress;

    /**
     * happiness rate
     */
    @NotNull
    public final BufferedFloatGuage happy;
    private final NAR nar;

    float _happy;


    @NotNull
    public final BufferedFloatGuage confident;


    /**
     * count of errors
     */
    @NotNull
    public final BufferedFloatGuage errrVol;


//    final Map<String,AtomicInteger> counts = new ConcurrentHashMap<>();

    //private transient final Logger logger;

//    /** alertness, % active concepts change per cycle */
//    @NotNull
//    public final FloatGuage alert;


    //final ResourceMeter resourceMeter = new ResourceMeter();

    public Emotion(NAR n) {
        super();

        this.nar = n;

        this.happy = new BufferedFloatGuage("happy");

        this.busyVol = new BufferedFloatGuage("busyV");

        this.learnPri = new BufferedFloatGuage("learnP");
        this.learnVol = new BufferedFloatGuage("learnV");

        this.confident = new BufferedFloatGuage("confidence");

        this.stress = new BufferedFloatGuage("stress");

        //this.alert = new BufferedFloatGuage("alert");

        this.errrVol = new BufferedFloatGuage("error");

        if (getClass() == Emotion.class) //HACK
            registerFields(this);

    }


    /**
     * new frame started
     */
    public synchronized void cycle() {


        _happy = (float) happy.getSum();
        happy.clear();

        busyVol.clear();

        learnPri.clear();
        learnVol.clear();

        stress.clear();

        errrVol.clear();

        confident.clear();

    }

//    public void clearCounts() {
//        counts.values().forEach(x -> x.set(0));
//    }

    /**
     * percentage of business which was not frustration, by aggregate volume
     */
    public float learningVol() {
        double v = busyVol.getSum();
        if (v > 0)
            return (float) (learnVol.getSum() / v);
        return 0;
    }


    public float erring() {
        return errrVol.getSum() / busyVol.getSum();
    }


//    /** joy = first derivative of happiness, delta happiness / delta business */
//    public float joy() {
//        double b = busyPri.getSum();
//        if (b == 0)
//            return 0;
//        return (float)(happy() / b);
//    }

    public float happy() {
        return _happy;
    }

//    public void print(@NotNull OutputStream output) {
//        final FSTConfiguration conf = FSTConfiguration.createJsonConfiguration(true,false);
//        try {
//            conf.encodeToStream(output, this);
//        } catch (IOException e) {
//            try {
//                output.write(e.toString().getBytes());
//            } catch (IOException e1) {            }
//        }
//    }

//    @Override
//    public String toString() {
//        ByteArrayOutputStream os = new ByteArrayOutputStream();
//        PrintStream ps = new PrintStream(os);
//        print(ps);
//        try {
//            return os.toString("UTF8");
//        } catch (UnsupportedEncodingException e) {
//            return e.toString();
//        }
//    }

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

    @Deprecated
    public void happy(float increment) {
        happy.accept(increment);
    }

    @Deprecated
    public void busy(float pri, int vol) {
        busyPri.increment(Math.round(pri * 1000));
        busyVol.accept(vol);
    }


    public final void stress(@NotNull MutableFloat pri) {
        float v = pri.floatValue();
        if (v > 0)
            stress.accept(v);
    }

    @Deprecated
    public void learn(float pri, int vol) {

        learnPri.accept(pri);
        learnVol.accept(vol);

    }

    public void confident(float deltaConf, @NotNull Compound term) {
        confident.accept(deltaConf);
    }

//    @Deprecated public void alert(float percentFocusChange) {
//        alert.accept( percentFocusChange );
//    }

    public void eror(int vol) {
        errrVol.accept(vol);
    }

//    public double happysad() {
//
//        return happy.getSum() + sad.getSum();
//    }

    public String summary() {
        //long now = nar.time();

        StringBuilder sb = new StringBuilder()
                .append(" hapy=").append(n4(happy()))
                .append(" busy=").append(n4(busyVol.getSum()))
                .append(" lern=").append(n4(learningVol()))
                .append(" errr=").append(n4(erring()))
                .append(" strs=").append(n4(stress.getSum()))
                //.append(" cpu=").append(resourceMeter.CYCLE_CPU_TIME)
                //.append(" mem=").append(resourceMeter.CYCLE_RAM_USED)
                //.append(" alrt=").append(n4(alert.getSum()))
                ;

//        counts.forEach((k,v) -> {
//            sb.append(' ').append(k).append('=').append(v.get());
//        });
        return sb.toString();


//                 + "rwrd=[" +
//                     n4( sad.beliefs().truth(now).motivation() )
//                             + "," +
//                     n4( happy.beliefs().truth(now).motivation() )
//                 + "] "


//                + "," + dRewardPos.belief(nar.time()) +
//                "," + dRewardNeg.belief(nar.time());

    }

    public void stat(SortedMap<String, Object> x) {

        monitors.forEach(m -> x.put(m.getConfig().getName(), m.getValue()));

        x.put("emotion", summary());
    }

    /**
     * sensory prefilter
     */
    @Nullable
    public ITask onInput(@NotNull ITask x) {
        if (x instanceof Task) {
            Task t = (Task) x;
            if (t.isCommand())
                return x; //ignore

            float cost = t.voluplexity();

            MetaGoal.value(MetaGoal.Perceive, t.cause(), cost, nar.causes);
        }

        return x;
    }

    public void onActivate(@NotNull Task t, float activation, Concept origin, NAR n) {
        n.emotion.taskActivations.increment();

        short[] effect = t.cause();
        int xl = effect.length;
        if (xl > 0) {
            origin.value(t, activation, n);
        }
    }

    public void onAnswer(Task question, @Nullable Task answer) {
        //transfer budget from question to answer
        //transfer more of the budget from an unoriginal question to an answer than an original question
        answer.take(question,
            answer.conf() * (1 - question.originality()), false,false);

        //reward answer for answering the question
        float str = answer.conf() * question.priSafe(0);
        MetaGoal.value(MetaGoal.Answer, answer.cause(), str, nar);
    }


//    /** float to long at the default conversion precision */
//    private static long f2l(float f) {
//        return (long)(f * 1000f); //0.001 precision
//    }
//    /** float to long at the default conversion precision */
//    private static float l2f(long l) {
//        return l / 1000f; //0.001 precision
//    }


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
