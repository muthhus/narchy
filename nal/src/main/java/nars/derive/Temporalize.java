package nars.derive;

import com.google.common.base.Joiner;
import nars.Op;
import nars.Task;
import nars.control.Derivation;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static nars.Op.CONJ;
import static nars.Op.Null;
import static nars.time.Tense.*;
import static org.junit.Assert.assertTrue;

/**
 * set missing temporal relations in a derivation using constraint solver
 *
 * @see http://choco-solver.readthedocs.io/en/latest/1_overview.html#directly
 */
public class Temporalize extends Model {

    /**
     * lower and upper bounds of the potential solution universe
     * this will be stretched by the union of the start/end times of the present task(s)
     * and also by temporal relations present within them
     */
    public int lb = Integer.MAX_VALUE, ub = Integer.MIN_VALUE;

    public long start = ETERNAL, end = ETERNAL;
    public Term conc;
    final Map<Term, IntVar[]> known = new HashMap(), unknown = new HashMap();

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
        known(task.term(), (int) (task.start() - root), (int) (task.end() - root));
    }

    /**
     * convenience method for testing: assumes start offset of zero, and dtRange taken from term
     */
    Temporalize eventize(Term term) {
        known(term, 0, term.dtRange());
        return this;
    }


    /**
     * recursively calculates the start and end time of all contained events within a term
     */
    void known(Term term, int start, int end) {

        //this represents a constant known range so this may not actually correspond to a variable
        //instead it may need to be represented as 1 or 2 constant values depending if it's point-like or an interval
        //IntVar i = intVar(term.toString(), start, end, true /* bounded domain */);
        IntVar s, e;
        String termID = term.toString();
        known.put(term, new IntVar[]{
                s = intVar(termID + '>', start),
                e = intVar('<' + termID, end)
        });


        if (lb > start)
            lb = start;
        if (ub < end)
            ub = end;

        if (term instanceof Compound) {
            Compound c = (Compound) term;
            Op o = c.op();
            if (o.temporal) {
                int dt = c.dt();

                if (dt == XTERNAL) {

                    //TODO UNKNOWN TO SOLVE FOR
                    throw new RuntimeException("no unknowns may be added during this phase");

                } else {
                    if (dt == DTERNAL)
                        dt = 0;

                    boolean reverse;
                    int t;
                    if (dt < 0 && o.commutative /* conj & equi */) {
                        dt = -dt;
                        reverse = true;
                        t = end;
                    } else {
                        reverse = false;
                        t = start;
                    }

                    TermContainer tt = c.subterms();
                    int l = tt.size();

                    for (int i = 0; i < l; i++) {
                        if (i > 0)
                            t += dt; //the dt offset (doesnt apply to the first term which is early/left-aligned)

                        Term st = tt.sub(reverse ? (l - 1 - i) : i);
                        int sdt = st.dtRange();
                        known(st, t, t + sdt);

                        t += sdt; //the duration of the event
                    }

                    //for conjunctions: by the end of the iteration we should be at the exact end of the interval
                    if (o == CONJ) {
                        assert (t == end) : "mis-aligned: " + start + "," + end + " but t=" + t;
                    }

                    //for others: they are "pointers" which relate time points but do not define durations

                }

            }
        }

    }

    //arithm(a, "+", b, "=", c).post();


    /**
     * using the lb and ub we can now create unknown variables to solve for
     */
    IntVar[] unknown(Term unknown) {
        assert (lb <= ub) : "solution range unknown: " + lb + ".." + ub;

        IntVar[] known = this.known.get(unknown);
        if (known != null)
            return known;

        String termID = unknown.toString();
        IntVar s, e;
        IntVar[] var;
        this.unknown.put(unknown, var = new IntVar[]{
                s = intVar(termID + ">", lb, ub, true /* bounded domain */),
                e = intVar("<" + termID, lb, ub, true /* bounded domain */),
        });


        if (unknown instanceof Compound) {


            Compound c = (Compound) unknown;
            Op o = c.op();
            if (o.temporal) {
                int dt = c.dt();

                TermContainer tt = c.subterms();
                int l = tt.size();

                if (dt == XTERNAL) {
                    assert (l == 2);

                    //scalar variable reprsenting unknown interval
                    IntVar xdt = intVar("~" + termID, lb, ub, true);

                    arithm(e, "-", s, "=", xdt).post(); //constraint on the duration

                    Term a = tt.sub(0);
                    IntVar[] av = unknown(a);

                    Term b = tt.sub(1);
                    IntVar[] bv = unknown(b);

                    arithm(av[1], "+", xdt, "=", bv[1]).post();



                } else {
                    int dtr = unknown.dtRange();
                    arithm(e, "-", s, "=", dtr).post(); //constraint on the duration

                    boolean reverse;
                    int t;
                    if (dt < 0 && o.commutative /* conj & equi */) {
                        dt = -dt;
                        reverse = true;
                        t = dtr;
                    } else {
                        reverse = false;
                        t = 0;
                    }

                    for (int i = 0; i < l; i++) {
                        if (i > 0)
                            t += dt; //the dt offset (doesnt apply to the first term which is early/left-aligned)

                        Term st = tt.sub(reverse ? (l - 1 - i) : i);
                        int sdt = st.dtRange();
                        IntVar[] subvar = unknown(st);
                        if (!reverse) {
                            arithm(subvar[0], "-", var[0], "=", t).post();
                        } else {
                            arithm(var[1], "-", subvar[1], "=", t).post();
                        }
                        t += sdt; //the duration of the event
                    }
                }
            }
        }
        return var;
    }

    @Override
    public String toString() {
        return Joiner.on(',').join(getVars()) + ":" +
                Joiner.on(',').join(getCstrs())
                ;
    }

    @Nullable public Term solve(@NotNull Term t) {
        unknown(t);

//        String beforeSolve = toString();
//        System.out.println(beforeSolve);

        Solver solver = getSolver();
        boolean solved = solver.solve();
        if (!solved)
            return null;

        assertTrue(solved);

        String afterSolve = toString();
        System.out.println(afterSolve);

        unknown.forEach((k,v) -> {
           System.out.println(k + " " + Arrays.toString(v));
        });

        //TODO attempt to construct new term with any solved numerics
        return null;
    }
}
