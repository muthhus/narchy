package nars.concept;

import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.Task;
import nars.table.EternalTable;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.signal.ScalarSignal;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;
import java.util.function.LongSupplier;


/**
 * primarily a collector for believing time-changing input signals
 */
public class SensorConcept extends WiredConcept implements FloatFunction<Term>, FloatSupplier, Function<NAR,Task> {



    @NotNull
    public final ScalarSignal sensor;
    private FloatSupplier signal;
    protected float currentValue = Float.NaN;

    public static final Logger logger = LoggerFactory.getLogger(SensorConcept.class);

    public SensorConcept(@NotNull String term, @NotNull NAR n, FloatSupplier signal, FloatToObjectFunction<Truth> truth) throws Narsese.NarseseException {
        this($.$(term), n, signal, truth);
    }

    public SensorConcept(@NotNull Compound term, @NotNull NAR n, FloatSupplier signal, FloatToObjectFunction<Truth> truth)  {
        super(term, n);

        this.sensor = new ScalarSignal(n, term, this, truth, resolution) {
            @Override
            protected LongSupplier update(Truth currentBelief, @NotNull NAR nar) {
                return SensorConcept.this.update(currentBelief, nar);
            }
        };

        this.signal = signal;
        //this.beliefs = new SensorBeliefTable();
    }

    /** returns a new stamp for a sensor task */
    protected LongSupplier update(Truth currentBelief, @NotNull NAR nar) {
        //Truth g = goal(nar.time(), nar.dur());
        //if (g!=null) {
            //compare goal with belief state to determine if an adjustment task should be created
            //System.out.println(this + "\tbelief=" + currentBelief + " desire=" + g);
        //}
        return nar.time::nextStamp;
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


//
//    @Override
//    final protected @NotNull BeliefTable newGoalTable() {
//        return newGoalTable(1,goalCapacity);
//    }


//    /** async timing: only commits when value has changed significantly, and as often as necessary */
//    @NotNull
//    public SensorConcept async() {
//        timing(0, 0);
//        return this;
//    }


    @Override
    public EternalTable newEternalTable(int eCap) {
        return EternalTable.EMPTY;
    }

    public void setSignal(FloatSupplier signal) {
        this.signal = signal;
    }


    @Override
    public float floatValueOf(Term anObject /* ? */) {
        return this.currentValue = signal.asFloat();
    }

    @NotNull public final void pri(FloatSupplier v) {
        sensor.pri(v);
    }

    @NotNull public SensorConcept pri(float v) {
        sensor.pri(v);
        return this;
    }



    @Override
    public float asFloat() {
        return currentValue;
    }



//    public static void activeAttention(@NotNull Iterable<? extends Prioritizable> c, @NotNull MutableFloat min, @NotNull MutableFloat limit, @NotNull NAR nar) {
//
//        activeAttention(c,
//                //(cp) -> Util.lerp( limit.floatValue(), min.floatValue(), cp) //direct pri -> pri mapping
//                (cp) -> Util.lerp(limit.floatValue(), min.floatValue(),
//                            UtilityFunctions.sawtoothCurved(cp))
//                , nar);
//    }



//    /** adaptively sets the priority of a group of sensors via a function  */
//    public static void activeAttention(@NotNull Iterable<? extends Prioritizable> c, @NotNull FloatToFloatFunction f, @NotNull NAR nar) {
//        c.forEach( s -> s.pri(() -> {
//            return f.valueOf(nar.pri((Termed)s, Float.NaN));
//        } ) );
//    }

    /** should only be called if autoupdate() is false */
    @Nullable
    @Override public final Task apply(NAR nar) {
        return sensor.apply(nar);
    }

    public SensorConcept resolution(float r) {
        resolution.setValue(r);
        return this;
    }


}
