package nars.nal;

import com.google.common.collect.*;
import nars.$;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import nars.term.container.TermSet;
import nars.term.container.TermVector;
import nars.term.obj.IntTerm;
import nars.term.obj.Termject.IntInterval;
import nars.util.data.list.FasterList;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.ByteHashSet;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static nars.Op.INT;
import static nars.Op.INTRANGE;

/**
 * arithmetic rule mining & variable introduction
 */
public class ArithmeticInduction {


    public static Logger logger = LoggerFactory.getLogger(ArithmeticInduction.class);

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


    public static RangeSet<Integer> ranges(List<Term> term) {
        TreeRangeSet<Integer> r = TreeRangeSet.create();
        for (Term x : term) {
            if (x.op() == INTRANGE)
                r.add(((IntInterval) x).val);
            else if (x.op() == INT)
                r.add(Range.singleton(((IntTerm) x).val()).canonical(DiscreteDomain.integers()));
        }
        return r;
    }


    @Nullable
    public static Integer intOrNull(Term term) {
        if (term.op() == INT) {
            return ((IntTerm) term).val();
        }
        return null;
    }


//    public static Set<Task> compress(Term in) {
//
//        if (!in.isBeliefOrGoal()) {
//
//            return Collections.emptySet();
//        } else {
//
//            Set<Task> generated = new HashSet();
//
//            int bdt = in.dt();
//            Op o = in.op();
//
//            //attempt to compress all subterms to a single rule
//            if ((/*(o == EQUI || o == IMPL || */(o == CONJ) && ((bdt == DTERNAL) || (bdt == 0)))
//                    ||
//                    (o.isSet())
//                    ||
//                    (o.isIntersect())) {
//
//                compress(in, (pattern) -> {
//
//                    task(in, pattern, generated);
//
//                });
//            }
//
//
//            //attempt to replace all subterms of an embedded conjunction subterm
//            Compound tn = in.term();
//            if ((o != CONJ && o!=EQUI && o!=IMPL) && tn.subterms().hasAny(CONJ)) {
//
//
//                //attempt to transform inner conjunctions
//                Map<ByteList, Compound> inners = $.newHashMap();
//
//                //Map<Compound, List<ByteList>> revs = $.newHashMap(); //reverse mapping to detect duplicates
//
//                tn.pathsTo(x -> x.op()==CONJ &&
//                        ((((Compound)x).dt() == 0) || ((Compound)x).dt() == DTERNAL)  ? x : null, (p, v) -> {
//                    if (!p.isEmpty())
//                        inners.put(p.toImmutable(), (Compound)v);
//                    return true;
//                });
//
//                //TODO see if duplicates exist and can be merged into one substitution
//
//                inners.forEach((pp,vv) -> {
//                    compress(vv, (fp) -> {
//
//                        Term c;
//                        try {
//                            if ((c = $.terms.transform(tn, pp, fp)) != null) {
//                                task(in, c, generated);
//                            }
//                        } catch (InvalidTermException e) {
//                            logger.warn("{}",e.toString());
//                        }
//
//                    });
//                });
//
//            }
//
//            if (!generated.isEmpty()) {
//                if (trace) {
//                    logger.info("{}\n\t{}", in, Joiner.on("\n\t").join(generated));
//                }
//
//            }
//            return generated;
//        }
//
//    }

    @NotNull
    public static TermContainer compress(@NotNull TermContainer subs) {
        int negs = subs.count(x -> x.op() == Op.NEG);

        int subCount = subs.size();
        boolean negate = (negs == subCount);
        if (negs != 0 && !negate) {
            //only if none or all of the subterms are negated
            return subs;
        }

        if (!subs.equivalentStructures())
            return subs;

        if (!equalNonIntegerAtoms(subs))
            return subs;

        if (negate) {
            subs = $.neg((TermVector) subs);
        }

        //paths * extracted sequence of numbers at given path for each subterm
        Map<ByteList, Pair<ByteHashSet, List<Term>>> data = new HashMap();


        //analyze subtermss
        for (int i = 0; i < subCount; i++) {
            //if a subterm is not an integer, check for equality of atoms (structure already compared abovec)
            @NotNull Term f = subs.term(i);

            //first subterm: infer location of all inductables
            int ii = i;
            BiPredicate<ByteList, Term> collect = (p, t) -> {
                if ((!p.isEmpty() || (t instanceof IntTerm) || (t instanceof IntInterval))) {
                    Pair<ByteHashSet, List<Term>> c = data.computeIfAbsent(p.toImmutable(), (pp) ->
                            Tuples.pair(new ByteHashSet(), $.newArrayList(1)));
                    c.getOne().add((byte) ii);
                    c.getTwo().add(t);
                }

                return true;
            };

            //if (f instanceof Compound) {
            f.pathsTo(x -> x, collect);
            /*} else {
                if (f instanceof IntTerm) //raw atomic int term
                    data.put(new ByteArrayList(new byte[] {0}), $.newArrayList(f));
            }*/
        }

        for (Map.Entry<ByteList, Pair<ByteHashSet, List<Term>>> e : data.entrySet()) {
            //data.forEach((pp, nn) -> {
            ByteList pp = e.getKey();
            Pair<ByteHashSet, List<Term>> nn = e.getValue();

            ByteHashSet involved = nn.getOne();
            int numInvolved = involved.size(); //# of subterms involved

            //at least 2 subterms must contribute a value for each path
            if (numInvolved < 2)
                continue;

            //for each path where the other numerics are uniformly equal (only one unique value)
            /*if (new HashSet(nn).size()==1)*/


            //List<Term> features = $.newArrayList();
            //final Compound pattern = (Compound) first;

            //data.forEach((ppp, nnnt) -> {

                    /*if (!pp.equals(ppp))*/

            List<Term> ff = features(nn.getTwo());
            if (ff.isEmpty() || ff.size() >= numInvolved) {
                //nothing would be gained; dont bother
                continue;
            }


            TreeSet<Term> s = new TreeSet();
            byte j = 0;
            Term template = null;
            for (Term x : subs) {
                if (!involved.contains(j)) {
                    s.add(x);
                } else {
                    //x is contained within range expression p
                    template = x;
                }
                j++;
            }

            try {
                for (Term f : ff) {
                    Term y = template instanceof Compound ? $.negIf((Compound) $.terms.transform((Compound) template, pp, f), negate) : f;
                    s.add(y);
                }

                return TermSet.the(s);
            } catch (ClassCastException eee) {
                //return subs; //HACK
                continue; //HACK
            }


//
//                for (Term ff : fff) {
//
////                            //introduce variable
////                            try {
////                                @Nullable Term y = $.terms.transform(pattern, ppp, ff);
////                                if (y instanceof Compound) {
////                                    @Nullable Compound z = $.negIf(
////                                            (Compound) y,
////                                            negate
////                                    );
////                                    //return z;
////                                    //each.accept(z);
////                                }
////                            } catch (InvalidTermException e) {
////                                logger.error("{}",e.toString());
////                            }
//
//
//                }
            //});

        }

        return subs;

    }

    static boolean compareNonInteger(Term x, Term y) {
        boolean xint = (x.op() == INT || x.op() == INTRANGE);
        if (xint) {
            return (y.op() == INT || y.op() == INTRANGE);
        } else if (x instanceof Compound) {
            return x.op() == y.op() && x.size() == y.size() && ((Compound) x).dt() == ((Compound) y).dt();
        } else {
            return x.equals(y);
        }
    }

    final static Function<Term, Term> xx = x -> x;

    private static boolean equalNonIntegerAtoms(TermContainer subs) {
        Term first = subs.term(0);
        int ss = subs.size();
        return first.pathsTo(xx, (ByteList p, Term x) -> {
            for (int i = 1; i < ss; i++) {
                Term y = subs.term(i);
                if (!p.isEmpty()) {
                    if (!compareNonInteger(x, ((Compound) y).subterm(p)))
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
            if (t.op() == INT)
                intTerms++;
            else if (t.op() == INTRANGE)
                intPreds++;
        }
        return (intTerms + intPreds == xSize); //all either intTerm or intPred
    }

    //protected final GenericVariable var(int i, boolean varDep) {
    //return new GenericVariable(varDep ? Op.VAR_DEP : Op.VAR_INDEP, Integer.toString(i));
    //}

    private static List<Term> features(List<Term> nnnt) {
        FasterList<Term> ll = $.newArrayList(0);

        RangeSet<Integer> intIntervals = ranges(nnnt);

        //if (!intIntervals.isEmpty()) {
        //Range<Integer> rNew = intIntervals.span();
        //if (rNew.upperEndpoint() - rNew.lowerEndpoint() > 1) {

        //boolean connected = true;
        //Range q = null;
        for (Range<Integer> rr : intIntervals.asRanges()) {
            int l = rr.lowerEndpoint();
            int u = rr.upperEndpoint();
            if (rr.lowerBoundType() == BoundType.OPEN)
                l++;
            if (rr.upperBoundType() == BoundType.OPEN)
                u--;
            if (u - l == 0)
                ll.add($.the(l)); //just the individual number
            else
                ll.add(new IntInterval(l, u));
        }
        //}
        //}

        //...

        return ll;
    }


//    @Nullable Task task(Task b, Term c, Collection<Task> target) {
//
//        if (b.isDeleted())
//            return null;
//
//        Task g = new GeneratedTask(
//                c,
//                b.punc(), b.truth())
//                .time(nar.time(), b.occurrence())
//                .budget(b)
//                .evidence(b.evidence())
//                .log(tag)
//                ;
//        if (g!=null)
//            target.add(g);
//        return g;
//    }


//    public static <X> Set<X> pluck(List<Term> term, Function<Term,X> p) {
//        Set<X> s = new HashSet();
//        for (Term x : term) {
//            X y = p.apply(x);
//            if (y!=null)
//                s.add(y);
//        }
//        return s;
//    }
}
