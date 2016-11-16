package nars.truth.func;

import nars.$;
import nars.NAR;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;


public interface TruthOperator {

    Atomic NONE = $.the("None");

    static void permuteTruth(@NotNull TruthOperator[] values, @NotNull Map<Term, TruthOperator> table) {
        for (TruthOperator tm : values) {
            table.put($.the(tm.toString()), tm);
            table.put($.the(tm.toString() + 'X'), new SwappedTruth(tm));
            table.put($.the(tm.toString() + 'N'), new NegatedTaskTruth(tm)); //ie. NP
            table.put($.the(tm.toString() + "PN"), new NegatedBeliefTruth(tm));
            table.put($.the(tm.toString() + "NN"), new NegatedTruths(tm));
            table.put($.the(tm.toString() + "NX"), new NegatedTaskTruth(new SwappedTruth(tm)));
        }
    }

    /**
     *
     * @param task
     * @param belief
     * @param m
     * @param minConf if confidence is less than minConf, it can return null without creating the Truth instance;
     *                if confidence is equal to or greater, then it is valid
     * @return
     */
    @Nullable Truth apply(@Nullable Truth task, @Nullable Truth belief, NAR m, float minConf);


    boolean allowOverlap();
    boolean single();


    final class SwappedTruth implements TruthOperator {

        private final TruthOperator o;

        public SwappedTruth(TruthOperator o) {
            this.o = o;
        }

        @Override
        public
        @Nullable
        Truth apply(@Nullable Truth task, @Nullable Truth belief, NAR m, float minConf) {
            return o.apply(belief, task, m, minConf);
        }

        @NotNull
        @Override
        public String toString() {
            return o.toString() + 'X';
        }

        @Override
        public boolean allowOverlap() {
            return o.allowOverlap();
        }

        @Override
        public boolean single() {
            return o.single();
        }
    }

    /** ____N , although more accurately it would be called: 'NP' */
    final class NegatedTaskTruth implements TruthOperator {

        @NotNull private final TruthOperator o;

        public NegatedTaskTruth(@NotNull TruthOperator o) {
            this.o = o;
        }

        @Override @Nullable public Truth apply(@Nullable Truth task, @Nullable Truth belief, NAR m, float minConf) {
            return task == null ? null : o.apply(task.negated(), belief, m, minConf);
        }

        @NotNull @Override public final String toString() {
            return o.toString() + "N";
        }

        @Override public boolean allowOverlap() { return o.allowOverlap(); }

        @Override public boolean single() {
            return o.single();
        }
    }

    final class NegatedBeliefTruth implements TruthOperator {

        @NotNull private final TruthOperator o;

        public NegatedBeliefTruth(@NotNull TruthOperator o) {
            this.o = o;
        }

        @Override @Nullable public Truth apply(@Nullable Truth task, @Nullable Truth belief, NAR m, float minConf) {
            return o.apply(task, belief!=null ? belief.negated() : null, m, minConf);
        }

        @NotNull @Override public final String toString() {
            return o + "PN";
        }

        @Override public boolean allowOverlap() {  return o.allowOverlap();         }

        @Override public boolean single() {
            return o.single();
        }
    }

    /** negates both task and belief frequency */
    final class NegatedTruths implements TruthOperator {

        @NotNull private final TruthOperator o;

        public NegatedTruths(@NotNull TruthOperator o) {
            this.o = o;
        }

        @Override @Nullable public Truth apply(@Nullable Truth task, @Nullable Truth belief, NAR m, float minConf) {
            return task == null ? null : o.apply(task.negated(), belief!=null ? belief.negated() : null, m, minConf);
        }

        @NotNull @Override public final String toString() {
            return o + "NN";
        }

        @Override public boolean allowOverlap() {  return o.allowOverlap();         }

        @Override public boolean single() {
            return o.single();
        }
    }

    @Nullable
    static Truth identity(@Nullable Truth t, float minConf) {
        return (t == null || (t.conf() < minConf)) ? null : t;
    }
}
