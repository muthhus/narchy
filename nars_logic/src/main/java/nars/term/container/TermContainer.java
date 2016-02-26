package nars.term.container;

import com.gs.collections.api.block.predicate.primitive.IntObjectPredicate;
import com.gs.collections.api.set.MutableSet;
import com.gs.collections.impl.factory.Sets;
import nars.$;
import nars.Global;
import nars.Op;
import nars.term.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;


/**
 * Methods common to both Term and Subterms
 * T = subterm type
 */
public interface TermContainer<T extends Term> extends Termlike, Comparable, Iterable<T> {

    int varDep();

    int varIndep();

    int varQuery();

    int varPattern();

    int vars();

    /** gets subterm at index i */
    @Nullable T term(int i);

    /** tests if subterm i is op o */
    default boolean term(int i, @NotNull Op o) {
        @Nullable T ti = term(i);
        return ti!=null && (ti.op() == o);
    }

    @Nullable
    default Term termOr(int i, Term ifOutOfBounds) {
        return size() <= i ? ifOutOfBounds : term(i);
    }

    default @NotNull  MutableSet<Term> toSet() {
        return Sets.mutable.of(terms());
    }

    static @NotNull MutableSet<Term> intersect(@NotNull TermContainer a, @NotNull TermContainer b) {
        return Sets.intersect(a.toSet(),b.toSet());
    }

    default boolean isEmpty() {
        return size() != 0;
    }

    /**
     * scans first level only, not recursive
     */
    default boolean contains(Object o) {
        return o instanceof Term && containsTerm((Term) o);
    }


    static boolean equals(TermContainer a, Object b) {
        return b instanceof TermContainer && TermContainer.equals(a, (TermContainer)b);
    }

    /** can be called from equals() */
    static boolean equals(TermContainer a, TermContainer b) {

        return
                (a == b) ||

                ((a.equalMeta(b)) &&
                 (a.equalTerms(b)));
    }

    default boolean equalMeta(TermContainer b) {
        return (hashCode() == b.hashCode()) &&
                (structure() == b.structure()) &&
                (volume() == b.volume()) &&
                (size() == b.size());
    }

    /** test for exhaustive equality */
    boolean equalTerms(TermContainer c);


//    static TermSet differ(TermSet a, TermSet b) {
//        if (a.size() == 1 && b.size() == 1) {
//            //special case
//            return a.term(0).equals(b.term(0)) ?
//                    TermIndex.EmptySet :
//                    a;
//        } else {
//            MutableSet dd = Sets.difference(a.toSet(), b.toSet());
//            if (dd.isEmpty()) return TermIndex.EmptySet;
//            return TermSet.the(dd);
//        }
//    }


    /** returns null if empty set; not sorted */
    @Nullable
    @Deprecated static Term difference(@NotNull Op op, @NotNull TermContainer a, @NotNull TermContainer b) {

        if (a.equals(b))
            return Terms.empty(op);

        Term[] aa = a.terms();

        List<Term> terms = Global.newArrayList(aa.length);

        for(Term x: aa) { //set difference
            if (!b.containsTerm(x))
                terms.add(x);
        }

        if (terms.isEmpty()) return Terms.empty(op);
        return $.the(op, terms).term();

//        if (a.size() == 1 && b.size() == 1) {
//            //special case
//            return a.term(0).equals(b.term(0)) ?
//                    Terms.Empty :
//                    a.terms();
//        } else {
//            MutableSet dd = Sets.difference(a.toSet(), b.toSet());
//            if (dd.isEmpty()) return Terms.Empty;
//            return Terms.toArray(dd);
//        }
    }



    void addAllTo(Collection<Term> set);



    /** expected to provide a non-copy reference to an internal array,
     *  if it exists. otherwise it should create such array.
     *  if this creates a new array, consider using .term(i) to access
     *  subterms iteratively.
     */
    @Deprecated @NotNull T[] terms();


    @NotNull
    default Term[] terms(@NotNull IntObjectPredicate<T> filter) {
        List<T> l = Global.newArrayList(size());
        int s = size();
        int added = 0;
        for (int i = 0; i < s; i++) {
            T t = term(i);
            if (filter.accept(i, t)) {
                l.add(t);
                added++;
            }
        }
        return added > 0 ? Terms.ZeroTermArray : l.toArray(new Term[added]);
    }


    void forEach(Consumer<? super T> action, int start, int stop);


    default void forEach(Consumer<? super T> action) {
        forEach(action, 0, size());
    }

    static void forEach(TermContainer c, Consumer action, int start, int stop) {
        for (int i = start; i < stop; i++) {
            action.accept(c.term(i));
        }
    }

//    static Term[] copyByIndex(TermContainer c) {
//        int s = c.size();
//        Term[] x = new Term[s];
//        for (int i = 0; i < s; i++) {
//            x[i] = c.term(i);
//        }
//        return x;
//    }


    static String toString(@NotNull TermContainer t) {
        StringBuilder sb = new StringBuilder("{[(");
        int s = t.size();
        for (int i = 0; i < s; i++) {
            sb.append(t.term(i));
            if (i < s-1)
                sb.append(", ");
        }
        sb.append(")]}");
        return sb.toString();

    }

    /** extract a sublist of terms as an array */
    @NotNull
    default Term[] terms(int start, int end) {
        //TODO for TermVector, create an Array copy directly
        //TODO for TermVector, if (start == 0) && end == just return its array

        Term[] t = new Term[end-start];
        int j = 0;
        for (int i = start; i < end; i++)
            t[j++] = term(i);
        return t;
    }

    /** follows normal indexOf() semantics; -1 if not found */
    default int indexOf(@NotNull Term t) {
        int s = size();
        if (impossibleSubterm(t))
            return -1;
        for (int i = 0; i < s; i++) {
            if (t.equals(term(i)))
                return i;
        }
        return -1;
    }




//    /** writes subterm bytes, including any attached metadata preceding or following it */
//    default void appendSubtermBytes(ByteBuf b) {
//
//        int n = size();
//
//        for (int i = 0; i < n; i++) {
//            Term t = term(i);
//
//            if (i != 0) {
//                b.add(ARGUMENT_SEPARATORbyte);
//            }
//
//            try {
//                byte[] bb = t.bytes();
//                if (bb.length!=t.bytesLength())
//                    System.err.println("wtf");
//                b.add(bb);
//            }
//            catch (ArrayIndexOutOfBoundsException a) {
//                System.err.println("Wtf");
//            }
//        }
//
//    }

//    @Override
//    default boolean containsTermRecursively(Term target) {
//        if (impossibleSubterm(target))
//            return false;
//
//        for (Term x : terms()) {
//            if (x.equals(target)) return true;
//            if (x instanceof Compound) {
//                if (x.containsTermRecursively(target)) {
//                    return true;
//                }
//            }
//        }
//        return false;
//
//    }

    default boolean equivalent(@NotNull List<Term> sub) {
        int s = size();
        if (s!=sub.size()) return false;
        for (int i = 0; i < s; i++) {
            if (!term(i).equals(sub.get(i))) return false;
        }
        return true;
    }


    /** returns true if evaluates true for any terms
     * @param p*/
    @Override
    default boolean or(@NotNull Predicate<? super Term> p) {
        for (Term t : terms()) {
            if (t.or(p))
                return true;
        }
        return false;
    }

    /** returns true if evaluates true for all terms
     * @param p*/
    @Override
    default boolean and(@NotNull Predicate<? super Term> p) {
        for (Term t : terms()) {
            if (!p.test(t)) {
                return false;
            }
        }
        return true;
    }



    /** produces the correct TermContainer for the given Op,
     * according to the existing type
     */
    @NotNull
    static TermContainer the(@NotNull Op op, @NotNull TermContainer tt) {
        return (!requiresTermSet(op, tt.size()) ||
                tt.isSorted()) ?
                    tt :
                    TermSet.the(tt.terms());
    }


    @NotNull
    static TermContainer the(@NotNull Term one) {
        return new TermVector(one);
    }


    @NotNull
    static TermContainer the(@NotNull Op op, @NotNull Collection<Term> tt) {
        //if (tt.isEmpty()) ...
        return requiresTermSet(op, tt.size()) ?
                TermSet.the(tt) :
                TermVector.the(tt.toArray(new Term[tt.size()]));
    }

    @NotNull
    static TermContainer the(@NotNull Op op, @NotNull Term... tt) {
        return  requiresTermSet(op, tt.length) ?
                TermSet.the(tt) :
                new TermVector(tt);
    }


    static boolean requiresTermSet(@NotNull Op op, int num) {
        return
            /*(dt==0 || dt==ITERNAL) &&*/ //non-zero or non-iternal dt disqualifies any reason for needing a TermSet
                ((num > 1) && op.isCommutative());
    }

    default boolean isSorted() {
        int s = size();
        if (s < 2) return true;

        for (int i = 1; i < s; i++) {
            if (term(i-1).compareTo(term(i))!=-1)
                return false;
        }
        return true;
    }

    @Override
    default int compareTo(@NotNull Object o) {
        return compareTo(this, o);
    }

    static int compareTo(TermContainer a, @NotNull Object b) {
        if (a == b) return 0;

        int diff;
        if ((diff = Integer.compare(a.hashCode(), b.hashCode())) != 0)
            return diff;

        TermContainer c = (TermContainer) b;
        int diff2;
        if ((diff2 = Integer.compare(a.structure(), c.structure())) != 0)
            return diff2;

        return compareContent(a, c);
    }


    static int compareContent(@NotNull TermContainer a, @NotNull TermContainer c) {

        int s = a.size(), diff;
        if ((diff = Integer.compare(s, c.size())) != 0)
            return diff;

        for (int i = 0; i < s; i++) {
            int d = a.term(i).compareTo(c.term(i));

            /*
            if (Global.DEBUG) {
                int d2 = b.compareTo(a);
                if (d2!=-d)
                    throw new RuntimeException("ordering inconsistency: " + a + ", " + b );
            }
            */

            if (d != 0) return d;
        }

        return 0;
    }


    @Nullable TermContainer replacing(int subterm, Term replacement);


    /** should be consistent with the other hash method(s) */
    static int hash(TermContainer<?> container) {
        int h = 1;
        for (Term t : container) {
            h = 31 /*Util.PRIME1 */ * h + t.hashCode();
        }
        return h;
    }

    /** computes the content hash while accumulating subterm metadata summary fields into int[] meta */
    static int hash(Term[] term, int[] meta) {
        int h = 1;
        for (Term t : term) {
            h = 31 /*Util.PRIME1 */ * h + t.init(meta);
        }
        return h;
    }

}
