package nars.nal.meta;

import nars.$;
import nars.Memory;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;


public interface TruthOperator {

    Atom NONE = $.the("None");

    static void permuteTruth(@NotNull TruthOperator[] values, @NotNull Map<Term, TruthOperator> table) {
        for (TruthOperator tm : values) {
            table.put($.the(tm.toString()), tm);
            table.put($.the(tm.toString() + 'X'), TruthOperator.swapped(tm));
            table.put($.the(tm.toString() + 'N'), TruthOperator.negated(tm));
            table.put($.the(tm.toString() + "NX"), TruthOperator.negated(TruthOperator.swapped(tm)));
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
    @Nullable Truth apply(@Nullable Truth task, @Nullable Truth belief, @NotNull Memory m, float minConf);


    boolean allowOverlap();
    boolean single();


    @NotNull
    static TruthOperator swapped(TruthOperator o) {
        return new SwappedTruth(o);
    }
    @NotNull
    static TruthOperator negated(@NotNull TruthOperator o) {
        return new NegatedTruth(o);
    }

    final class SwappedTruth implements TruthOperator {

        private final TruthOperator o;

        public SwappedTruth(TruthOperator o) {
            this.o = o;
        }

        @Override
        public
        @Nullable
        Truth apply(@Nullable Truth task, @Nullable Truth belief, @NotNull Memory m, float minConf) {
            return o.apply(belief, task, m, minConf);
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

    final class NegatedTruth implements TruthOperator {

        @NotNull
        private final TruthOperator o;
        private final transient boolean overlapCached;

        public NegatedTruth(@NotNull TruthOperator o) {
            this.o = o;
            overlapCached = o.allowOverlap();
        }

        @Override
        public
        @Nullable
        Truth apply(@Nullable Truth task, @Nullable Truth belief, @NotNull Memory m, float minConf) {
            return task == null ? null : o.apply(task.negated(), belief, m, minConf);
        }

        @Override
        public boolean allowOverlap() {
            //return o.allowOverlap();
            return overlapCached;
        }

        @Override
        public boolean single() {
            return o.single();
        }
    }


    @Nullable
    static Truth identity(@Nullable Truth t, float minConf) {
        return (t == null || (t.conf() < minConf)) ? null : t;
    }
}
