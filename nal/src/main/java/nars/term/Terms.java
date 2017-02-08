package nars.term;

import jcog.Texts;
import jcog.Util;
import jcog.data.sorted.SortedList;
import jcog.list.FasterList;
import nars.$;
import nars.Op;
import nars.term.compound.GenericCompound;
import nars.term.compound.Statement;
import nars.term.container.ArrayTermVector;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;
import java.util.function.*;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;

/**
 * Static utility class for static methods related to Terms
 * <p>
 * Also serves as stateless/memory-less transient static (on-heap) TermIndex
 *
 * @author me
 */
public class Terms   {

    //@NotNull public static final int[] ZeroIntArray = new int[0];
    @NotNull public static final Term[] empty = new Term[0];
    @NotNull public static final TermVector NoSubterms =  new ArrayTermVector((Term[]) new Term[]{});
    @NotNull public static final Compound ZeroProduct = new GenericCompound(Op.PROD, NoSubterms);
    @NotNull public static final IntFunction<Term[]> NewTermArray = Term[]::new;

    /**
     * TODO decide on some reasonable coding scheme for bundling these numeric values
     * into 32-bit or 64-bit fields/arrays
     */
    public static int hashVar(@NotNull Op type, int id) {
        return (type.ordinal() << 16) | id;
    }

    /** computes the content hash while accumulating subterm metadata summary fields into int[] meta */
    public static int hashSubterms(@NotNull Term[] term, @NotNull int[] meta) {
        int h = 1;
        for (Term t : term) {
            h = 31 /*Util.PRIME1 */ * h + t.init(meta);
        }
        return h;
    }

    /** should be consistent with the other hash method(s) */
    public static int hashSubterms(@NotNull TermContainer container) {
        int h = 1;
        int s = container.size();
        for (int i = 0; i < s; i++) {
            h = container.term(i).hashCode() + h * 31 /*Util.PRIME1 */;
        }
        return h;
    }

    /** match a range of subterms of Y.  */
    @NotNull
    public static Term[] subRange(@NotNull Compound c, int from, int to) {
        int s = to-from;

        Term[] l = new Term[to-from];

        int x = 0, y = from;
        for (int i = 0; i < s; i++) {
            l[x++] = c.term(y++);
        }

        return l;
    }

    public static boolean equalOrNegationOf(@Nullable Term a, @Nullable Term b) {
        if (a == null || b == null)
            return false;

        if (a.equals(b)) {
            return true;
        }
        if (a.op() == NEG) {
            return ((Compound)a).term(0).equals(b);
        } else if (b.op() == NEG) {
            return ((Compound)b).term(0).equals(a);
        }
        return false;
    }



    @Deprecated public static boolean equalSubTermsInRespectToImageAndProduct(@Nullable Term a, @Nullable Term b) {

        /*if (a == null || b == null) {
            return false;
        } else {*/
            Op o = a.op();
            boolean equalOps = (o == b.op());

            if (equalOps) {

                switch (o) {
                    case INH:
                        return equalSubjectPredicateInRespectToImageAndProduct((Compound) a, (Compound) b);

                    case SIM:
                        //only half seems necessary:
                        //boolean y = equalSubjectPredicateInRespectToImageAndProduct((Compound) b, (Compound) a);
                        return equalSubjectPredicateInRespectToImageAndProduct((Compound) a, (Compound) b);

                    default:
                        if (Terms.equalAtemporally(a, b))
                            return false;
                        break;
                }
            }


            if ((a instanceof Compound) && (b instanceof Compound)) {
                //both are compounds


                Compound A = ((Compound) a);
                Compound B = ((Compound) b);
                int aLen = A.size();
                if (aLen != B.size()) {
                    return false;
                } else {

                    //match all subterms
                    for (int i = 0; i < aLen; i++) {
                        if (!equalSubTermsInRespectToImageAndProduct(A.term(i), B.term(i)))
                            return false;
                    }
                    return true;
                }
            }

            return false;




    }


    public static boolean equalSubjectPredicateInRespectToImageAndProduct(@NotNull Compound A, @NotNull Compound B) {

        if (A.equals(B)) {
            return true;
        }

        int as = A.structure();
        if (/*!hasAny(as, Op.PRODUCT) || */!hasAny(as, Op.ImageBits))
            return false;
        int bs = B.structure();
        if (/*!hasAny(bs, Op.PRODUCT) || */!hasAny(bs, Op.ImageBits))
            return false;

//        if (!A.hasAny(Op.PRODUCT) || !B.hasAny(Op.PRODUCT) || !A.hasAny(Op.ImageBits) || !B.hasAny(Op.ImageBits)) {
//            //product and one of either image types
//            return false; //the remaining comparisons are unnecessary
//        }

        Term subjA = Statement.subj(A);
        Term predA = Statement.pred(A);
        Term subjB = Statement.subj(B);
        Term predB = Statement.pred(B);

        Term ta = null, tb = null; //the compound term to put itself in the comparison set
        Term sa = null, sb = null; //the compound term to put its components in the comparison set

        Op sao = subjA.op();
        Op sbo = subjB.op();
        Op pao = predA.op();
        Op pbo = predB.op();


        if ((sao == PROD) && (pbo == IMGe)) {
            ta = predA;
            sa = subjA;
            tb = subjB;
            sb = predB;
        }

        if ((sbo == PROD) && (pao == IMGe)) {
            ta = subjA;
            sa = predA;
            tb = predB;
            sb = subjB;
        }

        if ((pao == IMGe) && (pbo == IMGe)) {
            ta = subjA;
            sa = predA;
            tb = subjB;
            sb = predB;
        }

        if ((sao == IMGi) && (sbo == IMGi)) {
            ta = predA;
            sa = subjA;
            tb = predB;
            sb = subjB;
        }

        if ((pao == PROD) && (sbo == IMGi)) {
            ta = subjA;
            sa = predA;
            tb = predB;
            sb = subjB;
        }

        if ((pbo == PROD) && (sao == IMGi)) {
            ta = predA;
            sa = subjA;
            tb = subjB;
            sb = predB;
        }

        if (ta != null) {
            //original code did not check relation index equality
            //https://code.google.com/p/open-nars/source/browse/trunk/nars_core_java/nars/language/CompoundTerm.java
            //if (requireEqualImageRelation) {
            //if (sa.op().isImage() && sb.op().isImage()) {
            Compound csa = (Compound) sa;
            Compound csb = (Compound) sb;

            return csa.dt() == csb.dt() && containsAll(csa, ta, csb, tb);
        } else {
            return false;
        }

    }

    private static boolean containsAll(@NotNull TermContainer sat, Term ta, @NotNull TermContainer sbt, Term tb) {
        //set for fast containment check
        Set<Term> componentsA = sat.toSet();
        componentsA.add(ta);

        //test A contains B
        if (!componentsA.contains(tb))
            return false;

        Term[] sbtt = sbt.terms();
        for (Term x : sbtt) {
            if (!componentsA.contains(x))
                return false;
        }

        return true;
    }


    /**
     * brute-force equality test
     */
    public static boolean contains(@NotNull Term[] container, @NotNull Termlike v) {
        for (Term e : container)
            if (v.equals(e))
                return true;
        return false;
    }


    @NotNull
    public static Term[] reverse(@NotNull Term[] arg) {
        int l = arg.length;
        Term[] r = new Term[l];
        for (int i = 0; i < l; i++) {
            r[i] = arg[l - i - 1];
        }
        return r;
    }


    public static Term[] sorted(@NotNull Term... arg) {
        switch (arg.length) {

            case 0:
                return Terms.empty;

            case 1:
                return arg; //new Term[] { arg[0] };
            case 2:
                Term a = arg[0];
                Term b = arg[1];
                int c = a.compareTo(b);

//                if (Global.DEBUG) {
//                    //verify consistency of compareTo() and equals()
//                    boolean equal = a.equals(b);
//                    if ((equal && (c!=0)) || (!equal && (c==0))) {
//                        throw new RuntimeException("invalid order (" + c + "): " + a + " = " + b);
//                    }
//                }

                if (c < 0) return arg; //same as input //new Term[]{a, b};
                else if (c > 0) return new Term[]{b, a};
                else /*if (c == 0)*/ return new Term[]{a}; //equal

                //TODO fast sorted array for arg.length == 3

            default:
                //terms > 2:
                return new SortedList<>(arg).toArray(NewTermArray);
        }
    }

    public static void printRecursive(@NotNull PrintStream out, @NotNull Term x, int level) {
        //indent
        for (int i = 0; i < level; i++)
            out.print("  ");

        out.print(x);
        out.print(" (");
        out.print(x.op() + "[" + x.getClass().getSimpleName() + "] ");
        out.print("c" + x.complexity() + ",v" + x.volume() + ' ');
        out.print(Integer.toBinaryString(x.structure()) + ')');
        out.println();

        if (x instanceof Compound) {
            for (Term z : ((Compound) x))
                printRecursive(out, z, level + 1);
        }
    }

    /**
     * for printing complex terms as a recursive tree
     */
    public static void printRecursive(Term x, @NotNull Consumer<String> c) {
        printRecursive(x, 0, c);
    }

    public static void printRecursive(Term x, int level, @NotNull Consumer<String> c) {
        //indent
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < level; i++)
            line.append("  ");

        line.append(x);

        if (x instanceof Compound) {
            for (Term z : ((Compound) x))
                printRecursive(z, level + 1, c);
        }

        c.accept(line.toString());
    }


    /**
     * build a component list from terms
     *
     * @return the component list
     */
    public static Term[] toArray(Term... t) {
        return t;
    }

    public static List<Term> toList(Term[] t) {
        return Arrays.asList(t);
    }

    /**
     * makes a set from the array of terms
     */
    @NotNull
    public static Set<Term> toSet(@NotNull Term[] t) {
        if (t.length == 1)
            return Collections.singleton(t[0]);
        Set<Term> l = $.newHashSet(t.length);
        Collections.addAll(l, t);
        return l;
    }

    @NotNull
    @SafeVarargs
    public static <T> Set<T> toSortedSet(@NotNull T... t) {

        int l = t.length;
        if (l == 1)
            return Collections.singleton(t[0]);

        TreeSet<T> s = new TreeSet();
        Collections.addAll(s, t);
        return s;
    }

    public static int maxLevel(@NotNull Term term) {
        int[] max = {0};
        term.recurseTerms((t) -> {
            int m = t.op().minLevel;
            if (m > max[0])
                max[0] = m;
        });
        return max[0];
    }

    @Nullable
    public static Term[] concat(@Nullable Term[] a, @NotNull Term... b) {

        if (a == null) {
            return null;
        }

        if (a.length == 0) return b;
        if (b.length == 0) return a;

        int L = a.length + b.length;

        //TODO apply preventUnnecessaryDeepCopy to more cases

        Term[] arr = new Term[L];

        int l = a.length;
        System.arraycopy(a, 0, arr, 0, l);
        System.arraycopy(b, 0, arr, l, b.length);

        return arr;
    }

    @NotNull
    public static <T extends Term> Term[] filter(@NotNull T[] input, @NotNull IntObjectPredicate<T> filter) {

        int s = input.length;

        List<Term> l = $.newArrayList(s);

        for (int i = 0; i < s; i++) {
            T t = input[i];
            if (filter.accept(i, t))
                l.add(t);
        }
        if (l.isEmpty()) return Terms.empty;
        return l.toArray(new Term[l.size()]);
    }

    @NotNull
    public static Term[] filter(@NotNull Term[] input, @NotNull IntPredicate filter) {
        return filter(input, (i, t) -> filter.test(i));
    }

    @NotNull
    public static Term[] filter(@NotNull Term[] input, @NotNull Predicate<Term> filter) {
        return filter(input, (i, t) -> filter.test(t));
    }

    @NotNull
    public static Term[] toArray(@NotNull Collection<Term> l) {
        int s = l.size();
        if (s == 0)
            return Terms.empty;
        return l.toArray(new Term[s]);
    }

    @NotNull
    public static Term[] cloneTermsReplacing(@NotNull Term[] term, Term from, @NotNull Term to) {
        Term[] y = new Term[term.length];
        int i = 0;
        for (Term x : term) {
            if (x.equals(from))
                x = to;
            y[i++] = x;
        }
        return y;
    }

    /**
     * returns lev distance divided by max(a.length(), b.length()
     */
    public static float levenshteinDistancePercent(@NotNull CharSequence a, @NotNull CharSequence b) {
        int len = Math.max(a.length(), b.length());
        if (len == 0) return 0f;
        return Texts.levenshteinDistance(a, b) / ((float)len);
    }


//    /** compare everything except dt() when not in image case */
//    @Deprecated private static boolean equalsAnonymous(@NotNull Compound a, @NotNull Compound b) {
//        int as = a.structure();
//        if (Op.hasAny(as, Op.TemporalBits)) {
//
//            if (as == b.structure()) {
//                Op ao = a.op();
//                if (ao == b.op()) {
//                    if (ao.image && a.dt() != b.dt()) //must match dt for image
//                        return false;
//
//                    return equalsAnonymous(a.subterms(), b.subterms());
//                }
//            }
//
//            return false;
//
//        } else {
//            //no temporal subterms
//            return a.equals(b);
//        }
//
//    }




    public static ImmutableSet<Term> unique(@NotNull Term c, @NotNull Predicate<Term> p) {
        UnifiedSet<Term> u = new UnifiedSet();
        c.recurseTerms(x -> {
            if (p.test(x)) {
                u.add(x);
            }
        });
        return u.toImmutable();
    }

    /** dangerous because some operations involving concepts can naturally reduce to atoms, and using this interprets them as non-existent */
    @Deprecated @Nullable public static Compound compoundOrNull(@Nullable Term t) {
        if (t instanceof Compound)
            return ((Compound) t);
        else
            return null;
    }

    /** dangerous because some operations involving concepts can naturally reduce to atoms, and using this interprets them as non-existent */
    @Deprecated @Nullable public static Compound compoundOrNull(@Nullable Termed t) {
        return t instanceof Compound ? ((Compound)t) : compoundOrNull(t.term());
    }

    /** detects a negated conjunction of negated subterms:
     *  (--, (&&, --A, --B, .., --Z) ) */
    public static boolean isDisjunction(@NotNull Compound c) {
        if (c.dt() == DTERNAL && c.op() == NEG && c.isTerm(0, CONJ)) {
            return allNegated(((Compound)c.term(0)).subterms());
        }
        return false;
    }


    public static boolean allNegated(@NotNull TermContainer subterms) {
        return subterms.and((Term t)-> t.op() == NEG);
    }


//    @Nullable
//    public static Term atemporalize(@NotNull Term c) {
//        if (c instanceof Compound)
//            return atemporalize((Compound)c);
//        return c;
//    }

    @NotNull
    public static Term atemporalize(@NotNull Term t) {
        return t instanceof Compound ? atemporalize((Compound) t) : t;
    }

    @NotNull
    public static Compound atemporalize(@NotNull Compound c) {


        TermContainer psubs = c.subterms();
        TermContainer newSubs;

        Op o = c.op();
        if (psubs.hasAny(Op.TemporalBits)) {
            boolean subsChanged = false;
            int cs = c.size();
            Term[] ss = new Term[cs];
            for (int i = 0; i < cs; i++) {

                Term m = psubs.term(i);
                if (m != (ss[i] = m instanceof Compound ? atemporalize((Compound)m) : m))
                    subsChanged = true;

            }
            int dt = c.dt();
            newSubs = subsChanged ? /*theSubterms(*/TermContainer.the(o,
                    DTERNAL,
                    //(dt == DTERNAL||dt==0) ? DTERNAL : XTERNAL /* preserve order */,
                    ss)/*)*/ : null;

        } else {
            newSubs = null;
        }


        int pdt = c.dt();
        boolean dtChanged = (pdt != DTERNAL && o.temporal);
        boolean subsChanged = newSubs != null;

        if (subsChanged || dtChanged) {

            Compound xx = $.terms.newCompound(o,
                    dtChanged ? DTERNAL : pdt,
                    subsChanged ? newSubs : psubs);

            if (c.isNormalized())
                xx.setNormalized();

            //Termed exxist = get(xx, false); //early exit: atemporalized to a concept already, so return
            //if (exxist!=null)
                //return exxist.term();


            //x = i.the(xx).term();
            return xx;
        } else {
            return c;
        }
    }

    /** returns the most optimal subterm that can be replaced with a variable, or null if one does not meet the criteria */
    @Nullable
    public static Term[] substMaximal(@NotNull Compound c, @NotNull Predicate<Term> include, int minCount, int minScore) {
        HashBag<Term> uniques = subtermScore(c,
            t -> include.test(t) ? t.volume() : 0 //sum by complexity if passes include filter
        );

        int s = uniques.size();
        if (s>0) {
            MutableList<ObjectIntPair<Term>> u = uniques.topOccurrences(s);
            for (ObjectIntPair<Term> p : u) {
                int score = p.getTwo();
                if (score >= minScore) {
                    Term subterm = p.getOne();
                    int count = score / subterm.complexity(); //should be a whole number according to the above scoring policy
                    if (count >= minCount) {
                        return new Term[] { subterm };
                    }
                }
            }

        }

        return null;
    }

    /** returns the most optimal subterm that can be replaced with a variable, or null if one does not meet the criteria */
    @Nullable
    public static Term[] substRoulette(@NotNull Compound c, @NotNull Predicate<Term> include, int minCount, Random rng) {
        HashBag<Term> uniques = subtermScore(c,
                t -> include.test(t) ? 1 : 0 //sum by complexity if passes include filter
        );

        int s = uniques.size();
        if (s > 0) {
            ObjectIntPair<Term>[] oi = new ObjectIntPair[s];
            final int[] j = {0};
            final int[] sum = {0};
            uniques.forEachWithOccurrences((Term t, int count)->{
                if (count >= minCount) {
                    int score = count * t.volume();
                    oi[j[0]++] = PrimitiveTuples.pair(t, score);
                    sum[0] += score;
                }
            });

            int available = j[0];
            if (available == 1) {
                return new Term[]{oi[0].getOne()};
            } else if (available > 1) {
                int selected = Util.selectRoulette(j[0], (i) -> oi[i].getTwo(), sum[0], rng);
                return new Term[]{oi[selected].getOne()};
            }
        }

        return null;
    }

    /** returns the most optimal subterm that can be replaced with a variable, or null if one does not meet the criteria */
    @Nullable
    public static Term[] substAllRepeats(@NotNull Compound c, @NotNull Predicate<Term> include, int minCount) {
        HashBag<Term> uniques = Terms.subtermScore(c,
                (sub) -> {
                    return include.test(sub) ? 1 : 0; //sum by complexity if passes include filter
                }
        );

        int s = uniques.size();
        if (s > 0) {

            FasterList<Term> oi = (FasterList)$.newArrayList();

            uniques.forEachWithOccurrences((Term t, int count) -> {
                if (count >= minCount) {
                    oi.add(t);
                }
            });

            //remove terms which are contained by other terms in the list

            oi.sort((a,b)->Integer.compare(a.volume(),b.volume())); //sorted by volume
            Iterator<Term> ii = oi.iterator();
            while (ii.hasNext()) {
                Term b = ii.next();
                if (oi.anySatisfy((a) ->
                        a!=b && a instanceof Compound && ((Compound)a).containsTermRecursively(b)
                )) {
                    ii.remove();
                }
            }

            return oi.toArray(new Term[oi.size()]);
        }
        return null;
    }


//    /** produces a Map<Object,Int> of 'scores' of how much complexity would be reduced by replacing all occurrences of that subterm with a variable */
//    @NotNull public static ObjectIntMap<Term> substAnalysis(Compound c, Predicate<Term> include, int minCount) {
//
//        HashBag<Term> uniques = subtermRepeats(c, include, t -> t.complexity());
//
//        if (!uniques.isEmpty()) {
//
//            //ObjectIntHashMap<Term> h = new ObjectIntHashMap<>(uniques.size());
//            uniques.forEachWithOccurrences((subterm, score) -> {
//                if (count >= minCount) {
//                    int xc = subterm.complexity();
//                    int score = xc * count;
//                    h.put(subterm, score);
//                }
//            });
//
//            if (!h.isEmpty()) {
//                h.compact();
//                return h;
//            }
//
//        }
//
//        return ObjectIntMaps.immutable.empty();
//    }

    /** counts the repetition occurrence count of each subterm within a compound */
    @NotNull
    public static HashBag<Term> subtermScore(@NotNull Compound c, @NotNull ToIntFunction<Term> score) {
        HashBag<Term> uniques = new HashBag<>(c.volume());

        c.recurseTerms((Term subterm) -> {
            int s = score.applyAsInt(subterm);
            if (s > 0)
                uniques.addOccurrences(subterm, s);
        });

        return uniques;
    }

    /**
     *  atemporally set equality AND with any outer negations removed
     * NOTE: assumes that x must already contain only atemporal terms */
    public static boolean equalAtemporally(@NotNull Set<Term> x, @NotNull Set<Term> y) {
        if (x.size()!=y.size())
            return false;
        for (Term yy : y) {
            @NotNull Term ay = Terms.atemporalize(yy);
            if (!x.contains(ay) && !x.contains($.neg(ay)))
                return false;
        }
        return true;
    }

    public static boolean equalAtemporally(@NotNull Termed a, @NotNull Termed b) {
        return equal(a.term(), b.term(), false, false);
    }

    /** equal atemporally AND with any outer negations removed
     * @param samePolarity whether the top-level polarity should be ignored (auto-unnegate)
     *
     */
    public static boolean equal(@NotNull Term a, @NotNull Term b, boolean sameTemporality, boolean samePolarity) {

        if (a == b)
            return true;

        if (!samePolarity) {
            a = a.unneg();
            b = b.unneg();
        }

        if (a.equals(b))
            return true;

        if (!sameTemporality) {
            return ((a.structure() == b.structure()) &&
                    (a.volume() == b.volume()) &&
                    (a.op() == b.op()) &&
                    atemporalize(b).equals(atemporalize(a)));
        }

        return false;
    }

    /** a Set is already duplicate free, so just sort it */
    public static Term[] sorted(Set<Term> s) {

        //1. deduplicated
        Term[] x = s.toArray(new Term[s.size()]);

        //2. sorted
        if ((x.length >= 2) && (!(s instanceof SortedSet)))
            Arrays.sort(x);

        return x;
    }

    interface SubtermScorer {
        public int score(Compound superterm, Term subterm);
    }

    /** counts the repetition occurrence count of each subterm within a compound */
    @NotNull
    public static HashBag<Term> subtermScore(@NotNull Compound c, @NotNull SubtermScorer score) {
        HashBag<Term> uniques = new HashBag<>(c.volume());

        c.recurseTerms((Term subterm, Compound superterm) -> {
            int s = score.score(superterm, subterm);
            if (s > 0)
                uniques.addOccurrences(subterm, s);
        });

        return uniques;
    }
}


//    private static boolean equalsAnonymous(@NotNull TermContainer a, @NotNull TermContainer b) {
//        if (a.volume() == b.volume()) {
//            int n = a.size();
//            if (n == b.size()) {
//                for (int i = 0; i < n; i++) {
//                    Term as = a.term(i);
//                    Term bs = b.term(i);
//                    //        if (as == bs) {
////            return true;
////        } else if (as instanceof Compound && bs instanceof Compound) {
////            return equalsAnonymous((Compound) as, (Compound) bs);
////        } else {
////            return as.equals(bs);
////        }
//                    if (!Terms.equalAtemporally(as, bs))
//                        return false;
//                }
//                return true;
//            }
//        }
//        return false;
//    }
