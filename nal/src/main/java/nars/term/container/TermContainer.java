package nars.term.container;

import nars.$;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.Terms;
import nars.term.atom.Atomic;
import nars.term.var.Variable;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.index.TermBuilder.commutive;
import static org.eclipse.collections.impl.factory.Sets.immutable;
import static org.eclipse.collections.impl.factory.Sets.mutable;


/**
 * Methods common to both Term and Subterms
 * T = subterm type
 */
public interface TermContainer extends Termlike, Iterable<Term> {




    @NotNull
    default public TermContainer append(@NotNull Term x) {
        return TermVector.the(ArrayUtils.add(terms(),x));
    }

    //TODO optionally allow atomic structure positions to differ
    default boolean equivalentStructures() {
        int t0Struct = term(0).structure();
        for (int i = 1; i < size(); i++) {
            if (term(i).structure()!=t0Struct)
                return false;
        }

        ByteList structureKey = term(0).structureKey();
        {
            ByteArrayList reuseKey = new ByteArrayList(structureKey.size());
            for (int i = 1; i < size(); i++) {
                //all subterms must share the same structure
                //TODO only needs to construct the key while comparing equality with the first
                if (!term(i).structureKey(reuseKey).equals(structureKey))
                    return false;
                reuseKey.clear();
            }
        }
        return true;
    }


    /**
     * gets subterm at index i
     */
    @NotNull Term term(int i);


    @NotNull default Compound compound(int i) {
        return ((Compound)term(i));
    }


    /**
     * returns subterm automatically casted as compound (Use with caution)
     */
    @Nullable
    default public <C extends Compound> C cterm(int i) {
        return (C) term(i);
    }


    @Override
    @Nullable
    default Term termOr(int i, @Nullable Term ifOutOfBounds) {
        return size() <= i ? ifOutOfBounds : term(i);
    }

    default @NotNull Set<Term> toSet() {
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
        return commonSubterms(a, b, false);
    }

    /**
     * recursively
     */
    @NotNull
    static boolean commonSubtermsRecurse(@NotNull Compound a, @NotNull Compound b, boolean excludeVariables) {

        int commonStructure = a.structure() & b.structure();
        if (excludeVariables)
            commonStructure = commonStructure & ~(Op.VariableBits); //mask by variable bits since we do not want them

        if (commonStructure == 0)
            return false;

        Set<Term> scratch = new HashSet(a.size());
        a.termsToSetRecurse(commonStructure, scratch, true);
        return b.termsToSetRecurse(commonStructure, scratch, false);
    }

    @NotNull
    static boolean subtermOfTheOther(@NotNull Compound a, @NotNull Compound b, boolean recurse, boolean excludeVariables) {

        if ((excludeVariables) && (a instanceof Variable || b instanceof Variable))
            return false;

        if (recurse)
            return a.containsTermRecursively(b) || b.containsTermRecursively(a);
        else
            return a.containsTerm(b) || b.containsTerm(a);
    }


    /**
     * tests if subterm i is op o
     */
    default boolean isTerm(int i, @NotNull Op o) {
        return term(i).op() == o;
    }

    /**
     * Check the subterms (first level only) for a target term
     *
     * @param t The term to be searched
     * @return Whether the target is in the current term
     */
    @Override
    default boolean containsTerm(@NotNull Termlike t) {
        return !impossibleSubTerm(t) && or(t::equals);
    }

    default boolean containsTermRecursively(@NotNull Term b) {
        if (!impossibleSubTerm(b)) {
            int s = size();
            for (int i = 0; i < s; i++) {
                Term x = term(i);
                if (x.equals(b) || ((x instanceof Compound) && (((Compound) x).subterms().containsTermRecursively(b)))) {
                    return true;
                }
            }
        }
        return false;
    }

    default boolean containsTermAtemporally(@NotNull Term b) {
        b = b.unneg();
        if (!impossibleSubTerm(b)) {
            int s = size();
            for (int i = 0; i < s; i++) {
                if (Terms.equalAtemporally(term(i),b)) {
                    return true;
                }
            }
        }
        return false;
    }

//    default boolean containsTermRecursivelyAtemporally(@NotNull Term b) {
//        b = b.unneg();
//        if (!impossibleSubTerm(b)) {
//            int s = size();
//            for (int i = 0; i < s; i++) {
//                Term x = term(i);
//                if (Terms.equalAtemporally(x,b) || ((x instanceof Compound) && (((Compound) x).containsTermRecursivelyAtemporally(b)))) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }

    @NotNull
    static boolean commonSubterms(@NotNull Compound a, @NotNull Compound b, boolean excludeVariables) {

        int commonStructure = a.structure() & b.structure();
        if (excludeVariables)
            commonStructure = commonStructure & ~(Op.VariableBits); //mask by variable bits since we do not want them

        if (commonStructure == 0)
            return false;

        Set<Term> scratch = new HashSet(a.size()  );
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
                (equalTerms(b));
    }


    /**
     * size should already be known equal
     */
    default boolean equalTerms(@NotNull TermContainer c) {
        int s = size();
        if (s !=c.size())
            return false;
        for (int i = 0; i < s; i++) {
            if (!term(i).equals(c.term(i)))
                return false;
        }
        return true;
    }

    default boolean equalTerms(@NotNull List<Term> c) {
        int s = size();
        if (s !=c.size())
            return false;
        for (int i = 0; i < s; i++) {
            if (!term(i).equals(c.get(i)))
                return false;
        }
        return true;
    }
    default boolean equalTerms(@NotNull Term... c) {
        int s = size();
        if (s !=c.length)
            return false;
        for (int i = 0; i < s; i++) {
            if (!term(i).equals(c[i]))
                return false;
        }
        return true;
    }

    void copyInto(Collection<Term> target);


    /**
     * expected to provide a non-copy reference to an internal array,
     * if it exists. otherwise it should create such array.
     * if this creates a new array, consider using .term(i) to access
     * subterms iteratively.
     */
    @NotNull Term[] terms();


    @NotNull
    default Term[] terms(@NotNull IntObjectPredicate<Term> filter) {
        List<Term> l = $.newArrayList(size());
        int s = size();
        int added = 0;
        for (int i = 0; i < s; i++) {
            Term t = term(i);
            if (filter.accept(i, t)) {
                l.add(t);
                added++;
            }
        }
        return added > 0 ? Terms.empty : l.toArray(new Term[added]);
    }




    void forEach(Consumer<? super Term> action, int start, int stop);


    default void forEachAtomic(@NotNull Consumer<? super Atomic> action) {
        forEach(x -> {
            if (x instanceof Atomic)
                action.accept((Atomic)x);
        });
    }

    default void forEachCompound(@NotNull Consumer<? super Compound> action) {
        forEach(x -> {
            if (x instanceof Compound)
                action.accept((Compound)x);
        });
    }

    @Override
    default void forEach(Consumer<? super Term> action) {
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
        if (!impossibleSubTerm(t)) {
            int s = size();
            for (int i = 0; i < s; i++) {
                if (t.equals(term(i)))
                    return i;
            }
        }
        return -1;
    }
    default int indexOfAtemporally(Term t) {
        t = t.unneg(); //unneg before testing impossible
        if (!impossibleSubTerm(t)) {
            Term at = $.terms.atemporalize(t);
            int s = size();
            for (int i = 0; i < s; i++) {
                if (Terms.equalAtemporally(at, term(i)))
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

//    default boolean equivalent(@NotNull List<Term> sub) {
//        int s = size();
//        if (s == sub.size()) {
//            for (int i = 0; i < s; i++) {
//                if (!term(i).equals(sub.get(i)))
//                    return false;
//            }
//            return true;
//        }
//        return false;
//    }
//
//    default boolean equivalent(@NotNull Term[] sub) {
//        int s = size();
//        if (s == sub.length) {
//            for (int i = 0; i < s; i++) {
//                if (!term(i).equals(sub[i]))
//                    return false;
//            }
//            return true;
//        }
//        return false;
//    }
    default boolean equivalent(@NotNull TermContainer sub) {
        if (hashCodeSubTerms() == sub.hashCodeSubTerms()) {
            int s = size();
            if (s == sub.size()) {
                for (int i = 0; i < s; i++) {
                    if (!term(i).equals(sub.term(i)))
                        return false;
                }
                return true;
            }
        }
        return false;
    }

    /** allows the subterms to hold a different hashcode than hashCode when comparing subterms */
    default int hashCodeSubTerms() {
        return hashCode();
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

    default int count(@NotNull Predicate<Term> match) {
        int s = size();
        int count = 0;
        for (int i = 0; i < s; i++) {
            Term t = term(i);
            if (match.test(t)) {
                count++;
            }
        }
        return count;
    }

    /** note: if the function returns null, null will not be added to the result set */
    @NotNull
    default Set<Term> unique(@NotNull Function<Term, Term> each) {
        Set<Term> r = new HashSet(size());
        int s = size();
        for (int i = 0; i < s; i++) {
            Term e = each.apply(term(i));
            if (e!=null)
                r.add(e);
        }
        return r;
    }




    @NotNull
    static TermContainer the(@NotNull Op op, int dt, @NotNull Term... tt) {
        return TermVector.the(theTermArray(op, dt, tt));
    }

    @NotNull
    static Term[] theTermArray(@NotNull Op op, int dt, @NotNull Term... tt) {
        return mustSortAndUniquify(op, dt, tt.length) ?
                Terms.sorted(tt) :
                tt;
    }

    /** non-zero or non-iternal dt disqualifies any reason for needing a TermSet */
    public static boolean mustSortAndUniquify(@NotNull Op op, int dt, int num) {
        return num > 1 && op.commutative && commutive(dt);
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

    static int compare(@NotNull TermContainer A, @NotNull Termlike b) {
        if (A == b) return 0;

        int diff;

        if ((diff = (A.structure() - b.structure())) != 0)
            return diff;

        if ((diff = (A.volume() - b.volume())) != 0)
            return diff;

        int s;
        if ((diff = ((s = A.size()) - b.size())) != 0)
            return diff;

//        if ((diff = ( A.vars() - b.vars())) != 0)
//            return diff;

        TermContainer B = (TermContainer) b;


        int inequalVariable = -1; //only need to compare the first non-equal variable term
        for (int i = 0; i < s; i++) {
            Term x = A.term(i);
            Term y = B.term(i);
            if (x instanceof Variable && y instanceof Variable) {
                if (inequalVariable==-1 && !x.equals(y))
                    inequalVariable = i; //test below; allow differing non-variable terms to determine sort order first

            } else {
                int d = x.compareTo(y);
                if (d != 0) {
                    return d;
                }
            }
        }

        //2nd-stage:
        if (inequalVariable!=-1) {
            return A.term(inequalVariable).compareTo(B.term(inequalVariable));
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


    @NotNull
    static Set<Term> exceptToSet(@NotNull TermContainer c, @NotNull MutableSet<Term> toRemove) {

        int cs = c.size();
        Set<Term> s = new HashSet(cs);
        for (int i = 0; i < cs; i++) {
            Term x = c.term(i);
            if (!toRemove.contains(x))
                s.add(x);
        }
        return s;
    }

    @NotNull default TermContainer filter(Predicate<Term> p) {
        if (!(this instanceof TermContainer))
            throw new UnsupportedOperationException("only implemented for TermVector instance currently");

        return TermVector.the(
                Stream.of(terms()).filter(p).toArray(i -> new Term[i])
        );
    }


    default boolean hasAll(int equivalentSize, int structure, int volCached) {
        return (equivalentSize == size())
                &&
                hasAll(structure)
                &&
                (volCached <= volume());
    }


}
