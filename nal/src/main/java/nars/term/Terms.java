package nars.term;

import com.gs.collections.api.block.predicate.primitive.IntObjectPredicate;
import nars.$;
import nars.Global;
import nars.Op;
import nars.concept.ConceptBuilder;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.util.Texts;
import nars.util.data.sorted.SortedList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.nal.Tense.DTERNAL;

/**
 * Static utility class for static methods related to Terms
 * <p>
 * Also serves as stateless/memory-less transient static (on-heap) TermIndex
 *
 * @author me
 */
public class Terms extends TermBuilder implements TermIndex {

    @NotNull public static final Terms terms = new Terms();
    @NotNull public static final int[] ZeroIntArray = new int[0];
    @NotNull public static final Term[] empty = new Term[0];
    @NotNull public static final TermVector<?> ZeroSubterms = new TermVector<>((Term[])new Term[] { });
    @NotNull public static final Compound ZeroProduct = $.compound(Op.PRODUCT, ZeroSubterms);
    @NotNull public static final IntFunction<Term[]> NewTermArray = Term[]::new;


    Terms() {

    }

    /**
     * TODO decide on some reasonable coding scheme for bundling these numeric values
     * into 32-bit or 64-bit fields/arrays
     */
    public static int hashVar(@NotNull Op type, int id) {
        return (type.ordinal() << 16) | id;
    }

    /** computes the content hash while accumulating subterm metadata summary fields into int[] meta */
    public static int hashSubterms(@NotNull Term[] term, int[] meta) {
        int h = 1;
        for (Term t : term) {
            h = 31 /*Util.PRIME1 */ * h + t.init(meta);
        }
        return h;
    }

    /** should be consistent with the other hash method(s) */
    public static int hashSubterms(@NotNull TermContainer<?> container) {
        int h = 1;
        for (Term t : container) {
            h = 31 /*Util.PRIME1 */ * h + t.hashCode();
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

    @NotNull @Override
    public Termed make(@NotNull Op op, int relation, @NotNull TermContainer subterms, int dt) {
        return new GenericCompound(op, relation, subterms).dt(dt);
    }

    @Override
    public
    @Nullable
    Termed get(Termed t, boolean createIfMissing) {
        return createIfMissing ? t : null;
    }

    @Nullable
    @Override
    public Termed set(@NotNull Termed t) {
        throw new UnsupportedOperationException();
    }


    @NotNull
    @Override
    public TermBuilder builder() {
        return this;
    }

    @Nullable
    @Override
    public ConceptBuilder conceptBuilder() {
        return null;
    }



    public static boolean equalSubTermsInRespectToImageAndProduct(@Nullable Term a, @Nullable Term b) {

        /*if (a == null || b == null) {
            return false;
        } else {*/
            Op o = a.op();
            boolean equalOps = (o == b.op());

            if (equalOps) {
                if (a.equals(b))
                    return false;

                switch (o) {
                    case INHERIT:
                        return equalSubjectPredicateInRespectToImageAndProduct((Compound) a, (Compound) b);

                    case SIMILAR:
                        //only half seems necessary:
                        //boolean y = equalSubjectPredicateInRespectToImageAndProduct((Compound) b, (Compound) a);
                        return equalSubjectPredicateInRespectToImageAndProduct((Compound) a, (Compound) b);
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
        } else if (!A.hasAny(Op.ProductOrImageBits) || !B.hasAny(Op.ProductOrImageBits)) {
            return false; //the remaining comparisons are unnecessary
        }

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


        if ((sao == PRODUCT) && (pbo == IMAGE_EXT)) {
            ta = predA;
            sa = subjA;
            tb = subjB;
            sb = predB;
        }

        if ((sbo == PRODUCT) && (pao == IMAGE_EXT)) {
            ta = subjA;
            sa = predA;
            tb = predB;
            sb = subjB;
        }

        if ((pao == IMAGE_EXT) && (pbo == IMAGE_EXT)) {
            ta = subjA;
            sa = predA;
            tb = subjB;
            sb = predB;
        }

        if ((sao == IMAGE_INT) && (sbo == IMAGE_INT)) {
            ta = predA;
            sa = subjA;
            tb = predB;
            sb = subjB;
        }

        if ((pao == PRODUCT) && (sbo == IMAGE_INT)) {
            ta = subjA;
            sa = predA;
            tb = predB;
            sb = subjB;
        }

        if ((pbo == PRODUCT) && (sao == IMAGE_INT)) {
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

            return csa.relation() == csb.relation() && containsAll(csa, ta, csb, tb);
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
    public static boolean contains(@NotNull Term[] container, @NotNull Term v) {
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

    public static Term[] toSortedSetArray(@NotNull Term... arg) {
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

    public static void printRecursive(@NotNull Term x, int level) {
        //indent
        for (int i = 0; i < level; i++)
            System.out.print("  ");

        System.out.print(x);
        System.out.print(" (");
        System.out.print(x.op() + "[" + x.getClass().getSimpleName() + "] ");
        System.out.print("c" + x.complexity() + ",v" + x.volume() + ' ');
        System.out.print(Integer.toBinaryString(x.structure()) + ')');
        System.out.println();

        if (x instanceof Compound) {
            for (Term z : ((Compound<?>) x))
                printRecursive(z, level + 1);
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
            for (Term z : ((Compound<?>) x))
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
        Set<Term> l = Global.newHashSet(t.length);
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
        term.recurseTerms((t, p) -> {
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

        List<Term> l = Global.newArrayList(s);

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
        float len = Math.max(a.length(), b.length());
        if (len == 0) return 0;
        return Texts.levenshteinDistance(a, b) / len;
    }

    /**
     * heuristic which evaluates the semantic similarity of two terms
     * returning 1f if there is a complete match, 0f if there is
     * a totally separate meaning for each, and in-between if
     * some intermediate aspect is different (ex: temporal relation dt)
     */
    public static float termRelevance(@NotNull Compound a, @NotNull Compound b) {
        Op aop = a.op();
        if (aop != b.op()) return 0f;
        if (aop.isTemporal()) {
            int at = a.dt();
            int bt = b.dt();
            if ((at != bt) && (at!=DTERNAL) && (bt!=DTERNAL)) {
//                if ((at == DTERNAL) || (bt == DTERNAL)) {
//                    //either is atemporal but not both
//                    return 0.5f;
//                }

//                boolean symmetric = aop.isCommutative();
//
//                if (symmetric) {
//                    int ata = Math.abs(at);
//                    int bta = Math.abs(bt);
//                    return 1f - (ata / ((float) (ata + bta)));
//                } else {
//                    boolean ap = at >= 0;
//                    boolean bp = bt >= 0;
//                    if (ap ^ bp) {
//                        return 0; //opposite direction
//                    } else {
//                        //same direction
                        return 1f - (Math.abs(at - bt) / (1f + Math.abs(at + bt)));
//                    }
//                }
            }
        }
        return 1f;
    }

    public static Term empty(@NotNull Op op) {
        switch (op) {

            case PRODUCT:
                return ZeroProduct;
            default:
                return null;
        }
    }

    public static int opRel(int opOrdinal, int relation) {
        return opOrdinal << 16 | (relation & 0xffff);
    }

    public static int opRel(@NotNull Op op, int relation) {
        return opRel(op.ordinal(), relation);
    }

    public static int opComponent(int oprel) {
        return oprel >> 16;
    }
    public static int relComponent(int oprel) {
        return oprel & 0xffff; //HACK do something here with signed and unsigned short/int
    }

    @Override
    public int size() {
        return 0;
    }


    @Override
    public
    @Nullable
    TermContainer theSubterms(TermContainer s) {
        return s;
    }

    @Override
    public int subtermsCount() {
        return 0;
    }

    @Override
    public void clear() {

    }

    @Override
    public void forEach(Consumer<? super Termed> c) {

    }

}
