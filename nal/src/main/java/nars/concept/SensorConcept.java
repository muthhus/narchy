package nars.concept;

import nars.*;
import nars.budget.policy.ConceptPolicy;
import nars.nal.UtilityFunctions;
import nars.table.BeliefTable;
import nars.table.DefaultBeliefTable;
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


/**
 * primarily a collector for believing time-changing input signals
 */
public class SensorConcept extends WiredCompoundConcept implements FloatFunction<Term>, FloatSupplier, WiredCompoundConcept.Prioritizable, Runnable {

    @NotNull
    public final ScalarSignal sensor;
    private FloatSupplier input;
    private float current = Float.NaN;

    public static final Logger logger = LoggerFactory.getLogger(SensorConcept.class);




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
            public void input(Task prev, Task next) {

                SensorConcept.this.input(next);

            }
        };

        this.input = input;

        pri(() -> nar.priorityDefault(BELIEF));


    }



    protected final void input(Task t) {
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
    final protected void beliefCapacity(ConceptPolicy p, long now, List<Task> removed) {
        beliefCapacity(0, beliefCapacity, 1, goalCapacity, now, removed);
    }

    @Override
    final protected @NotNull BeliefTable newBeliefTable() {
        return newBeliefTable(0,beliefCapacity);
    }

    @Override
    final protected @NotNull BeliefTable newGoalTable() {
        return newGoalTable(1,goalCapacity);
    }


    /** async timing: only commits when value has changed significantly, and as often as necessary */
    @NotNull
    public SensorConcept async() {
        timing(0, 0);
        return this;
    }

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
        return this.current = input.asFloat();
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
        return current;
    }

    @NotNull
    @Override
    protected BeliefTable newBeliefTable(int eCap, int tCap) {
        return new SensorBeliefTable(tCap);
    }


    public static void attentionGroup(List<? extends Prioritizable> c, MutableFloat min, MutableFloat limit, NAR nar) {

        attentionGroup(c,
                //(cp) -> Util.lerp( limit.floatValue(), min.floatValue(), cp) //direct pri -> pri mapping
                (cp) -> Util.lerp(limit.floatValue(), min.floatValue(),
                            UtilityFunctions.sawtoothCurved(cp))
                , nar);
    }



    /** adaptively sets the priority of a group of sensors via a function  */
    public static void attentionGroup(List<? extends Prioritizable> c, FloatToFloatFunction conceptPriToTaskPri, NAR nar) {
        c.forEach( s -> s.pri(() -> {
            return conceptPriToTaskPri.valueOf(nar.conceptPriority((Termed)s));
        } ) );
    }

    /** should only be called if autoupdate() is false */
    @Override public final void run() {
        sensor.accept(nar);
    }

    private final class SensorBeliefTable extends DefaultBeliefTable {

        public SensorBeliefTable(int tCap) {
            super(tCap);
        }

        @Override
        public Truth truth(long when, long now) {
//            if (when == now || when == ETERNAL)
//                return sensor.truth();

            // if when is between the last input time and now, evaluate the truth at the last input time
            // to avoid any truth decay across time. this emulates a persistent latched sensor value
            // ie. if it has not changed
            if (when <= now && when >= sensor.lastInputTime) {
                //now = when = sensor.lastInputTime;
                return sensor.truth();
            }

            return super.truth(when, now);
        }
        @Override
        public Task match(@NotNull Task target, long now) {
            long when = target.occurrence();
            @Nullable Task next = sensor.next;
            if (next !=null && when <= now && when >= next.occurrence()) {
                //use the last known sensor value as-is
                return next;
            }
            return super.match(target, now);
        }

//        @Override
//        public Task match(@NotNull Task target, long now) {
//            long when = target.occurrence();
//            if (when == now || when == ETERNAL) {
//                sensor.
//                return sensor.truth();
//            }
//
//            return super.match(target, now);
//        }
    }
}
