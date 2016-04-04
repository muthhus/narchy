package nars.util.data;

import com.gs.collections.api.block.function.primitive.FloatFunction;
import com.gs.collections.api.block.function.primitive.FloatToFloatFunction;
import nars.Global;
import nars.NAR;
import nars.Symbols;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

/**
 * Created by me on 2/2/16.
 */
public class Sensor implements Consumer<NAR>, DoubleSupplier {


    /**
     * resolution of the output freq value
     */
    float resolution = 0.05f;

    @NotNull
    private final Term term;
    private final FloatFunction<Term> value;
    private final FloatToFloatFunction freq;
    @NotNull
    public final NAR nar;
    private float pri;
    private final float dur;
    private float confFactor;
    private float prevF = Float.NaN;

    boolean inputIfSame;
    int maxTimeBetweenUpdates;
    int minTimeBetweenUpdates;

    char punc = '.';

    private long lastInput;

    public final static FloatToFloatFunction direct = n -> n;

    public Sensor(@NotNull NAR n, @NotNull Termed  t, FloatFunction<Term> value) {
        this(n, t, value, direct);
    }

    public Sensor(@NotNull NAR n, @NotNull String tt, FloatFunction<Term> value) {
        this(n, tt, value, direct);
    }

    public Sensor(@NotNull NAR n, @NotNull String tt, FloatFunction<Term> value, FloatToFloatFunction valueToFreq) {
        this(n, n.term(tt), value, valueToFreq);
    }

    public Sensor(@NotNull NAR n, @NotNull Termed  t, FloatFunction<Term> value, FloatToFloatFunction valueToFreq) {
        this(n, t, value, valueToFreq,

                n.getDefaultConfidence(Symbols.BELIEF),

                n.DEFAULT_JUDGMENT_PRIORITY, n.DEFAULT_JUDGMENT_DURABILITY);
    }

    public Sensor(@NotNull NAR n, @NotNull Termed t, FloatFunction<Term> value, FloatToFloatFunction valueToFreq, float conf, float pri, float dur) {
        this.nar = n;
        this.term = t.term();
        this.value = value;
        this.freq = valueToFreq;
        this.confFactor = conf;

        this.pri = pri;
        this.dur = dur;
        this.lastInput = n.time() - 1;

        this.prevF = Float.NaN;
        init();
    }

    protected void init() {
        //auto-start
        nar.onFrame(this);
    }

    @NotNull
    public Sensor pri(float defaultPri) {
        this.pri = defaultPri;
        return this;
    }

    @NotNull
    public Sensor punc(char c) {
        this.punc = c;
        return this;
    }

    public void ready() {
        this.lastInput = nar.time()-minTimeBetweenUpdates;
    }

    @Override
    public void accept(@NotNull NAR nar) {

        int timeSinceLastInput = (int)(nar.time() - lastInput);


        double nextD = value.floatValueOf(term);
        if (!Double.isFinite(nextD))
            return; //allow the value function to prevent input by returning NaN
        float next = (float) nextD;

        float fRaw = freq.valueOf(next);
        if (!Float.isFinite(fRaw))
            return; //allow the frequency function to prevent input by returning NaN


        float f = Util.round(fRaw, resolution);

        int maxT = this.maxTimeBetweenUpdates;
        boolean limitsMaxTime = maxT > 0;
        int minT = this.minTimeBetweenUpdates;
        boolean limitsMinTime =  minT > 0;

        boolean tooSoon = (limitsMinTime && (timeSinceLastInput < minT));
        boolean lateEnough = (limitsMaxTime && (timeSinceLastInput >= maxT));
        boolean different = !Util.equals(f, prevF, Global.TRUTH_EPSILON);

        if ((inputIfSame || different || lateEnough) && (!tooSoon)) {

            Task t = input(f);
            this.lastInput = t.creation();
            this.prevF = f;

        }


        //this.prevValue = next;
    }

    @NotNull
    public Sensor resolution(float r) {
        this.resolution = r;
        return this;
    }

    @NotNull
    private Task input(float v) {
        float f, c;
        if (v < 0.5f) {
            f = 0f;
            c = (0.5f - v)*(2f * confFactor);
        } else {
            f = 1f;
            c = (v - 0.5f)*(2f * confFactor);
        }


//        float f = v;
//        float c = confFactor;

        long now = nar.time();
        Task t = new MutableTask(term(), punc)
                //.truth(v, conf)
                .truth(f, c)
                .time(now, now + dt())
                .budget(pri, dur);
        nar.input(t);
        return t;
    }

    public Termed<Compound> term() {
        return term;
    }

    /** time shift input tasks, relative to NAR's current time */
    protected int dt() {
        return 0;
    }

    @Override
    public double getAsDouble() {
        return prevF;
    }

    
    /** sets default confidence */
    @NotNull
    public Sensor conf(float conf) {
        this.confFactor = conf;
        return this;
    }

//    /** sets minimum time between updates, even if nothing changed. zero to disable this */
//    @NotNull
//    public Sensor maxTimeBetweenUpdates(int dt) {
//        this.maxTimeBetweenUpdates = dt;
//        return this;
//    }

    @NotNull
    public Sensor minTimeBetweenUpdates(int dt) {
        this.minTimeBetweenUpdates = dt;
        return this;
    }
    @NotNull
    public Sensor maxTimeBetweenUpdates(int dt) {
        this.maxTimeBetweenUpdates = dt;
        return this;
    }
    //        public void on() {
//
//        }
//        public void off() {
//
//        }
}
