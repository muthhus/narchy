package nars.term.container;

import com.gs.collections.api.block.predicate.primitive.IntObjectPredicate;
import com.gs.collections.api.set.ImmutableSet;
import com.gs.collections.api.set.MutableSet;
import com.gs.collections.impl.factory.Sets;
import nars.$;
import nars.Global;
import nars.Op;
import nars.term.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.gs.collections.impl.factory.Sets.immutable;
import static com.gs.collections.impl.factory.Sets.mutable;


/**
 * Methods common to both Term and Subterms
 * T = subterm type
 */
public interface TermContainer<T extends Term> extends Termlike, Iterable<T> {

    @NotNull
    static TermContainer union(@NotNull TermContainer a, @NotNull TermContainer b) {
        if (a.equals(b))
            return a;

        int as = a.size();
        int bs = b.size();
        int maxSize = Math.max(as, bs);
        TreeSet<Term> t = new TreeSet<>();
        a.copyInto(t);
        b.copyInto(t);
        if (t.size() == maxSize) {
            //the smaller is contained by the larger other
            return as > bs ? a : b;
        }

        return TermSet.the(t);
    }


    /**
     * gets subterm at index i
     */
    @Nullable T term(int i);

    /**
     * returns subterm automatically casted as compound (Use with caution)
     */
    @Nullable
    default public <C extends Compound> C cterm(int i) {
        return (C) term(i);
    }

    /**
     * tests if subterm i is op o
     */
    boolean isTerm(int i, @NotNull Op o);
    /*default boolean isTerm(int i, @NotNull Op o) {
        T ti = term(i);
        return (ti.op() == o);
    }*/

    @Override
    @Nullable
    default Term termOr(int i, @Nullable Term ifOutOfBounds) {
        return size() <= i ? ifOutOfBounds : term(i);
    }

    default @NotNull MutableSet<Term> toSet() {
        return mutable.of(terms());
    }

    default @NotNull ImmutableSet<Term> toSetImmutable() {
        return immutable.of(terms());
    }

    static @NotNull MutableSet<Term> intersect(@NotNull TermContainer a, @NotNull TermContainer b) {
        if ((a.structure() & b.structure()) == 0)
            return Sets.mutable.empty(); //nothing in common
        else
            return Sets.intersect(a.toSet(), b.toSet());
    }

//    Predicate2<Object, SetIterable> subtermIsCommon = (Object yy, SetIterable xx) -> {
//        return xx.contains(yy);
//    };
//    Predicate2<Object, SetIterable> nonVarSubtermIsCommon = (Object yy, SetIterable xx) -> {
//        return yy instanceof Variable ? false : xx.contains(yy);
//    };

    @NotNull
    static boolean commonSubterms(@NotNull Compound a, @NotNull Compound b) {
        return commonSubterms(a, b, false, new HashSet());
    }

    /**
     * recursively
     */
    @NotNull
    static boolean commonSubtermsRecurse(@NotNull Compound a, @NotNull Compound b, boolean excludeVariables, @NotNull HashSet<Term> scratch) {

        int commonStructure = a.structure() & b.structure();
        if (excludeVariables)
            commonStructure = commonStructure & ~(Op.VariableBits); //mask by variable bits since we do not want them

        if (commonStructure == 0)
            return false;

        scratch.clear();
        a.termsToSetRecurse(commonStructure, scratch, true);
        return b.termsToSetRecurse(commonStructure, scratch, false);
    }

    @NotNull
    static boolean commonSubterms(@NotNull Compound a, @NotNull Compound b, boolean excludeVariables, @NotNull HashSet<Term> scratch) {

        int commonStructure = a.structure() & b.structure();
        if (excludeVariables)
            commonStructure = commonStructure & ~(Op.VariableBits); //mask by variable bits since we do not want them

        if (commonStructure == 0)
            return false;

        scratch.clear();
        a.termsToSet(commonStructure, scratch, true);
        return b.termsToSet(commonStructure, scratch, false);

    }

    @Override
    default boolean hasTemporal() {
        return false;
    }

    /**
     * recursively
     */
    @NotNull
    static boolean commonSubtermOrContainment(@NotNull Term a, @NotNull Term b) {

        boolean aCompound = a instanceof Compound;
        boolean bCompound = b instanceof Compound;
        if (aCompound && bCompound) {
            return commonSubterms((Compound) a, ((Compound) b));
        } else {
            if (aCompound && !bCompound) {
                return ((Compound) a).containsTerm(b);
            } else if (bCompound && !aCompound) {
                return ((Compound) b).containsTerm(a);
            } else {
                //neither are compounds
                return a.equals(b);
            }
        }

    }


//    /**
//     * scans first level only, not recursive
//     */
//    default boolean contains(Object o) {
//        return o instanceof Term && containsTerm((Term) o);
//    }


//    static boolean equals(@NotNull TermContainer a, Object b) {
//        return b instanceof TermContainer && TermContainer.equals(a, (TermContainer)b);
//    }

    /**
     * should be called only from equals()
     */
    default boolean equalTo(@NotNull TermContainer b) {
        return (hashCode() == b.hashCode()) &&
                //(structure() == b.structure()) &&
                //(volume() == b.volume()) &&
                (size() == b.size()) &&
                (equalTerms(b));
    }


    /**
     * size should already be known equal
     */
    default boolean equalTerms(@NotNull TermContainer c) {
        int cl = size();
        for (int i = 0; i < cl; i++) {
            if (!term(i).equals(c.term(i)))
                return false;
        }
        return true;
    }


    /**
     * returns null if empty set
     */
    @Nullable
    static Compound difference(@NotNull Op op, @NotNull Compound a, @NotNull Compound b) {
        return difference($.terms, op, a, b);
    }

    @Nullable
    static Compound difference(@NotNull TermBuilder t, @NotNull Compound a, @NotNull TermContainer b) {
        return difference(t, a.op(), a, b);
    }

    @Nullable
    static Compound difference(@NotNull TermBuilder t, @NotNull Op o, @NotNull Compound a, @NotNull TermContainer b) {

        //intersect the mask
        if ((a.structure() & b.structure()) == 0)
            return null;

        Term[] aa = a.terms();

        List<Term> terms = Global.newArrayList(aa.length);

        int retained = 0, size = a.size();
        for (int i = 0; i < size; i++) {
            Term x = a.term(i);
            if (!b.containsTerm(x)) {
                terms.add(x);
                retained++;
            }
        }

        if (retained == size) { //same as 'a'
            return a;
        } else if (retained == 0) {
            return null;
        } else {
            return (Compound) t.build(o, terms.toArray(new Term[retained]));
        }

    }


    void copyInto(Collection<Term> target);


    /**
     * expected to provide a non-copy reference to an internal array,
     * if it exists. otherwise it should create such array.
     * if this creates a new array, consider using .term(i) to access
     * subterms iteratively.
     */
    @NotNull T[] terms();


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
        return added > 0 ? Terms.empty : l.toArray(new Term[added]);
    }

    /**
     * Check the subterms (first level only) for a target term
     *
     * @param t The term to be searched
     * @return Whether the target is in the current term
     */
    @Override
    default boolean containsTerm(@NotNull Termlike t) {
        if (!impossibleSubterm(t)) {
            int s = size();
            for (int i = 0; i < s; i++) {
                if (t.equals(term(i)))
                    return true;
            }
        }
        return false;
    }


    void forEach(Consumer<? super T> action, int start, int stop);


    @Override
    default void forEach(Consumer<? super T> action) {
        forEach(action, 0, size());
    }

    static void forEach(@NotNull TermContainer c, @NotNull Consumer action, int start, int stop) {
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
            if (i < s - 1)
                sb.append(", ");
        }
        sb.append(")]}");
        return sb.toString();

    }

    /**
     * extract a sublist of terms as an array
     */
    @NotNull
    default Term[] terms(int start, int end) {
        //TODO for TermVector, create an Array copy directly
        //TODO for TermVector, if (start == 0) && end == just return its array

        Term[] t = new Term[end - start];
        int j = 0;
        for (int i = start; i < end; i++)
            t[j++] = term(i);
        return t;
    }

    /**
     * follows normal indexOf() semantics; -1 if not found
     */
    default int indexOf(@NotNull Term t) {
        if (!impossibleSubterm(t)) {
            int s = size();
            for (int i = 0; i < s; i++) {
                if (t.equals(term(i)))
                    return i;
            }
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
        if (s == sub.size()) {
            for (int i = 0; i < s; i++) {
                if (!term(i).equals(sub.get(i)))
                    return false;
            }
            return true;
        }
        return false;
    }

    default boolean equivalent(@NotNull Term[] sub) {
        int s = size();
        if (s == sub.length) {
            for (int i = 0; i < s; i++) {
                if (!term(i).equals(sub[i]))
                    return false;
            }
            return true;
        }
        return false;
    }


    /**
     * returns true if evaluates true for any terms
     *
     * @param p
     */
    @Override
    default boolean or(@NotNull Predicate<Term> p) {
        int s = size();
        for (int i = 0; i < s; i++) {
            Term t = term(i);
            //if (t.or(p))
            if (p.test(t))
                return true;
        }
        return false;
    }

    /**
     * returns true if evaluates true for all terms
     *
     * @param p
     */
    @Override
    default boolean and(@NotNull Predicate<Term> p) {
        int s = size();
        for (int i = 0; i < s; i++) {
            Term t = term(i);
            if (!p.test(t)) {
                return false;
            }
        }
        return true;
    }


    /**
     * produces the correct TermContainer for the given Op,
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
        return TermVector.the(one);
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
        return requiresTermSet(op, tt.length) ?
                TermSet.the(tt) :
                TermVector.the(tt);
    }


    static boolean requiresTermSet(@NotNull Op op, int num) {
        return
            /*(dt==0 || dt==ITERNAL) &&*/ //non-zero or non-iternal dt disqualifies any reason for needing a TermSet
                ((num > 1) && op.commutative);
    }

    default boolean isSorted() {
        int s = size();
        if (s < 2) return true;

        for (int i = 1; i < s; i++) {
            if (term(i - 1).compareTo(term(i)) != -1)
                return false;
        }
        return true;
    }


//    default int compareTo(@NotNull Termlike o) {
//        return compareTo(this, o);
//    }

    static int compare(@NotNull TermContainer a, @NotNull Termlike b) {
        if (a == b) return 0;

        int s = a.size(), diff;
        if ((diff = Integer.compare(s, b.size())) != 0)
            return diff;

        TermContainer cc = (TermContainer) b;
        for (int i = 0; i < s; i++) {
            int d = a.term(i).compareTo(cc.term(i));

            /*
            if (Global.DEBUG) {
                int d2 = b.compareTo(a);
                if (d2!=-d)
                    throw new RuntimeException("ordering inconsistency: " + a + ", " + b );
            }
            */

            if (d != 0)
                return d;
        }

        return 0;
    }



    /**
     * a and b must be instances of input, and output must be of size input.length-2
     */
    @NotNull
    static Term[] except(@NotNull TermContainer input, Term a, Term b, @NotNull Term[] output) {
//        int targetLen = input.size() - 2;
//        if (output.length!= targetLen) {
//            throw new RuntimeException("wrong size");
//        }
        int j = 0;
        for (int i = 0; i < input.size(); i++) {
            Term x = input.term(i);
            if ((x != a) && (x != b))
                output[j++] = x;
        }

        if (j != output.length)
            throw new RuntimeException("permute underflow");

        return output;
    }

    /**
     * a must be in input, and output must be of size input.length-1
     */
    @NotNull
    static Term[] except(@NotNull Term[] input, Term a, @NotNull Term[] output) {
//        int targetLen = input.size() - 1;
//        if (output.length!= targetLen) {
//            throw new RuntimeException("wrong size");
//        }
        int j = 0;
        for (Term x : input) {
            if (x != a)
                output[j++] = x;
        }

        if (j != output.length)
            throw new RuntimeException("permute underflow");

        return output;
    }

    /**
     * for use with commutive (TermSet's)
     */
    @NotNull
    static TermContainer except(@NotNull TermContainer c, @NotNull MutableSet<Term> toRemove) {
        MutableSet<Term> s = c.toSet();
        if (s.removeAll(toRemove)) {
            return TermSet.the(s);
        }
        return c; //unchanged
    }

    @NotNull
    default TermContainer filter(Predicate<T> p) {
        if (!(this instanceof TermVector))
            throw new UnsupportedOperationException("only implemented for TermVector instance currently");

        return TermVector.the(
                Stream.of(terms()).filter(p).toArray(i -> new Term[i])
        );
    }

    int init(@NotNull int[] meta);

    default boolean hasAll(int equivalentSize, int structure, int volCached) {
        return (equivalentSize == size())
                &&
                hasAll(structure)
                &&
                (volCached <= volume());
    }

}
