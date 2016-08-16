package nars.op;

import com.google.common.base.Joiner;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import nars.*;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import nars.task.GeneratedTask;
import nars.term.Compound;
import nars.term.InvalidTermException;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.term.obj.Termject;
import nars.term.obj.Termject.IntInterval;
import nars.util.data.list.FasterList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

import static nars.Op.CONJ;
import static nars.Op.EQUI;
import static nars.Op.IMPL;
import static nars.nal.Tense.DTERNAL;

/** arithmetic rule mining & variable introduction */
public class ArithmeticInduction implements Consumer<Task> {

    final static String tag = ArithmeticInduction.class.getSimpleName();

    private final NAR nar;
    private final Consumer<Collection<Task>> target;


    public static Logger logger = LoggerFactory.getLogger(ArithmeticInduction.class);
    private boolean trace = false;

    public ArithmeticInduction(NAR nar) {
        this(nar, nar::inputLater);
        //nar.onTask(this);
    }

    public ArithmeticInduction(NAR nar, Consumer<Collection<Task>> target) {
        this.nar = nar;
        this.target = target;
    }

    @Nullable
    public static IntArrayList ints(List<Term> term) {
        IntArrayList l = new IntArrayList(term.size());
        for (Term x : term) {
            Integer i = intOrNull(x);
            if (i != null)
                l.add(i);
        }
        return l;
    }

    public static <X> Set<X> pluck(List<Term> term, Function<Term,X> p) {
        Set<X> s = new HashSet();
        for (Term x : term) {
            X y = p.apply(x);
            if (y!=null)
                s.add(y);
        }
        return s;
    }

    public static RangeSet<Integer> ranges(List<Term> term) {
        TreeRangeSet<Integer> r = TreeRangeSet.create();
        for (Term x : term) {
            if (x instanceof IntInterval)
                r.add(((IntInterval)x).val());
            else if (x instanceof Termject.IntTerm)
                r.add(Range.singleton(((Termject.IntTerm)x).val()).canonical(DiscreteDomain.integers()));
        }
        return r;
    }

//    @Nullable
//    public static Term intOrNullTerm(Term term) {
//        if (term instanceof Termject.IntTerm) {
//            return term;
//        }
////        if (term instanceof Atomic) {
////            int i = Texts.i(term.toString(), Integer.MIN_VALUE);
////            if (i == Integer.MIN_VALUE)
////                return null;
////            return term;
////        }
//        return null;
//    }

    @Nullable
    public static Integer intOrNull(Term term) {
        if (term instanceof Termject.IntTerm) {
            return ((Termject.IntTerm)term).val();
        }
//        if (term instanceof Atomic) {
//
//            int i = Texts.i(term.toString(), Integer.MIN_VALUE);
//            if (i == Integer.MIN_VALUE)
//                return null;
//            return i;
//        }
//        if (term instanceof Compound && term.op() == Op.SETe) {
//            Compound c = ((Compound)term);
//            if (c.size() == 1) {
//                return intOrNull(c.term(0)); //unwrap singleton extset
//            }
//        }
        return null;
    }

    @Override
    public void accept(Task in) {

        compress(in);

    }

    public Set<Task> compress(Task in) {

        if (!in.isBeliefOrGoal()) {

            return Collections.emptySet();
        } else {

            Set<Task> generated = new HashSet();

            int bdt = in.dt();
            Op o = in.op();

            //attempt to compress all subterms to a single rule
            if ((/*(o == EQUI || o == IMPL || */(o == CONJ) && ((bdt == DTERNAL) || (bdt == 0)))
                    ||
                    (o.isSet())
                    ||
                    (o.isIntersect())) {

                compress(in, (pattern) -> {

                    task(in, pattern, generated);

                });
            }


            //attempt to replace all subterms of an embedded conjunction subterm
            Compound<?> tn = in.term();
            if ((o != CONJ && o!=EQUI && o!=IMPL) && tn.subterms().hasAny(CONJ)) {


                //attempt to transform inner conjunctions
                Map<ByteList, Compound> inners = $.newHashMap();

                //Map<Compound, List<ByteList>> revs = $.newHashMap(); //reverse mapping to detect duplicates

                tn.pathsTo(x -> x.op()==CONJ &&
                        ((((Compound)x).dt() == 0) || ((Compound)x).dt() == DTERNAL)  ? x : null, (p, v) -> {
                    if (!p.isEmpty())
                        inners.put(p.toImmutable(), (Compound)v);
                    return true;
                });

                //TODO see if duplicates exist and can be merged into one substitution

                inners.forEach((pp,vv) -> {
                    compress(vv, (fp) -> {

                        Term c;
                        try {
                            if ((c = nar.index.transform(tn, pp, fp)) != null) {
                                task(in, c, generated);
                            }
                        } catch (InvalidTermException e) {
                            logger.warn("{}",e.toString());
                        }

                    });
                });

            }

            if (!generated.isEmpty()) {
                if (trace) {
                    logger.info("{}\n\t{}", in, Joiner.on("\n\t").join(generated));
                }

            }
            return generated;
        }

    }


    protected void compress(Termed<Compound> b, Consumer<Term> each) {
        Compound<?> bt = b.term();
        TermContainer<?> subs = bt.subterms();
        int negs = subs.count(x -> x.op() == Op.NEG);

        int subCount = bt.size();
        boolean negate = (negs == subCount);
        if (negs != 0 && !negate) {
            //only if none or all of the subterms are negated
            return;
        }

        if (!subs.equivalentStructures())
            return;

        if (!equalNonIntegerAtoms(subs))
            return;

        if (negate) {
            subs = $.neg((TermVector)subs);
        }

        //paths * extracted sequence of numbers at given path for each subterm
        Map<ByteList,List<Term>> data = new HashMap();

        //first subterm: infer location of all inductables
        BiPredicate<ByteList, Term> collect = (p, t) -> {
            if (!p.isEmpty() && t!=null) {
                List<Term> c = data.computeIfAbsent(p.toImmutable(), (pp) -> $.newArrayList(1));
                c.add(t);
            }
            return true;
        };

        Term first = subs.term(0);
        if (!(first instanceof Compound))
            return;

        //first.pathsTo(ArithmeticTest::intOrNullTerm, collect);
        first.pathsTo(x -> x, collect);

        //prefilter: no actual integer numbers found
        if (!data.values().stream().anyMatch(x -> matchable(x)))
            return;

        //analyze remaining subterms
        for (int i = 1; i < subCount; i++) {

            //if a subterm is not an integer, check for equality of atoms (structure already compared abovec)
            subs.term(i).pathsTo(
                    (e) -> e, //(e instanceof Atom && intOrNull(e)==null) ? e : null,
                    collect);
        }

        data.forEach((pp, nn) -> {

            //all subterms must contribute a value for this path
            if (nn.size()!=subCount)
                return;

            //for each path where the other numerics are uniformly equal (only one unique value)
            if (new HashSet(nn).size()==1) {


                //List<Term> features = $.newArrayList();
                final Compound pattern = (Compound) first;

                data.forEach((ppp, nnnt) -> {

                    if (!pp.equals(ppp)) {

                        List<Term> fff = features(nnnt);

                        for (Term ff : fff) {

                            //introduce variable
                            try {
                                @Nullable Term y = nar.index.transform(pattern, ppp, ff);
                                if (y instanceof Compound) {
                                    each.accept($.negIf(
                                            (Compound) y,
                                            negate
                                    ));
                                }
                            } catch (InvalidTermException e) {
                                logger.error("{}",e.toString());
                            }


                        }
                    }
                });

            }

        });


    }

    static boolean compareNonInteger(Term x, Term y) {
        boolean xint = (x instanceof Termject.IntTerm || x instanceof IntInterval);
        if (xint) {
            return (y instanceof Termject.IntTerm || y instanceof IntInterval);
        } else if (x instanceof Compound) {
            return x.op() == y.op() && x.size() == y.size() && ((Compound)x).dt() == ((Compound)y).dt();
        } else {
            return x.equals(y);
        }
    }

    final static Function<Term,Term> xx = x -> x;

    private boolean equalNonIntegerAtoms(TermContainer<?> subs) {
        Term first = subs.term(0);
        int ss = subs.size();
        return first.pathsTo(xx, (ByteList p, Term x)-> {
            for (int i = 1; i < ss; i++){
                Term y = subs.term(i);
                if (!p.isEmpty()) {
                    if (!compareNonInteger(x, ((Compound)y).subterm(p)))
                        return false;
                }/* else {
                    if (!compareNonInteger(x, y))
                        return false;
                }*/
            }
            return true;
        });
    }

    static boolean matchable(List<Term> x) {
        int intTerms = 0, intPreds = 0;
        int xSize = x.size();
        for (int i = 0; i < xSize; i++) {
            Term t = x.get(i);
            if (t instanceof Termject.IntTerm)
                intTerms++;
            else if (t instanceof Termject.IntPred)
                intPreds++;
        }
        return (intTerms + intPreds == xSize); //all either intTerm or intPred
    }

    //protected final GenericVariable var(int i, boolean varDep) {
        //return new GenericVariable(varDep ? Op.VAR_DEP : Op.VAR_INDEP, Integer.toString(i));
    //}

    private List<Term> features(List<Term> nnnt) {
        FasterList<Term> ll = $.newArrayList(0);

        RangeSet<Integer> intIntervals = ranges(nnnt);

        if (!intIntervals.isEmpty()) {
            Range<Integer> rNew = intIntervals.span();
            if (rNew.upperEndpoint() - rNew.lowerEndpoint() > 1) {

                boolean connected = true;
                Range q = null;
                for (Range r : intIntervals.asRanges()) {

                    if (q != null && !q.isConnected(r)) {
                        connected = false;
                        break;
                    }
                    q = r;
                }
                if (connected) {
                    //merge their spans
                    ll.add(new IntInterval(rNew));
                }
            }
        }

        //...

        return ll;
    }





    @Nullable Task task(Task b, Term c, Collection<Task> target) {

        if (b.isDeleted())
            return null;

        Task g = new GeneratedTask(
                c,
                b.punc(), b.truth())
                .time(nar.time(), b.occurrence())
                .budget(b)
                .evidence(b.evidence())
                .log(tag)
                ;
        if (g!=null)
            target.add(g);
        return g;
    }
}
