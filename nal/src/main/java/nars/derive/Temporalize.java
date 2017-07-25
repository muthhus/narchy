package nars.derive;

import nars.Op;
import nars.Task;
import nars.control.Derivation;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import org.intelligentjava.machinelearning.decisiontree.feature.P;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static nars.Op.CONJ;
import static nars.time.Tense.*;

/**
 * set missing temporal relations in a derivation using constraint solver
 *
 * @see http://choco-solver.readthedocs.io/en/latest/1_overview.html#directly
 */
public class Temporalize {


    abstract static class Event {
        public final Term term;

        Event(Term term) {
            this.term = term;
        }

        abstract public long startAbs();
        abstract public long endAbs();

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Event)) return false;

            Event event = (Event) o;

            return term.equals(event.term);
        }

        @Override
        public int hashCode() {
            return term.hashCode();
        }
    }

    static int dt(long a, long b) {
        if (a == ETERNAL && b == ETERNAL) {
            return DTERNAL;
        } else if (a!=ETERNAL && b!=ETERNAL) {
            return (int)(b - a); //TODO check for numeric precision loss
        } else {
            throw new UnsupportedOperationException("?"); //maybe just return DTERNAL
        }
    }

    static class AbsoluteEvent extends Event {

        public final long start, end;

        AbsoluteEvent(Term term, long start, long end) {
            super(term);

            assert((start == ETERNAL && end == ETERNAL) || (start!=ETERNAL && end!=ETERNAL)):
                "invalid semi-eternalization: " + start + " " + end;

            if (start <= end) {
                this.start = start; this.end = end;
            } else {
                this.start = end; this.end = start;
            }
        }

        @Override
        public long startAbs() {
            return start;
        }

        @Override
        public long endAbs() {
            return end;
        }

        @Override
        public String toString() {
            if (start!=ETERNAL) {
                if (start!=end)
                    return term + ("@[" + timeStr(start) + ".." + timeStr(end)) + "]";
                else
                    return term + "@" + timeStr(start);
            } else
                return term + "@ETE";
        }

    }

    public static class SolutionEvent extends AbsoluteEvent {

        SolutionEvent(Term term, long start, long end) {
            super(term, start, term.op()==CONJ ? end /* && */ : start /* ==> and <=> */);
        }
    }

    static String timeStr(long when) {
        return when != ETERNAL ? Long.toString(when) : "ETE";
    }
    static String timeStr(int when) {
        return when != DTERNAL ? (when != XTERNAL ? Integer.toString(when) : "?") : "DTE";
    }

    static class RelativeEvent extends Event {
        private final Event rel;
        private final int start, end;

        public RelativeEvent(Term term, Event relativeToItsStart, int start, int end) {
            super(term);
            this.rel = relativeToItsStart;
            this.start = start;
            this.end = end;
        }

        @Override
        public long startAbs() {
            long rs = rel.startAbs();
            if (rs == ETERNAL)
                return ETERNAL;
            return rs + this.start;
        }

        @Override
        public long endAbs() {
            long rs = rel.startAbs();
            if (rs == ETERNAL)
                return ETERNAL;
            return rs + this.end;
        }

        @Override
        public String toString() {
            if (start!=end) {
                return term + "@[" + timeStr(start) + ".." + timeStr(end) + "]->" + rel;
            } else {
                return term + "@" + timeStr(start) + "->" + rel;
            }
        }

    }

    public long start = ETERNAL, end = ETERNAL;
    Term conc;
    final Map<Term, Event> events = new HashMap();

    @Nullable
    public static Event solve(@NotNull Derivation d, Term pattern) {

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


        Temporalize model = new Temporalize();

        model.know(task);
        if (belief != null)
            model.know(belief);

        return model.unknown(pattern);
    }

    public void know(Task task) {
        assert (task.end() == task.start() + task.dtRange());
        Term taskTerm = task.term();
        AbsoluteEvent root = new AbsoluteEvent(taskTerm, task.start(), task.end());
        know(root, taskTerm, 0, taskTerm.dtRange());
    }

    Temporalize knowTerm(Term term) {
        return knowTerm(term, 0);
    }

    /**
     * convenience method for testing: assumes start offset of zero, and dtRange taken from term
     */
    Temporalize knowTerm(Term term, long when) {
        know(new AbsoluteEvent(term, when, when!=ETERNAL ? when + term.dtRange() : ETERNAL), term, 0, term.dtRange());
        return this;
    }


    /**
     * recursively calculates the start and end time of all contained events within a term
     *
     * @param occ superterm occurrence, may be ETERNAL
     */
    Event know(@Nullable Event root, Term term, int start, int end) {


        //TODO support multiple but different occurrences  of the same event term within the same supercompound
        if (root.term != term) {
            Event exist = events.get(term);
            if (exist != null)
                return exist;
        }

        Event event = add(root, term, start, end);

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

                    TermContainer tt = c.subterms();
                    if (dt < 0 && o.commutative) {
                        dt = -dt;
                        tt = c.reverse();
                    }

                    int t = start;

                    int l = tt.size();

                    //System.out.println(tt + " presubs " + t + "..reverse=" + reverse);
                    for (int i = 0; (i < l); i++) {

                        Term st = tt.sub(i);
                        int sdt = st.dtRange();

                        int subStart = t;
                        int subEnd = t + sdt;
                        //System.out.println("\t" + st + " sub(" + i + ") " + subStart + ".." + subEnd);
                        know(root, st, subStart, subEnd);

                        t = subEnd; //the duration of the event

                        if (i < l-1)
                            t += dt; //the dt offset (doesnt apply to the first term which is early/left-aligned)

                    }


//                    //for conjunctions: by the end of the iteration we should be at the exact end of the interval
//                    if (o == CONJ) {
//                        int expectedEnd = end;
////                        if (t!=expectedEnd) {
////                            throw new RuntimeException(term + " with dtRange=" + term.dtRange() + " mis-aligned: " + start + "," + end + " but t=" + t);
////                        }
//                        assert (t == expectedEnd) : term + " with dtRange=" + term.dtRange() + " mis-aligned: " + start + "," + end + " but t=" + t;
//                    }

                    //for others: they are "pointers" which relate time points but do not define durations

                }

            }
        }

        return event;
    }

    Event add(@Nullable Temporalize.@Nullable Event root, Term term, int start, int end) {
        Event event;
        long occ = root.startAbs();
        events.put(term, event =
                (occ != ETERNAL ?
                        new AbsoluteEvent(term, occ + start, occ + end) :
                        new RelativeEvent(term, root, start, end)
                )
        );
        return event;
    }


    /**
     * using the lb and ub we can now create unknown variables to solve for
     */
    Event unknown(Term unknown) {

        Event known = this.events.get(unknown);
        if (known != null)
            return known;


//
//        String termID = unknown.toString();
//        IntVar s, dur;
//        IntVar[] var;
//        this.events.put(unknown, var = new IntVar[]{
//                s = intVar(termID + "@", lb, ub, true),
//                dur = intVar(termID + "~", -Integer.MIN_VALUE/2, Integer.MAX_VALUE/2, true),
//        });
//
//
//
        if (unknown instanceof Compound) {

            Compound c = (Compound) unknown;
            Op o = c.op();
            if (o.temporal) {
                int dt = c.dt();

                if (dt == XTERNAL) {

                    TermContainer tt = c.subterms();
                    assert (tt.size() == 2);

                    Term a = tt.sub(0);
                    Event ae = unknown(a);

                    Term b = tt.sub(1);
                    Event be = unknown(b);

                    if (ae != null && be != null) {
                        return new SolutionEvent(
                                o.the(dt(ae.endAbs(), be.startAbs()), new Term[]{a, b}),
                                ae.startAbs(), be.endAbs()
                        );
                    } else {
                        return null;
                    }
                }
            }
        }
        //TODO know(variable, ETERNAL);
        return null;
    }

    @Override
    public String toString() {
        return events.values().toString();
    }

    /**
     * TODO support differing copies of the same term as subterms uniquely identified by their subpaths from a root
     */
    @Nullable
    public Event solve(@NotNull Term x) {

        return unknown(x);

//
////        String beforeSolve = toString();
////        System.out.println(beforeSolve);
//
//        Solver solver = getSolver();
//        boolean solved = solver.solve();
//        if (!solved)
//            return null;
//
//        String afterSolve = toString();
//        System.out.println(afterSolve);
//
////        unknown.forEach((k, v) -> {
////            System.out.println(k + " " + Arrays.toString(v));
////        });
//
//        return $.terms.retemporalize(x, new Retemporalize() {
//            @Override
//            public int dt(@NotNull Compound x) {
//
//                IntVar[] u = events.get(x);
//                if (u != null) {
//                    if (u[0].isInstantiated() && u[1].isInstantiated()) {
//                        int a = u[0].getValue();
//                        int b = a + u[1].getValue();
//                        return b - a;
//                    }
//                }
//                return XTERNAL; //unknown
//            }
//        });
    }
}
