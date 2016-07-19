package nars.util.data;

import com.gs.collections.api.block.function.primitive.FloatFunction;
import com.gs.collections.api.block.function.primitive.FloatToFloatFunction;
import com.gs.collections.api.block.function.primitive.FloatToObjectFunction;
import nars.Global;
import nars.NAR;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import nars.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

/**
 * Created by me on 2/2/16.
 */
public class Sensor implements Consumer<NAR>, DoubleSupplier {


    /**
     * resolution of the output freq value
     */
    float resolution = 0.01f;

    @NotNull
    private final Term term;
    private final FloatFunction<Term> value;
    private final FloatToObjectFunction<Truth> truthFloatFunction;

    @NotNull
    public final NAR nar;
    public float pri;
    private final float dur;

    private float prevF = Float.NaN;

    boolean inputIfSame;
    int maxTimeBetweenUpdates;
    int minTimeBetweenUpdates;

    char punc = '.';

    private long lastInput;

    public final static FloatToFloatFunction direct = n -> n;
    @Nullable
    private Task next;
    private int dt;



    public Sensor(@NotNull NAR n, @NotNull Termed t, FloatFunction<Term> value, FloatToObjectFunction<Truth> truthFloatFunction) {
        this(n, t, value, truthFloatFunction, n.DEFAULT_BELIEF_PRIORITY, n.DEFAULT_BELIEF_DURABILITY);
    }

    public Sensor(@NotNull NAR n, @NotNull Termed t, FloatFunction<Term> value, FloatToObjectFunction<Truth> truthFloatFunction, float pri, float dur) {
        this.nar = n;
        this.term = t.term();
        this.value = value;
        this.truthFloatFunction = truthFloatFunction;

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
    public Sensor pri(float p) {
        this.pri = p;
        return this;
    }

    @NotNull
    public Sensor punc(char c) {
        this.punc = c;
        return this;
    }

    public char punc() { return punc; }

    /** clears timing information so it thinks it will need to input on next attempt */
    public void ready() {
        this.lastInput = nar.time() - minTimeBetweenUpdates;
    }

    @Override
    public void accept(@NotNull NAR nar) {

        long now = nar.time();
        int timeSinceLastInput = (int) (now - lastInput);


        float next = value.floatValueOf(term);
        if (!Float.isFinite(next))
            return; //allow the value function to prevent input by returning NaN

        float f = Util.round(next, resolution);

        int maxT = this.maxTimeBetweenUpdates;
        boolean limitsMaxTime = maxT > 0;
        int minT = this.minTimeBetweenUpdates;
        boolean limitsMinTime = minT > 0;

        boolean tooSoon = (limitsMinTime && (timeSinceLastInput < minT));
        boolean lateEnough = (limitsMaxTime && (timeSinceLastInput >= maxT));
        boolean different = !Util.equals(f, prevF, Global.TRUTH_EPSILON);

        if ((inputIfSame || different || lateEnough) && (!tooSoon)) {

            commit(f);
            this.lastInput = now;
            this.prevF = f;


        }


        //this.prevValue = next;
    }

    @Nullable
    public Task next() {
        return next;
    }

    @NotNull
    public Sensor resolution(float r) {
        this.resolution = r;
        return this;
    }

    protected final void commit(float v) {
//        float f, c;
//        if (v < 0.5f) {
//            f = 0f;
//            c = (0.5f - v)*(2f * confFactor);
//        } else {
//            f = 1f;
//            c = (v - 0.5f)*(2f * confFactor);
//        }

        long now = nar.time();

        @Nullable Task t = this.next = newInputTask(v, now);
        if (t!=null)
            nar.inputTask(t);
    }



//    protected float conf(float v) {
//        return confFactor;
//    }
//    protected float freq(float v) {
//        return v;
//    }

    @NotNull
    protected Task newInputTask(float v, long now) {
        Truth t = truthFloatFunction.valueOf(v);
        if (t!=null) {
            return new MutableTask(term(), punc, t)
                    .time(now, now + dt())
                    .budget(pri(v, now, prevF, lastInput), dur)
                    .log(this);
        } else {
            return null;
        }
    }

    public float pri(float v, long now, float prevV, long lastV) {
        return pri;
    }

    @NotNull
    public Termed<Compound> term() {
        return term;
    }

    /**
     * time shift input tasks, relative to NAR's current time
     */
    protected int dt() {
        return dt;
    }

    public void dt(int dt) {
        this.dt = dt;
    }

    @Override
    public double getAsDouble() {
        return prevF;
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
