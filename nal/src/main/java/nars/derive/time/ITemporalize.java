package nars.derive.time;

import nars.Param;
import nars.Task;
import nars.control.Derivation;
import nars.term.Term;
import nars.term.subst.Subst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
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
     */
    @Nullable
    default Term solve(@NotNull Derivation d, Term pattern, long[] occ) {


        Task task = d.task;
        Task belief = d.belief;

        ITemporalize model = this;
        //model.dur = Param.DITHER_DT ? d.dur : 1;

        boolean taskRooted = true; //(belief == null) || ( !task.isEternal() );
        boolean beliefRooted = true; //belief!=null && (!taskRooted || !belief.isEternal());


        model.know(task, d, taskRooted);

        if (belief != null) {
            if (!belief.equals(task)) {

//                if (task.isEternal() && belief.isEternal() /*&& some interesction of terms is prsent */)
//                    beliefRooted = false; //avoid confusing with multiple eternal roots; force relative calculation

                model.know(belief, d, beliefRooted); //!taskRooted || !belief.isEternal()); // || (bo != IMPL));
            }
        } else if (d.beliefTerm != null) {
            if (!task.term().equals(d.beliefTerm)) //dont re-know the term
                model.know(d.beliefTerm, d, null);
        }

        Map<Term, Time> trail = new HashMap<>();
        Event e;
        try {
            e = model.solve(pattern, trail);
        } catch (StackOverflowError er) {
            System.err.println(
                    Arrays.toString(new Object[] { "temporalize stack overflow:\n{} {}\n\t{}\n\t{}", pattern, d, model, trail } )
            //logger.error(
                    );
//            trail.clear();
//            model.solve(pattern, trail);
            return null;
        }
        if (e == null)
            return null;

        if (e instanceof AbsoluteEvent) {
            AbsoluteEvent a = (AbsoluteEvent) e; //faster, preferred since pre-calculated
            occ[0] = a.start;
            occ[1] = a.end;
        } else {
            occ[0] = e.start(trail).abs();
            occ[1] = e.end(trail).abs();
        }

        if (!(
                (occ[0] != ETERNAL)
                        ||
                        (task.isEternal()) && (belief == null || belief.isEternal()))) {
            //"eternal derived from non-eternal premise:\n" + task + ' ' + belief + " -> " + occ[0];
            return null;
        }
        return e.term;
    }



}
