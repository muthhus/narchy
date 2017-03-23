package nars.concept;

import nars.NAR;
import nars.Task;
import nars.table.EternalTable;
import nars.table.HijackTemporalBeliefTable;
import nars.table.HijackTemporalExtendedBeliefTable;
import nars.term.Compound;
import nars.truth.Truth;
import nars.truth.TruthAccumulator;
import nars.util.signal.SignalTask;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;


public abstract class ActionConcept extends WiredConcept implements Function<NAR,Task>, Consumer<NAR> {

    @NotNull
    @Deprecated protected final NAR nar;

    final TruthAccumulator
            beliefIntegrated = new TruthAccumulator(),
            goalIntegrated = new TruthAccumulator();

    public ActionConcept(@NotNull Compound term, @NotNull NAR n) {
        super(term, n);
        this.nar = n;
        n.onCycle(this);
    }

    @Override
    public void accept(NAR nar) {
        long now = nar.time();
        int dur = nar.dur();
        beliefIntegrated.add( belief( now, dur ));
        goalIntegrated.add( goal( now, dur ));
    }

    @Override
    public HijackTemporalBeliefTable newTemporalTable(int tCap, NAR nar) {
        //TODO only for Beliefs; Goals can remain normal
        return new MyListTemporalBeliefTable(tCap, tCap * 2, nar.random);
    }

    @Override
    public EternalTable newEternalTable(int eCap) {
        return EternalTable.EMPTY;
    }

    @Override
    public abstract Task apply(NAR nar);

    /** produces a curiosity exploratoin task */
    @Nullable public abstract Task curiosity(float conf, NAR nar);


    /** determines the feedback belief when desire or belief has changed in a MotorConcept
     *  implementations may be used to trigger procedures based on these changes.
     *  normally the result of the feedback will be equal to the input desired value
     *  although this may be reduced to indicate that the motion has hit a limit or
     *  experienced resistence
     * */
    @FunctionalInterface  public interface MotorFunction  {

        /**
         * @param desired current desire - null if no desire Truth can be determined
         * @param believed current belief - null if no belief Truth can be determined
         * @return truth of a new feedback belief, or null to disable the creation of any feedback this iteration
         */
        @Nullable Truth motor(@Nullable Truth believed, @Nullable Truth desired);

        /** all desire passes through to affect belief */
        MotorFunction Direct = (believed, desired) -> desired;

        /** absorbs all desire and doesnt affect belief */
        @Nullable MotorFunction Null = (believed, desired) -> null;
    }

    protected class MyListTemporalBeliefTable extends HijackTemporalExtendedBeliefTable {

        public MyListTemporalBeliefTable(int tCap, int historicCap, Random r) {
            super(tCap, historicCap, r);
        }

        @Override
        protected Task ressurect(Task t) {
            t.budget().setPriority(0);
            return t;
        }

        @Override
        protected boolean save(Task t) {
            if (t.isBelief())
                return t instanceof SignalTask;
            else
                return true; //accept all goals
        }

        @Override
        protected void feedback(MutableList<Task> l, @NotNull Task inserted) {
            //ignore feedback here
        }
    }
}
