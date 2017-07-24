package nars.derive;

import com.google.common.base.Joiner;
import nars.Op;
import nars.Task;
import nars.control.Derivation;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import org.chocosolver.solver.Model;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.CONJ;
import static nars.time.Tense.*;

/**
 * set missing temporal relations in a derivation using constraint solver
 *
 * @see http://choco-solver.readthedocs.io/en/latest/1_overview.html#directly
 */
public class Temporalize extends Model {

    public long start = ETERNAL, end = ETERNAL;
    public Term conc;

    @Nullable
    public static Temporalize solve(@NotNull Derivation d, Term t) {

        /*
        unknowns to solve otherwise the result is impossible:
            - derived task start time
            - derived task end time
            - dt intervals for any XTERNAL appearing in the input term
        knowns:
            - for each task and optional belief in the derived premise:
                - start/end time of the task
                - start/end time of any contained events
            - possible relations between events referred to in the conclusion that
                appear in the premise.  this may be partial due to variable introduction
                and other reductions. an attempt can be made to back-solve the result.
                if that fails, a heuristic could decide the match. in the worst case,
                the derivation will not be temporalizable and this method returns null.
        */
        Task task = d.task;
        Task belief = d.belief;

        //these will be used to calculate the final result, if any
        boolean floating;
        long root = ETERNAL;

        Temporalize model = new Temporalize();


        //1. find a central root point about which all solutions are relative.
        // this is because choco doesnt support long's , only int's . and also it
        // works for all cases where belief and/or task are eternal.
        if (belief == null || belief.isEternal()) {
            if (!task.isEternal()) {
                root = task.mid();
                model.eventize(task, root);
                floating = false;
            } else {
                root = 0;
                floating = true;
            }
        } else {
            floating = false;
            if (!task.isEternal() && !belief.isEternal()) {
                root = (task.mid() + belief.mid()) / 2;
                model.eventize(task, root);
            } else {
                //task is eternal and belief is non-eternal
                root = belief.mid();
            }
            model.eventize(belief, root);
        }



        /*        // 1. Create a Model
        Model model = new Model("my first problem");
        // 2. Create variables
        IntVar x = model.intVar("X", 0, 5);                 // x in [0,5]
        IntVar y = model.intVar("Y", new int[]{2, 3, 8});   // y in {2, 3, 8}
        // 3. Post constraints
        model.arithm(x, "+", y, "<", 5).post(); // x + y < 5
        model.times(x,y,4).post();              // x * y = 4
        // 4. Solve the problem
        model.getSolver().solve();
        // 5. Print the solution
        System.out.println(x); // Prints X = 2
        System.out.println(y); // Prints Y = 2 */

        return null;
    }

    public void eventize(Task task, long root) {
        eventize(task.term(), (int) (task.start() - root), (int) (task.end() - root));
    }

    /** convenience method for testing: assumes start offset of zero, and dtRange taken from term */
    void eventize(Term term) {
        eventize(term, 0, term.dtRange());
    }

    /**
     * recursively calculates the start and end time of all contained events within a term
     */
    void eventize(Term term, int start, int end) {

        intVar(term.toString(), start, end, true /* bounded domain */);

        if (term instanceof Compound) {
            Compound c = (Compound) term;
            Op o = c.op();
            if (o == CONJ) {
                int dt = c.dt();

                if (dt != DTERNAL && dt != XTERNAL) {

                    boolean reverse;
                    int t;
                    if (dt < 0) {
                        dt = -dt;
                        reverse = true;
                        t = end;
                    } else {
                        reverse = false;
                        t = start;
                    }

                    TermContainer tt = c.subterms();
                    int s = tt.size();

                    for (int i = 0; i < s; i++) {
                        Term st = tt.sub(reverse ? (s - 1 - i) : i);
                        int sdt = st.dtRange();
                        eventize(st, t, t + sdt);
                        if (i < s-1)
                            t += dt + sdt;
                    }
                    assert(t == end): "by the end of the iteration we should be at the exact end of the interval " + start + "," + end + " but t=" + t;
                }

            }
        }

    }

    @Override
    public String toString() {
        return Joiner.on(',').join(getVars());
    }
}
