package nars.derive.time;

import jcog.Util;
import jcog.math.Interval;
import jcog.random.XorShift128PlusRandom;
import nars.$;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.control.Derivation;
import nars.task.TruthPolation;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.container.TermContainer;
import nars.term.subst.Subst;
import nars.time.Tense;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static nars.Op.*;
import static nars.time.Tense.*;

/**
 * set missing temporal relations in a derivation using constraint solver
 *
 * @see http://choco-solver.readthedocs.io/en/latest/1_overview.html#directly
 */
public class Temporalize implements ITemporalize {

    private final static Logger logger = LoggerFactory.getLogger(Temporalize.class);

    private static final Time AMBIENT_EVENT = Time.the(ETERNAL, DTERNAL);

    /**
     * left-aligned earliest event which other terms can relative to
     */
    private static final Time EARLIEST_EVENT = Time.the(ETERNAL, 0);


    /**
     * constraint graph
     */
    public final Map<Term, SortedSet<Event>> constraints = new HashMap<>();
    private final Random random;
    public int dur = 1;

    /**
     * for testing
     */
    public Temporalize() {
        this(new XorShift128PlusRandom(1));
    }

    public Temporalize(Random random) {
        this.random = random;
    }

    @Override
    @Nullable
    public Term solve(@NotNull Derivation d, Term pattern, long[] occ, float[] eviGain) {

        Task task = d.task;
        Task belief = !d.single ? d.belief : null;
        dur = Math.max(1, Math.round(Param.DITHER_DT * d.dur));

        ITemporalize t = this;


        t.knowDerivedTerm(d, task.term(), task.start(), task.end());

        if (belief != null) {
            if (!belief.equals(task)) {

                t.knowDerivedTerm(d, d.beliefTerm, belief.start(), belief.end()); //!taskRooted || !belief.isEternal()); // || (bo != IMPL));
            }
        } else /*if (d.beliefTerm != null)*/ {
            if (!task.term().equals(d.beliefTerm)) { //dont re-know the term

                Term b = d.beliefTerm;
                ((Temporalize) t).knowAmbient(b);

                Term b2 = d.transform(b);
                if (!b2.equals(b) && !(b2 instanceof Bool))
                    knowAmbient(b2);
            }
            //t.know(d.beliefTerm, d, null);
        }

        Map<Term, Time> trail = new HashMap<>();
        Event e;
        try {
            e = t.solve(pattern, trail);
        } catch (StackOverflowError ignored) {
            System.err.println(
                    Arrays.toString(new Object[]{"temporalize stack overflow:\n{} {}\n\t{}\n\t{}", pattern, d, t, trail})
                    //logger.error(
            );
//            trail.clear();
//            model.solve(pattern, trail);
            return null;
        }
        if (e == null || !e.term.op().conceptualizable)
            return null;

        if (e instanceof AbsoluteEvent) {
            AbsoluteEvent a = (AbsoluteEvent) e; //faster, preferred since pre-calculated
            occ[0] = a.start;
            occ[1] = a.end;
        } else {
            occ[0] = e.start(trail).abs();
            occ[1] = e.end(trail).abs();
        }

        boolean te = task.isEternal();
        if (occ[0] == ETERNAL && (!te || (belief != null && !belief.isEternal()))) {
            //"eternal derived from non-eternal premise:\n" + task + ' ' + belief + " -> " + occ[0];
            //uneternalize/retemporalize:

//            if (/*(e.term.op() != IMPL) && */
//                    (task.op() == IMPL) && (belief == null || d.beliefTerm.op() == IMPL)) {
//                //dont retemporalize a non-implication derived from two implications
//                //it means that the timing is unknown
//                return null;
//            }

            long ts = task.start();
            long k;
            if (!te && (belief != null && !belief.isEternal())) {
                //interpolate
                ts = task.nearestTimeBetween(belief.start(), belief.end());
                long bs = belief.nearestTimeBetween(ts, task.end());
                if (ts != bs) {
                    //confidence decay in proportion to lack of coherence
                    if (task.isBeliefOrGoal()) {
                        float taskEvi = task.conf();
                        float beliefEvi = belief.conf();
                        float taskToBeliefEvi = taskEvi / (taskEvi + beliefEvi);
                        k = Util.lerp(taskToBeliefEvi, bs, ts); //TODO any duration?
                        long distSum =
                                Math.abs(task.nearestTimeTo(k) - k) +
                                        Math.abs(belief.nearestTimeTo(k) - k);
                        if (distSum > 0) {
                            eviGain[0] *= TruthPolation.evidenceDecay(1, d.dur, distSum);
                        }
                    } else {
                        k = bs;
                    }
                } else {
                    k = ts;
                }
            } else if (te) {
                k = belief.start(); //TODO any duration?
            } else /*if (be)*/ {
                k = ts; //TODO any duration?
            }
            occ[0] = occ[1] = k;
        }


        return e.term;
    }


//    /**
//     * heuristic for ranking temporalization strategies
//     */
//    @Override
//    public float score(Term x) {
//        SortedSet<Event> cc = constraints.get(x);
//        if (cc == null) {
//            return Float.NEGATIVE_INFINITY;
//        } else {
//            float s = 0;
//            for (Event e : cc) {
//                float t;
//                if (e instanceof AbsoluteEvent) {
//                    if (((AbsoluteEvent) e).start != ETERNAL)
//                        t = 2; // * (1 + x.size()); //prefer non-eternal as it is more specific
//                    else
//                        t = 0;
//                } else if (e instanceof TimeEvent) {
//                    //if (((TimeEvent)e).
//                    t = 2;
//                } else {
//                    RelativeEvent re = (RelativeEvent) e;
//                    Term tr = re.rel;
//
//                    //simultaneous NEG relations are not that valuable. usually the pos/neg occurring in a term represent different events, so this relationship is weak
//                    if (re.start == 0 &&
//                            ((tr.op() == NEG && re.term.equals(tr.unneg()))
//                                    ||
//                                    (re.term.op() == NEG && tr.equals(re.term.unneg()))
//                            )) {
//                        t = 0.1f;
//                    } else {
//                        t = 1;
//                    }
//                    //  (1f / (1 + tr.size()));  //decrease according to the related term's size
//
//                }
//                s = Math.max(s, t);
//            }
//            return s;
//        }
//    }

    public AbsoluteEvent absolute(Term x, long start, long end) {
        return new AbsoluteEvent(this, x, start, end);
    }


    private static int dt(Time a /* from */, Time b /* to */) {

        assert (a.base != XTERNAL);
        assert (b.base != XTERNAL);

        if (a.base != ETERNAL && b.base != ETERNAL) {
            return (int) (b.abs() - a.abs()); //TODO check for numeric precision loss
        } else if (a.offset != XTERNAL && b.offset != XTERNAL && a.offset != DTERNAL && b.offset != DTERNAL) {
            //if (a.base == ETERNAL || b.base == ETERNAL) {
            return b.offset - a.offset; //relative offsets within a complete or partial eternal context
//            } else {
//                if (a.offset == b.offset)
//                    return a.offset;
//
//
//            }
        }
        if (a.offset == DTERNAL ^ b.offset == DTERNAL) {
            //one is unknown so it is effectively a point interval around the known one
            return 0;
        } else {
            return DTERNAL;
        }
        //throw new UnsupportedOperationException(a + " .. " + b); //maybe just return DTERNAL
    }


    private Event solution(Term term, Time start) {
        return new TimeEvent(this, term, start);
//        long st = start.abs();
//        long et;
//        if (st == ETERNAL) et = ETERNAL;
//        else et = term.op() == CONJ ? st + term.dtRange() : st;
//
//        return new SolutionEvent(this, term, st, et);
    }

//    static String timeStr(int when) {
//        return when != DTERNAL ? (when != XTERNAL ? Integer.toString(when) : "?") : "DTE";
//    }

    public RelativeEvent relative(Term term, Term relativeTo, int start) {
        int end = (start == DTERNAL ? DTERNAL : start + term.dtRange());
        return relative(term, relativeTo, start, end);
    }

    private Event relative(Term term, Event inherited, int offset) {
        if (inherited.term.equals(term))
            return inherited;
        else
            return relative(term, inherited.term, offset);
    }

    private RelativeEvent relative(Term term, Term relativeTo, int start, int end) {
        assert (!term.equals(relativeTo));
        assert ((start == DTERNAL) == (end == DTERNAL));
        return new RelativeEvent(this, term, relativeTo, start, end);
    }


    protected void print() {

        constraints.forEach((k, v) -> {
            for (Event vv : v)
                System.out.println(k + " " + vv);
        });

        System.out.println();
    }


    @Override
    public void knowDerivedTerm(Subst d, Term term, long start, long end) {
        knowTerm(term, start, end);

//        Term t2 = d.transform(term);
//        if (!t2.equals(term) && !(t2 instanceof Bool)) {
//            knowTerm(t2, start, end);
//        }
    }


    /**
     * convenience method for testing: assumes start offset of zero, and dtRange taken from term
     */
    public void knowTerm(Term term, long when) {
        knowTerm(term, when, when != ETERNAL ? when + term.dtRange() : ETERNAL);
    }

    /**
     * convenience method for testing: assumes start offset of zero, and dtRange taken from term
     */
    public void knowTerm(Term term, long from, long to) {
        AbsoluteEvent basis = absolute(term, from, to);
        if (from!=ETERNAL) {
            know(term, basis,
                    0, (int) (to - from)
            );
        } else {
            know(term, basis, 0, 0);
        }
    }


    /**
     * recursively calculates the start and end time of all contained events within a term
     *
     * @param occ    superterm occurrence, may be ETERNAL
     * @param start, end - term-local temporal bounds
     */
    private void know(Term x, @Nullable AbsoluteEvent root, int start, int end) {

//        if (!x.op().conceptualizable) // || (!term.hasAny(ATOM.bit | INT.bit)))
//            return; //ignore variable's and completely-variablized's temporalities because it can conflict

        //TODO support multiple but different occurrences  of the same event term within the same supercompound
        if (root == null || root.term != x) {
            SortedSet<Event> exist = constraints.get(x);
            if (exist != null)
                return;
        }

        if (root != null) {
            Event event;

            if (x.equals(root.term)) {
                event = root;
            } else {
                Time occ = root.start(null);
                assert (occ.base != XTERNAL);
                event = (occ.base != ETERNAL ?
                        absolute(x, occ.abs() + start, occ.abs() + end) :
                        relative(x, root.term, start, end)
                );
            }

            know(x, event);
        } else {
            know(x, null);
        }


        Op o = x.op();
        if (o == IMPL) { //CONJ already handled in a call from the above know
            int dt = x.dt();

            if (dt == XTERNAL) {

                //TODO UNKNOWN TO SOLVE FOR
                //throw new RuntimeException("no unknowns may be added during this phase");

            } else if (dt == DTERNAL) {
                //do not infer any specific temporal relation between the subterms
                //just inherit from the parent directly
                x.subterms().forEach(st -> this.know(st, relative(st, root, DTERNAL)));

            } else {
                TermContainer tt = x.subterms();
//                boolean reverse;
//                    reverse = false;
//                } else if (dt >= 0) {
//                    reverse = false;
//                } else {
//                    reverse = true;
//                    if (o == CONJ) {
//                        tt = tt.reverse();
//                        dt = -dt;
//                    }
//                }


                int l = tt.size();


                int t = start;

                int lastStart = DTERNAL, lastEnd = DTERNAL;

                //System.out.println(tt + " presubs " + t + "..reverse=" + reverse);
                for (int i = 0; (i < l); i++) {

                    Term st = tt.sub(i);

                    if (i > 0)
                        t += dt; //the dt offset (doesnt apply to the first term which is early/left-aligned)

                    int stDT = st.dtRange();
                    int subStart = t, subEnd = t + stDT;


                    t = subEnd;


                    if (i > 0) {
                        //IMPL: crosslink adjacent subterms.  conjunction is already temporalized in another method
                        int relInner = lastStart - subStart;
                        Term rt = tt.sub(i - 1);
                        int rtDT = rt.dt();
                        if (rt.op() == CONJ && rtDT != DTERNAL) {
                            //link to the subj term's starting event
                            Term rtEarly = rt.sub(rtDT >= 0 ? 0 : 1);
                            if (!st.equals(rtEarly)) { //HACK when multiple times can be tracked, this wont apply
                                int relOuter = lastStart - subStart;
                                know(rtEarly, relative(rtEarly, st, relOuter));
                                know(st, relative(st, rtEarly, -relOuter));
                            }
                        }

                        if (!rt.equals(st)) {
                            know(rt, relative(rt, st, relInner));
                            know(st, relative(st, rt, -relInner));
                        }


                    }

                    lastStart = subStart;
                    lastEnd = subEnd;


                }


            }

        }
    }


    private void know(Term term, @Nullable Event superterm) {

        if (superterm != null) {
            SortedSet<Event> l = constraints.computeIfAbsent(term, (t) -> new TreeSet<>());
            l.add(superterm);
        }

        switch (term.op()) {
            case NEG:
                Term u = term.unneg();


                know(u, relative(u, superterm, 0));

                break;
            case CONJ:
                int tdt = term.dt();
                if (tdt != XTERNAL) {
                    //add the known timing of the conj's events
                    TermContainer ss = term.subterms();
                    int sss = ss.size();

                    for (int i = 0; i < sss; i++) {
                        Term a = ss.sub(i);
                        int aStart = term.subtermTime(a);
                        //relative to the root of the compound
                        int aDT = a.dtRange();

                        if (tdt != DTERNAL) {
                            if (superterm instanceof AbsoluteEvent) {
                                //add direct link to this event by calculating its relative offset to the absolute super event
                                AbsoluteEvent ase = (AbsoluteEvent) superterm;
                                long s, e;
                                if (ase.start == ETERNAL) {
                                    s = e = ETERNAL;
                                } else {
                                    if (tdt >= 0) {
                                        s = ase.start + aStart;
                                    } else {
                                        s = ase.end + tdt + aStart;
                                    }
                                    e = s + aDT;
                                }

                                know(a, new AbsoluteEvent(this, a, s, e));
                            }

                            //know(a, relative(a, term, aStart, aStart + aDT));

                            if (i < sss - 1) {
                                //relative to sibling subterm
                                Term b = ss.sub(i + 1);
                                if (!a.equals(b) /*&& !a.containsRecursively(b) && !b.containsRecursively(a)*/) {
                                    int bt = term.subtermTime(b);
                                    int ba = bt - aStart;
                                    know(b, relative(b, a, ba, ba + b.dtRange()));
                                    know(a, relative(a, b, -ba, -ba + aDT));
                                }
                            }
                        } else {
                            know(a, superterm);
                        }
                    }
                }
                break;
        }

    }

    private boolean fullyEternal() {
        return events().noneMatch(x -> x.term.op() != IMPL && x instanceof AbsoluteEvent && ((AbsoluteEvent) x).start != ETERNAL);
    }

    private Stream<? extends Event> events() {
        return constraints.values().stream().flatMap(Collection::stream);
    }

    @Override
    public Event solve(final Term x, Map<Term, Time> trail) {
        assert (!(x instanceof Bool));
        //System.out.println("solve " + target + "\t" + trail);

        if (trail.containsKey(x)) {
            Time xs = trail.get(x);
            if (xs != null)
                return solution(x, xs);
            else
                return null; //cyclic
        }

        if (fullyEternal() && /*empty(trail) && */x.op() != IMPL && x.dt() != XTERNAL) {
            //HACK
            trail.put(x, (x.op().temporal && x.dt()==DTERNAL) ? AMBIENT_EVENT : EARLIEST_EVENT); //glue
            //knowTerm(x, ETERNAL); //everything will be relative to this, in eternity
        } else {
            trail.putIfAbsent(x, null); //placeholder
        }


        SortedSet<Event> cc = constraints.get(x);
        if (cc != null) {
            for (Event e : cc) {

                //System.out.println(x + " " + i + "\t" + trail + "\t" + e);

                Time xt = e.start(trail);
                if (xt != null) {
                    trail.put(x, xt);
                    return e;
                }
            }

        }

        if (x.size() > 0) {
            Event e = solveComponents(x, trail);

            if (e != null) {
                Time xs = e.start(trail);
                if (xs != null) {
                    trail.put(x, xs); //assign
                    return e;
                }
            }
        }

        trail.remove(x, null); //remove null placeholder

//        //update constraints, in case they were changed above
//        cc = constraints(x);
//        if (cc!=null)
//            return cc.get(0);
//        else
        return null;
    }

//    static boolean empty(Map<Term, Time> trail) {
//        return trail.isEmpty() || trail.values().stream().noneMatch(Objects::nonNull);
//    }

    private Event solveComponents(Term x, Map<Term, Time> trail) {

        Op o = x.op();

        if (o == NEG) {
            Event ss = solve(x.unneg(), trail);
            if (ss != null)
                return ss.neg();
            else
                return null;
        } else if (o.temporal) {
            if (x.dt() != XTERNAL) {
                //TODO verify that the provided subterm timing is correct.
                // if so, return the input as-is
                // if not, return null
                if (x.op() == CONJ && x.size() == 2) {
                    Term a = x.sub(0);
                    Event ae = solve(a, trail);
                    Term b = x.sub(1);
                    Event be = solve(b, trail);
                    if ((ae != null) && (be != null)) {
                        Time at = ae.start(trail);
                        if (at != null) {
                            Time bt = be.start(trail);
                            if (bt != null) {
                                return solveConj(a, at, b, bt);
                            }
                        }
                    }
                }
            } else /*if (target.dt() == XTERNAL)*/ {
                TermContainer tt = x.subterms();

                int tts = tt.size();
                assert (tts > 1);


                boolean has0 = false;
                boolean hasEte = false;
                for (Term y : constraints.keySet()) {
                    if (y.op() == x.op()) {
                        switch (y.dt()) {
                            case 0: has0 = true; break;
                            case DTERNAL: hasEte = true; break;
                        }
                    }
                }
                {
                    /* test for the exact appearance of temporalized form present in constraints */
                    if (hasEte) { //quick test filter
                        @NotNull Term xEte = x.dt(DTERNAL);
                        if (!(xEte instanceof Bool) && constraints.get(xEte) != null) {
                            Event ds = solve(xEte, trail);
                            if (ds != null) {
                                return relative(xEte, ds, DTERNAL);
                            }
                        }
                    }
                }

                {
                    /* test for the exact appearance of temporalized form present in constraints */
                    if (has0) { //quick test filter
                        @NotNull Term xPar = x.dt(0);
                        if (!(xPar instanceof Bool) && constraints.get(xPar) != null) {
                            Event ds = solve(xPar, trail);
                            if (ds != null)
                                return relative(xPar, ds, 0);
                        }
                    }
                }

                //HACK try this trick: fully anonymous match
                Term xRoot = x.conceptual();
                if (x.equals(xRoot)) {
                    for (Term y : constraints.keySet()) {
                        if (y.root().equals(xRoot)) {
                            return solve(y, trail);
                        }
                    }
                }

//                {
//                    /* failed to find the sequential case: >=2 or more, so dt=DTERNAL or dt=0 */
//
//
//                    //test if any of the terms have been linked to ambient as this implies it must be DTERNAL HACK
//                    //or if one of them has a specific time, then siblings must share it
//                    TermContainer cj = x.subterms();
//                    int cjs = cj.size();
//                    for (int i = 0; i < cjs; i++) {
//                        Term xc = cj.sub(i);
//                        SortedSet<Event> c = constraints.get(xc);
//                        if (c != null) {
//                            Event e = c.first();
//                            if (e instanceof RelativeEvent) {
//                                Term y = ((RelativeEvent) e).rel;
//                                int ydt = y.dt();
//                                if (y.op() == CONJ && ydt != XTERNAL && y.contains(xc)) {
//                                    Term cxEte = x.dt(y.dt());
//                                    Event es = solve(cxEte, trail);
//                                    if (es != null)
//                                        return relative(cxEte, es.term, DTERNAL, DTERNAL);
//                                    //System.out.println(y + " " + e.getClass() + " " + e);
//                                }
//                            }
//                        }
//                    }
//                }


                if (tts == 2) {


//                    boolean dir = true; //forward
                    Term t0 = tt.sub(0);
                    Term t1 = tt.sub(1);

//                    //decide subterm solution order intelligently: allow reverse if the 2nd subterm can more readily and absolutely temporalize
//                    if (score(t1) > score(t0)  /* || t1.volume() > t0.volume()*/) {
//                        dir = false; //reverse: solve simpler subterm first
//                    }

                    Event ea;
//                    if (dir) {
//                        //forward
                    if ((ea = solve(t0, trail)) != null) {

                        Event eb;
                        if ((eb = solve(t1, trail)) != null) {

//                    } else {
//                        //reverse
//                        if ((eb = solve(t1, trail)) == null)
//                            return null;
//                        if ((ea = solve(t0, trail)) == null)
//                            return null;
//                    }


                            Time at = ea.start(trail);

                            if (at != null) {

                                Time bt = eb.start(trail);

                                if (bt != null) {

                                    Term a = ea.term;
                                    Term b = eb.term;


                                    if (o == CONJ /*&& (a.op() == CONJ || b.op() == CONJ)*/) {
                                        //conjunction merge, since the results could overlap
                                        //either a or b, or both are conjunctions. and the result will be conjunction

                                        Event e = solveConj(a, at, b, bt);
                                        if (e != null)
                                            return e;

                                    } else {

                                        Event e = solveTemporal(o, a, at, b, bt);
                                        if (e != null)
                                            return e;

                                    }
                                }
                            }
                        }
                    }
                }


            }
        }


        /**
         * for novel compounds, (ex. which might be created by syllogistic rules
         * we wont be able to find them in the constraints.
         * this heuristic computes the temporal intersection of all involved subterms.
         * if they are coherent, then create the solved term as-is (since it will not contain any XTERNAL) with the appropriate
         * temporal bounds. */
        if (o.statement && constraints.get(x) == null) {
            //choose two absolute events which cover both 'a' and 'b' terms
            List<Event> relevant = $.newArrayList(); //maybe should be Set?
            Set<Term> uncovered = new HashSet();
            x.subterms().recurseTermsToSet(
                    ~(Op.SECTi.bit | Op.SECTe.bit | Op.DIFFe.bit | Op.DIFFi.bit) /* everything but sect/diff; just their content */,
                    uncovered, true);

            for (Term c : constraints.keySet()) {

                if (c.equals(x)) continue; //cyclic; already tried above

                Event ce = solve(c, trail);

                if (ce != null) {
                    if (uncovered.removeIf(c::containsRecursively)) {
                        relevant.add(ce);
                        if (uncovered.isEmpty())
                            break; //got them all
                    }
                }

            }
            if (!uncovered.isEmpty())
                return null; //insufficient information

            //HACK just use the only two for now, it is likely what is relevant anyway
            int rr = relevant.size();
            switch (rr) {
                case 0:
                    return null; //can this happen?
                case 1:
                    Event r = relevant.get(0);
                    return relative(x, r.term, 0, x.dtRange());

            }

            int retries = rr > 2 ? 2 : 1;

            for (int i = 0; i < retries; i++) {

                if (rr > 2)
                    Collections.shuffle(relevant, random); //dont reshuffle if only 2, it's pointless; intersection is symmetric

                Event ra = relevant.get(0);
                Event rb = relevant.get(1);

                Event ii = solveStatement(x, trail, ra, rb);
                if (ii != null)
                    return ii;
            }
        }

        return null;
    }


    private Event solveConj(Term a, Time at, Term b, Time bt) {
        long ata = at.abs();
        long bta = bt.abs();

        if (ata == ETERNAL || /* && */ bta == ETERNAL) {
            if (at.offset == DTERNAL || bt.offset == DTERNAL) {
                //assert(at.offset==bt.offset);
                Term ce = CONJ.the(DTERNAL, a, b);
                if (ce instanceof Bool)
                    return null;
                return new AbsoluteEvent(this, ce, ETERNAL);
            }
            ata = at.offset; //TODO maybe apply shift, and also needs to affect 'start'
            bta = bt.offset;// + a.dtRange();
        } /*else if (ata == ETERNAL ^ bta == ETERNAL) {
            return null; //one is eternal the other isn't
        }*/


        Term newTerm = Op.conjMerge(a, ata, b, bta);
        if (newTerm instanceof Bool)
            return null;
//        if (!newTerm.op().conceptualizable) //failed to create conj
//            return null;

        Time early = ata < bta ? at : bt;
        return new TimeEvent(this, newTerm, early);
    }

    private Event solveStatement(Term target, Map<Term, Time> trail, Event ra, Event rb) {

        //not overlapping at all, compute point interpolation
        Time as = ra.start(trail);
        if (as != null) {
            Time bs = rb.start(trail);
            if (bs != null) {
                Time at = ra.end(trail);
                if (at != null) {
                    Time bt = rb.end(trail);
                    if (bt != null) {
                        long ta = as.abs();
                        long tz = at.abs();
                        if (tz == ETERNAL) tz = ta;
                        long ba = bs.abs();
                        long bz = bt.abs();
                        if (bz == ETERNAL) bz = ba;

                        if (ta == ETERNAL || bz == ETERNAL) {
                            return new SolutionEvent(this, target, ETERNAL);
                        } else {
                            Interval ii = Interval.intersect(ta, tz, ba, bz);
                            if (ii != null) {
                                //overlap or adjacent
                                return new SolutionEvent(this, target, ii.a, ii.b);
                            } else {
                                //interpolate
                                long dist = Interval.unionLength(ta, tz, ba, bz) - (tz - ta) - (bz - ba);
                                if (Param.TEMPORAL_TOLERANCE_FOR_NON_ADJACENT_EVENT_DERIVATIONS * dur >= ((float) dist)) {
                                    long occ = ((ta + tz) / 2L + (ba + bz) / 2L) / 2L;
                                    //occInterpolate(t, b);
                                    return new SolutionEvent(this, target, occ);
                                }
                            }
                        }


                    }
                }
            }
        }


        return null;
    }

    private Event solveTemporal(Op o, Term a, Time at, Term b, Time bt) {

        assert (o != CONJ && o.temporal);

        int dt = dt(at, bt);
        if (dt != XTERNAL) {

            if (dt != DTERNAL) {
                int innerRange = a.dtRange(); //only A, not B (because the end of A points to the start of B)
                //            if (dt > 0) {
                dt -= innerRange;
                //            } else if (dt < 0) {
                //                dt += innerRange;
                //            }

                if (dt != 0 && Math.abs(dt) < dur)
                    dt = 0; //perceived as simultaneous within duration
            }


            Term newTerm = o.the(dt, a, b);
            if (newTerm instanceof Bool)
                return null;

            return new TimeEvent(this, newTerm, at);
        }
        return null;

    }


    @Override
    public String toString() {
        return constraints.values().stream().flatMap(Collection::stream).map(Object::toString).collect(Collectors.joining(","));
    }


    public void knowAmbient(@NotNull Term t) {
        know(t, new AmbientEvent(t), 0, t.dtRange());
    }

    private final class AmbientEvent extends AbsoluteEvent {
        public AmbientEvent(@NotNull Term t) {
            super(Temporalize.this, t, Tense.ETERNAL, Tense.ETERNAL);
        }

        @NotNull
        @Override
        public Time start(@Nullable Map<Term, Time> ignored) {
            return AMBIENT_EVENT;
        }

        @NotNull
        @Override
        public Time end(Map<Term, Time> ignored) {
            return AMBIENT_EVENT;
        }
    }
}
