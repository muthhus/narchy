package nars.nal;

import com.google.common.collect.*;
import nars.$;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
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

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static nars.Op.INT;

/**
 * arithmetic rule mining & variable introduction
 * WARNING: use of ByteArrayLists limits compound terms to max length of ~127
 */
public class ArithmeticInduction {


    public static Logger logger = LoggerFactory.getLogger(ArithmeticInduction.class);

    @NotNull
    public static TermContainer compress(@NotNull TermContainer subs) {

        int subCount = subs.size();
        if (subCount == 1 || !subs.hasAny(Op.INT))
            return subs; //early exit condition


        ListMultimap<ByteList, Term> subTermStructures = MultimapBuilder.hashKeys().arrayListValues().build();
        for (Term x : subs)
            subTermStructures.put(x.structureKey(), x);

        int numUniqueSubstructures = subTermStructures.keySet().size();
        if (numUniqueSubstructures == subCount) {
            return subs; //each subterm has a unique structure so nothing will be combined
        } else if (numUniqueSubstructures > 1) {
            //recurse with each sub-structure group and re-combine

            Set<Term> ss = new TreeSet();
            for (Collection<Term> stg : subTermStructures.asMap().values()) {
                int gs = stg.size();

                if (gs > 1) {
                    TermContainer gg = TermVector.the(stg);
                    gg = compress(gg);
                    for (Term ggg : gg)
                        ss.add(ggg);
                } else {
                    ss.addAll(stg);
                }

            }

            return recompressIfChanged(subs, ss);
        }

        //group again according to appearance of unique atoms
        ListMultimap<List<Term>, Term> subAtomSeqs = MultimapBuilder.hashKeys().arrayListValues().build();
        for (Term x : subs)
            subAtomSeqs.put(atomSeq(x), x);

        int ssa = subAtomSeqs.keySet().size();
        if (ssa == subCount) {
            return subs;
        } else if (ssa > 1) {
            //process each unique atom seq group:
            Set<Term> ss = new TreeSet();
            for (Collection<Term> ssg : subAtomSeqs.asMap().values()) {
                TermContainer gg = TermVector.the(ssg);
                gg = compress(gg);
                for (Term ggg : gg)
                    ss.add(ggg);
            }
            return recompressIfChanged(subs, ss);
        }


//        int negs = subs.count(x -> x.op() == Op.NEG);
//        boolean negate = (negs == subCount);
//        if (negs != 0 && !negate) {
//            //only if none or all of the subterms are negated
//            return subs;
//        }

        if (!subs.equivalentStructures())
            return subs;

        if (!equalNonIntegerAtoms(subs))
            return subs;

//        if (negate) {
//            subs = $.neg((TermVector) subs);
//        }

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

        TreeSet<Term> result = new TreeSet();
        Set<Term> subsumed = new HashSet();

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



            List<IntInterval> ff = features(nn.getTwo());
            if (ff.isEmpty() || ff.size() >= numInvolved) {
                //nothing would be gained; dont bother
                continue;
            }


            for (IntInterval f : ff) {
                byte j = 0;
                for (Term x : subs) {

                    if (!involved.contains(j)) {
                        result.add(x);
                        //System.out.println("1: " + result);
                    } else {


                        //x is contained within range expression p
                        Term xpp = x instanceof Compound ? ((Compound) x).subterm(pp) : x;

                        boolean contained;
                        if (xpp instanceof IntTerm) {
                            contained = (f.val.contains(((IntTerm) xpp).val));
                        } else if (xpp instanceof IntInterval) {
                            contained = (f.val.encloses(((IntInterval) xpp).val));
                        } else {
                            contained = false;
                        }

                        if (contained) {
                            Term y = x instanceof Compound ?
                                    $.terms.transform((Compound) x, pp, f)
                                    : f;
                            //if (!y.equals(x)) {

                            if (!x.equals(y)) {
                                result.remove(x);
                                subsumed.add(x);
                            }
                            result.add(y);
                            //System.out.println(x + " 3: " + result + "\t + " + y);
                            //}
                        } else {
                            result.add(x);
                        }
                    }
                    j++;
                }

                if (result.size() == 1) {
                    break; //reduced to one, go no further
                }
            }

        }

        result.removeAll(subsumed);

        if (result.isEmpty()) {
            return subs;
        } else {
            return recompressIfChanged(subs, result);
        }

    }

    public
    @NotNull
    static TermContainer recompressIfChanged(@NotNull TermContainer subs, Set<Term> ss) {
        //try {
        TermSet newSubs = TermSet.the(ss);
        if (newSubs.equals(subs))
            return subs; //nothing changed
        else
            return compress(newSubs);
//        } catch (StackOverflowError e) {
//            throw new RuntimeException("compression: " + subs + " " + ss);
//        }

    }

    private static List<Term> atomSeq(Term x) {
        if (x instanceof Compound) {
            List<Term> s = $.newArrayList(0);
            x.recurseTerms(v -> {
                if ((v instanceof Atomic && v.op() != INT)) {
                    s.add(v);
                }
            });
            return s;
        } else {
            return Collections.emptyList();
        }
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


    public static RangeSet<Integer> ranges(List<Term> term) {
        TreeRangeSet<Integer> r = TreeRangeSet.create();
        for (Term x : term) {
            if (x instanceof IntInterval)
                r.add(((IntInterval) x).val);
            else if (x instanceof IntTerm)
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


    static boolean compareNonInteger(Term x, Term y) {
        if ((x.op() == INT)) {
            return (y.op() == INT);
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


    //protected final GenericVariable var(int i, boolean varDep) {
    //return new GenericVariable(varDep ? Op.VAR_DEP : Op.VAR_INDEP, Integer.toString(i));
    //}

    private static List<IntInterval> features(List<Term> nnnt) {
        FasterList<IntInterval> ll = $.newArrayList(0);

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
            if (u - l == 0) {
                //ll.add($.the(l)); //just the individual number
            } else {
                ll.add(new IntInterval(l, u));
            }
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
