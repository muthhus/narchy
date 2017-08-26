package nars.derive.time;

import jcog.list.FasterList;
import jcog.math.Interval;
import jcog.random.XorShift128PlusRandom;
import nars.$;
import nars.Op;
import nars.Param;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.container.TermContainer;
import nars.term.subst.Subst;
import nars.time.Tense;
import org.eclipse.collections.api.tuple.primitive.ObjectLongPair;
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
    protected static final boolean knowTransformed = true;

    /**
     * HACK move this this to the solution instance class when it is separated from the pattern class
     */
    protected Boolean fullyEternal = null;

    /**
     * for testing
     */
    public Temporalize() {
        this(new XorShift128PlusRandom(1));
    }

    public Temporalize(Random random) {
        this.random = random;
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
        return new AbsoluteEvent(x, start, end);
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


    public void print() {

        constraints.forEach((k, v) -> {
            System.out.println(k);
            for (Event vv : v) {
                System.out.println("\t" + vv);
            }
        });

        System.out.println();
    }




    /**
     * convenience method for testing: assumes start offset of zero, and dtRange taken from term
     */
    public void knowAbsolute(Term term, long when) {
        knowAbsolute(term, when, when != ETERNAL ? when + term.dtRange() : ETERNAL);
    }

    /**
     * convenience method for testing: assumes start offset of zero, and dtRange taken from term
     */
    public void knowAbsolute(Term term, long from, long to) {
        know(absolute(term, from, to), 0, from != ETERNAL ? (int) (to - from) : 0);
    }



    /**
     * recursively calculates the start and end time of all contained events within a term
     *
     * @param occ    superterm occurrence, may be ETERNAL
     * @param start, end - term-local temporal bounds
     */
    private void know(@NotNull AbsoluteEvent root, int start, int end) {

//        if (!x.op().conceptualizable) // || (!term.hasAny(ATOM.bit | INT.bit)))
//            return; //ignore variable's and completely-variablized's temporalities because it can conflict
        Term x = root.term;


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


    }


    private void know(Term x, @Nullable Event superterm) {

        if (superterm != null) {
            SortedSet<Event> l = constraints.computeIfAbsent(x, (t) -> new TreeSet<>());
            l.add(superterm);
        }

        switch (x.op()) {
            case IMPL:
                Term impl = x;
                int implDT = x.dt();

                if (implDT == XTERNAL) {

                    //TODO UNKNOWN TO SOLVE FOR
                    //throw new RuntimeException("no unknowns may be added during this phase");
                    return;
                }

                TermContainer implComponents = x.subterms();
                Term implSubj = implComponents.sub(0);
                Term implPred = implComponents.sub(1);

                if (implDT == DTERNAL) {
                    //do not infer any specific temporal relation between the subterms
                    //just inherit from the parent directly
                    //impl.subterms().forEach(i -> know(i, relative(i, root, 0)));

                    know(implSubj, relative(implSubj, implPred, DTERNAL));
                    know(implPred, relative(implPred, implSubj, DTERNAL));

                } else {
                    if (!implSubj.equals(implPred)) {
                        int predFromSubj = implDT + implSubj.dtRange();
                        know(implPred, relative(implPred, implSubj, predFromSubj));
                        know(implSubj, relative(implSubj, implPred, -predFromSubj));

                        if (implSubj.hasAny(CONJ)) {
                            FasterList<ObjectLongPair<Term>> se = implSubj.events();
                            for (int i = 0, eventsSize = se.size(); i < eventsSize; i++) {
                                ObjectLongPair<Term> oe = se.get(i);
                                Term ss = oe.getOne();
                                if (!ss.equals(implPred)) {
                                    int t = -predFromSubj + ((int) oe.getTwo());
                                    know(ss, relative(ss, implPred, t));
                                    know(implPred, relative(implPred, ss, -t));
                                } else {
                                    //TODO repeat case
                                }
                            }
                        }
                        if (implPred.hasAny(CONJ)) {
                            FasterList<ObjectLongPair<Term>> pe = implPred.events();
                            for (int i = 0, eventsSize = pe.size(); i < eventsSize; i++) {
                                ObjectLongPair<Term> oe = pe.get(i);
                                Term pp = oe.getOne();
                                if (!pp.equals(implSubj)) {
                                    int t = predFromSubj + ((int) oe.getTwo());
                                    know(pp, relative(pp, implSubj, t));
                                    know(implSubj, relative(implSubj, pp, -t));
                                } else {
                                    //TODO repeat case
                                }
                            }
                        }
                    } else {
                        //TODO repeat case
                    }

                }


                break;


            case NEG:
                Term u = x.unneg();


                know(u, relative(u, x, 0));

                break;
            case CONJ:
                int tdt = x.dt();
                if (tdt == DTERNAL) {
                    x.subterms().forEach(sub -> {
                        know(sub, relative(sub, x, 0)); //link to super-conj
                    });
                } else if (tdt != XTERNAL) {
                    //add the known timing of the conj's events

                    FasterList<ObjectLongPair<Term>> ee = x.events();
                    int numEvents = ee.size();

                    if (numEvents <= 1) {
                        return;
                    }

                    //matrix n^2/2
                    for (int i = 0, eeSize = ee.size(); i < eeSize; i++) {
                        ObjectLongPair<Term> ii = ee.get(i);
                        Term a = ii.getOne(); //conj subevent

                        int at = (int) ii.getTwo();
                        know(a, relative(a, x, at)); //link to super-conj

                        for (int j = i + 1; j < eeSize; j++) {
                            ObjectLongPair<Term> jj = ee.get(j);
                            Term b = jj.getOne();

                            if (!ii.getOne().equals(b)) {
                                //chain to previous term
                                int bt = (int) jj.getTwo();
                                know(a, relative(a, b, at - bt));
                                know(b, relative(b, a, bt - at));
                            }
                        }
                    }

//                    TermContainer ss = term.subterms();
//                    int sss = ss.size();
//
//                    for (int i = 0; i < sss; i++) {
//                        Term a = ss.sub(i);
//                        int aStart = term.subtermTime(a);
//                        //relative to the root of the compound
//                        int aDT = a.dtRange();
//
//                        if (tdt != DTERNAL) {
//                            if (superterm instanceof AbsoluteEvent) {
//                                //add direct link to this event by calculating its relative offset to the absolute super event
//                                AbsoluteEvent ase = (AbsoluteEvent) superterm;
//                                long s, e;
//                                if (ase.start == ETERNAL) {
//                                    s = e = ETERNAL;
//                                } else {
//                                    if (tdt >= 0) {
//                                        s = ase.start + aStart;
//                                    } else {
//                                        s = ase.end + tdt + aStart;
//                                    }
//                                    e = s + aDT;
//                                }
//
//                                know(a, new AbsoluteEvent(this, a, s, e));
//                            }
//
//                            //know(a, relative(a, term, aStart, aStart + aDT));
//
//                            if (i < sss - 1) {
//                                //relative to sibling subterm
//                                Term b = ss.sub(i + 1);
//                                if (!a.equals(b) /*&& !a.containsRecursively(b) && !b.containsRecursively(a)*/) {
//                                    int bt = term.subtermTime(b);
//                                    int ba = bt - aStart;
//                                    know(b, relative(b, a, ba, ba + b.dtRange()));
//                                    know(a, relative(a, b, -ba, -ba + aDT));
//                                }
//                            }
//                        } else {
//                            know(a, superterm);
//                        }
//                    }
                }
                break;
        }

    }

    protected boolean fullyEternal() {
        if (fullyEternal == null) {
            for (SortedSet<Event> x : constraints.values()) {
                for (Event y : x) {
                    if (y instanceof AbsoluteEvent && ((AbsoluteEvent) y).start != ETERNAL && y.term.op() != IMPL) {
                        fullyEternal = false;
                        return false;
                    }
                }
            }
            fullyEternal = true;
            return true;
        } else {
            return fullyEternal;
        }

        //return events().noneMatch(x -> x instanceof AbsoluteEvent && ((AbsoluteEvent) x).start != ETERNAL && x.term.op() != IMPL);
    }

    private Stream<? extends Event> events() {
        return constraints.values().stream().flatMap(Collection::stream);
    }


//    protected List<AbsoluteEvent> overloaded(Term x) {
//        SortedSet<Event> cc = constraints.get(x);
//        if (cc==null || cc.size() == 1)
//            return null;
//
//        List<AbsoluteEvent> oo = $.newArrayList(1);
//        for (Event e : cc) {
//            if (e instanceof AbsoluteEvent) {
//                oo.add((AbsoluteEvent) e);
//            }
//        }
//        return oo.size() > 1 ? oo : null;
//    }

    @Override
    public Event solve(final Term x, Map<Term, Time> trail) {
        assert (!(x instanceof Bool));
        //System.out.println("solve " + target + "\t" + trail);


        if (trail.containsKey(x)) {
            Time xs = trail.get(x);
            if (xs != null) {
                //round-robin assignment of overloads
                //TODO generalize to RelativeEvent's
//                List<AbsoluteEvent> oo = overloaded(x);
//                if (oo != null) {
//                    int current = 0;
//                    int oos = oo.size();
//                    for (int i = 0; i < oos; i++) {
//                        if (xs == oo.get(i).startTime) {
//                            current = (i + 1)%oos;
//                            break;
//                        }
//                    }
//
//                    trail.put(x, oo.get(current).startTime);
//                    return oo.get(current);
//                } else {
                    return new TimeEvent(x, xs);
     //           }

            } else
                return null; //cyclic
        }

        if (!x.op().temporal && fullyEternal()) { //*empty(trail) && */x.op() != IMPL && x.dt() != XTERNAL) {
            //HACK anchor in eternity
            trail.putIfAbsent(x, EARLIEST_EVENT); //glue
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
            int xSize = x.size();
            if (x.dt() != XTERNAL) {
                //TODO verify that the provided subterm timing is correct.
                // if so, return the input as-is
                // if not, return null
                if (x.op() == CONJ && xSize == 2) {
                    Term a = x.sub(0);
                    Event ae = solve(a, trail);
                    Term b = x.sub(1);
                    Event be = solve(b, trail);
                    if ((ae != null) && (be != null)) {
                        Time at = ae.start(trail);
                        if (at != null) {
                            Time bt = be.start(trail);
                            if (bt != null) {
                                Event e = solveConj(a, at, b, bt);
                                if (e != null)
                                    return e;
                            }
                        }
                    }
                }
                return new AmbientEvent(x); //last resort, use the input as-is
            } else {
                TermContainer tt = x.subterms();

                int tts = tt.size();
                assert (tts > 1);


                boolean has0 = false;
                boolean hasEte = false;
                for (Term y : constraints.keySet()) {
                    if (y.op() == x.op()) {
                        switch (y.dt()) {
                            case 0:
                                has0 = true;
                                break;
                            case DTERNAL:
                                hasEte = true;
                                break;
                        }
                    }
                }

                //test for 0 first because it's more specific
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


                //HACK try this trick: fully anonymous match
                if (!constraints.isEmpty()) {
                    Term xRoot = x.root();
                    int xRootStr = xRoot.structure();
                    if (x.equals(xRoot)) {
                        //Op xRootOp = xRoot.op();
                        for (Term y : constraints.keySet()) {
                            int xRootVol = xRoot.volume();
                            if (y.hasAll(xRootStr) && y.volume() >= xRootVol && y.root().equals(xRoot)) {
                                Event e = solve(y, trail);
                                if (e != null) {
                                    return (e.term.op() == NEG ^ o == NEG) ?
                                            e.neg()  //negate, because the root term will always be unneg
                                            :
                                            e;
                                }
                            }
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


//        /**
//         * for novel compounds, (ex. which might be created by syllogistic rules
//         * we wont be able to find them in the constraints.
//         * this heuristic computes the temporal intersection of all involved subterms.
//         * if they are coherent, then create the solved term as-is (since it will not contain any XTERNAL) with the appropriate
//         * temporal bounds. */
//        if (o.statement && constraints.get(x) == null) {
//            //choose two absolute events which cover both 'a' and 'b' terms
//            List<Event> relevant = $.newArrayList(); //maybe should be Set?
//            Set<Term> uncovered = new HashSet();
//            x.subterms().recurseTermsToSet(
//                    ~(Op.SECTi.bit | Op.SECTe.bit | Op.DIFFe.bit | Op.DIFFi.bit) /* everything but sect/diff; just their content */,
//                    uncovered, true);
//
//            for (Term c : constraints.keySet()) {
//
//                if (c.equals(x)) continue; //cyclic; already tried above
//
//                Event ce = solve(c, trail);
//
//                if (ce != null) {
//                    if (uncovered.removeIf(c::containsRecursively)) {
//                        relevant.add(ce);
//                        if (uncovered.isEmpty())
//                            break; //got them all
//                    }
//                }
//
//            }
//            if (!uncovered.isEmpty())
//                return null; //insufficient information
//
//            //HACK just use the only two for now, it is likely what is relevant anyway
//            int rr = relevant.size();
//            switch (rr) {
//                case 0:
//                    return null; //can this happen?
//                case 1:
//                    Event r = relevant.get(0);
//                    return relative(x, r.term, 0, x.dtRange());
//
//            }
//
//            int retries = rr > 2 ? 2 : 1;
//
//            for (int i = 0; i < retries; i++) {
//
//                if (rr > 2)
//                    Collections.shuffle(relevant, random); //dont reshuffle if only 2, it's pointless; intersection is symmetric
//
//                Event ra = relevant.get(0);
//                Event rb = relevant.get(1);
//
//                Event ii = solveStatement(x, trail, ra, rb);
//                if (ii != null)
//                    return ii;
//            }
//        }

        return new SolutionEvent(x, ETERNAL);
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
                return new AbsoluteEvent(ce, ETERNAL);
            }
            ata = at.offset; //TODO maybe apply shift, and also needs to affect 'start'
            bta = bt.offset;// + a.dtRange();
        } /*else if (ata == ETERNAL ^ bta == ETERNAL) {
            return null; //one is eternal the other isn't
        }*/

        Time early = ata < bta ? at : bt;
        Time late = ata < bta ? bt : at;

        int cDur;
        if (Math.abs(ata - bta) < dur) {
            //dither
            cDur = (int) Math.abs(ata - bta);
            bta = ata;
            late = early;
        } else {
            cDur = -1;
        }

        Term newTerm = Op.conjMerge(a, ata, b, bta);
        if (newTerm instanceof Bool)
            return null;
//        if (!newTerm.op().conceptualizable) //failed to create conj
//            return null;


        return new TimeEvent(newTerm, early, cDur == -1 ? newTerm.dtRange() : cDur);
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
                            return new SolutionEvent(target, ETERNAL);
                        } else {
                            Interval ii = Interval.intersect(ta, tz, ba, bz);
                            if (ii != null) {
                                //overlap or adjacent
                                return new SolutionEvent(target, ii.a, ii.b);
                            } else {
//                                //interpolate
//                                long dist = Interval.unionLength(ta, tz, ba, bz) - (tz - ta) - (bz - ba);
//                                if (Param.TEMPORAL_TOLERANCE_FOR_NON_ADJACENT_EVENT_DERIVATIONS * dur >= ((float) dist)) {
//                                    long occ = ((ta + tz) / 2L + (ba + bz) / 2L) / 2L;
//                                    //occInterpolate(t, b);
//                                    return new SolutionEvent(target, occ);
//                                }
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

                dt = dither(dt);
            }


            Term newTerm = o.the(dt, a, b);
            if (newTerm instanceof Bool)
                return null;

            return new TimeEvent(newTerm, at);
        }
        return null;

    }

    private int dither(int dt) {
        if (dt != 0 && Math.abs(dt) < dur)
            return 0; //perceived as simultaneous within duration
        return dt;
    }


    @Override
    public String toString() {
        return constraints.values().stream().flatMap(Collection::stream).map(Object::toString).collect(Collectors.joining(","));
    }


    public void knowAmbient(@NotNull Term t) {
        know(new AmbientEvent(t), 0, t.dtRange());
    }

    private final static class AmbientEvent extends AbsoluteEvent {
        public AmbientEvent(@NotNull Term t) {
            super(t, Tense.ETERNAL, Tense.ETERNAL);
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
