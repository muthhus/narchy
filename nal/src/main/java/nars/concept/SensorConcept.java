package nars.concept;

import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.Task;
import nars.budget.policy.ConceptPolicy;
import nars.nal.UtilityFunctions;
import nars.table.BeliefTable;
import nars.table.DefaultBeliefTable;
import nars.task.Revision;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import nars.util.Util;
import nars.util.math.FloatSupplier;
import nars.util.signal.ScalarSignal;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static nars.Symbols.BELIEF;
import static nars.time.Tense.ETERNAL;


/**
 * primarily a collector for believing time-changing input signals
 */
public class SensorConcept extends WiredCompoundConcept implements FloatFunction<Term>, FloatSupplier, WiredCompoundConcept.Prioritizable, Runnable {

    @NotNull
    public final ScalarSignal sensor;
    private FloatSupplier input;
    protected float currentValue = Float.NaN;

    public static final Logger logger = LoggerFactory.getLogger(SensorConcept.class);

    //private boolean latchLastValue = true;




    public SensorConcept(@NotNull String term, @NotNull NAR n, FloatSupplier input, FloatToObjectFunction<Truth> truth) throws Narsese.NarseseException {
        this($.$(term), n, input, truth);
    }

    public SensorConcept(@NotNull Compound term, @NotNull NAR n, FloatSupplier input, FloatToObjectFunction<Truth> truth)  {
        super(term, n);

        this.sensor = new ScalarSignal(n, this, this, truth) {

            @Override
            protected void init() {
                //dont auto-start, do nothing here
            }

            @Override
            public void input(Task prevStart, @Nullable Task prevEnd, Task next) {

                SensorConcept.this.input(next);
                if (prevEnd!=null)
                    SensorConcept.this.input(prevEnd);
            }
        };

        this.input = input;

        pri(() -> nar.priorityDefault(BELIEF));


    }



    protected final void input(@NotNull Task t) {
//        if (autoupdate())
//            nar.inputLater(t);
//        else
        nar.input(t);
    }



//    /** originating from this sensor, or a future prediction */
//    @Override
//    public boolean validBelief(@NotNull Task t, @NotNull NAR nar) {
//        //return onlyDerivationsIfFuture(t, nar);
//        return true;
//    }
//    @Override
//    public boolean validGoal(@NotNull Task goal, @NotNull NAR nar) {
//        //return onlyDerivationsIfFuture(t, nar);
//        return true;
//    }


//    public static boolean onlyDerivationsIfFuture(@NotNull Task belief, @NotNull NAR nar) {
//        if (!(belief instanceof DerivedTask))
//            return true;
//
//        long bocc = belief.occurrence();
//        return (bocc!=ETERNAL && bocc > nar.time());
//    }
//


    @Override
    final protected void beliefCapacity(ConceptPolicy p, NAR mar) {
        beliefCapacity(0, beliefCapacity, 1, goalCapacity, nar);
    }

    @Override
    final protected @NotNull BeliefTable newBeliefTable() {
        return newBeliefTable(0,beliefCapacity);
    }

    @Override
    final protected @NotNull BeliefTable newGoalTable() {
        return newGoalTable(1,goalCapacity);
    }


//    /** async timing: only commits when value has changed significantly, and as often as necessary */
//    @NotNull
//    public SensorConcept async() {
//        timing(0, 0);
//        return this;
//    }

    /** commits every N cycles only */
    @NotNull
    public SensorConcept every(int minCycles) {
        timing(minCycles, minCycles);
        return this;
    }

    
    /**
     * adjust min/max temporal resolution of feedback input
     * ex:
     *          min=0, max=2 : update every 2 cycles, or immediately if changed
     *          max=2, min=0 : update no sooner than 2 cycles
     *          max=2, min=4 : update no sooner than 2 cycles, and no later than 4
     */
    @NotNull
    public SensorConcept timing(int minCycles, int maxCycles) {
        sensor.minTimeBetweenUpdates(minCycles);
        sensor.maxTimeBetweenUpdates(maxCycles);
        return this;
    }

//    public Task desire(@Nullable Truth t, float pri, float dur) {
//        if (this.desire==null || !this.desire.truth().equals(t)) {
//            if (this.desire != null) {
//                this.desire.delete(nar);
//            }
//
//            if (t!=null) {
//                this.desire = new MutableTask(term(), Symbols.GOAL, t).budget(pri, dur).log("Sensor Goal");
//                //policy(policy(), nar.time()); //trigger capacity update
//                sensor.nar.inputLater(this.desire);
//            }
//        }
//        return this.desire;
//    }


    public void setInput(FloatSupplier input) {
        this.input = input;
    }

    public final FloatSupplier getInput() {
        return input;
    }

    @Override
    public final float floatValueOf(Term anObject) {
        return this.currentValue = input.asFloat();
    }

    @NotNull
    public SensorConcept resolution(float v) {
        sensor.resolution(v);
        return this;
    }

    @Override @NotNull public final void pri(FloatSupplier v) {
        sensor.pri(v);
    }

    @Deprecated @NotNull public SensorConcept pri(float v) {
        sensor.pri(v);
        return this;
    }

    @NotNull
    public <S extends SensorConcept> S punc(char c) {
        sensor.punc(c);
        return (S)this;
    }

    @Override
    public float asFloat() {
        return currentValue;
    }

    @NotNull
    @Override
    protected BeliefTable newBeliefTable(int eCap, int tCap) {
        return new SensorBeliefTable(tCap);
    }


    public static void activeAttention(@NotNull Iterable<? extends Prioritizable> c, @NotNull MutableFloat min, @NotNull MutableFloat limit, @NotNull NAR nar) {

        activeAttention(c,
                //(cp) -> Util.lerp( limit.floatValue(), min.floatValue(), cp) //direct pri -> pri mapping
                (cp) -> Util.lerp(limit.floatValue(), min.floatValue(),
                            UtilityFunctions.sawtoothCurved(cp))
                , nar);
    }



    /** adaptively sets the priority of a group of sensors via a function  */
    public static void activeAttention(@NotNull Iterable<? extends Prioritizable> c, @NotNull FloatToFloatFunction f, @NotNull NAR nar) {
        c.forEach( s -> s.pri(() -> {
            return f.valueOf(nar.activation((Termed)s));
        } ) );
    }

    /** should only be called if autoupdate() is false */
    @Override public final void run() {
        sensor.accept(nar);
    }


    public static void flatAttention(@NotNull Iterable<? extends Prioritizable> c, @NotNull MutableFloat p) {
        c.forEach( s -> s.pri(p::floatValue) );
    }

    private final class SensorBeliefTable extends DefaultBeliefTable {


        public SensorBeliefTable(int tCap) {
            super(tCap);
        }

        @Override
        public void clear(@NotNull NAR nar) {
            //TODO this will happen even if goal.clear is called, which shouldnt
            sensor.current = null;
            currentValue = Float.NaN;
            super.clear(nar);
        }



        @Override
        public Truth truth(long when, long now) {

            long lastSensorInputTime = sensor.lastInputTime;

            Truth interpolated = super.truth(when, now);

            //in the future after the last sensor input time..

            long futureDT = when - lastSensorInputTime;
            if (when == ETERNAL || (futureDT > 0)) {

                // Default implementation:
                /** provides a prediction truth for this sensor at a (> 0) time dtFuture in the future.
                 *  this could return the current sensor value, a sensor value which has decreased confidence,
                 *  or some more advanced machine-learning predictor.
                 */
                // use last sensor value with projected, and ultimately eternalized confidence
                Truth assumed = sensor.truth();
                if (assumed!=null) {
                    assumed = Revision.project(assumed, when, now, lastSensorInputTime, false);
                    if (assumed!=null) {
                        if (interpolated != null) {
                            //GUESS based on both
                            //return Truth.maxConf(assumed, interpolated);
                            //return interpolated;
                            return Revision.revise(assumed, interpolated);
                        } else if (interpolated == null) {
                            return assumed;
                        }
                    }
                }

            }

            return interpolated;
        }

    }

}
