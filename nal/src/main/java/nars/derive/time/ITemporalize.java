package nars.derive.time;

import nars.Task;
import nars.control.Derivation;
import nars.term.Term;
import nars.term.subst.Subst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static nars.time.Tense.ETERNAL;

public interface ITemporalize {

    float score(Term y);


    /**
     * warning: for external use only; all internal calls should use solve(target, trail) to prevent stack overflow
     */
    default Event solve(Term target) {
        return solve(target, new HashMap<>(target.volume()));
    }

    Event solve(Term rel, Map<Term, Time> trail);

    void know(Task task, @NotNull Subst d, boolean taskRooted);

    void know(@NotNull Term beliefTerm, @NotNull Subst d, AbsoluteEvent root);

    static String timeStr(long when) {
        return when != ETERNAL ? Long.toString(when) : "ETE";
    }


        /**
     * unknowns to solve otherwise the result is impossible:
     * - derived task start time
     * - derived task end time
     * - dt intervals for any XTERNAL appearing in the input term
     * knowns:
     * - for each task and optional belief in the derived premise:
     * - start/end time of the task
     * - start/end time of any contained events
     * - possible relations between events referred to in the conclusion that
     * appear in the premise.  this may be partial due to variable introduction
     * and other reductions. an attempt can be made to back-solve the result.
     * if that fails, a heuristic could decide the match. in the worst case,
     * the derivation will not be temporalizable and this method returns null.
     *
     * @param eviGain length-1 float array. the value will be set to 1f by default
     *
     */
    @Nullable Term solve(@NotNull Derivation d, Term pattern, long[] occ, float[] eviGain);


}
