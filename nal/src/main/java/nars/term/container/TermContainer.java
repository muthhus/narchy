package nars.term.container;

import nars.$;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.Terms;
import nars.term.atom.Atomic;
import nars.term.mutate.CommutivePermutations;
import nars.term.subst.Unify;
import nars.term.var.Variable;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static nars.Op.*;


/**
 * Methods common to both Term and Subterms
 * T = subterm type
 */
public interface TermContainer extends Termlike, Iterable<Term> {

    @NotNull
    @Deprecated
    default public TermContainer append(@NotNull Term x) {
        return Op.subterms(ArrayUtils.add(toArray(), x));
    }

    //TODO optionally allow atomic structure positions to differ
    default boolean equivalentStructures() {
        int t0Struct = sub(0).structure();
        int s = size();
        for (int i = 1; i < s; i++) {
            if (sub(i).structure() != t0Struct)
                return false;
        }

        ByteList structureKey = sub(0).structureKey();
        {
            ByteArrayList reuseKey = new ByteArrayList(structureKey.size());
            for (int i = 1; i < s; i++) {
                //all subterms must share the same structure
                //TODO only needs to construct the key while comparing equality with the first
                if (!sub(i).structureKey(reuseKey).equals(structureKey))
                    return false;
                reuseKey.clear();
            }
        }
        return true;
    }


    /**
     * a termcontainer is not necessarily a term of its own
     */
    @NotNull
    @Override
    default Term term() {
        throw new UnsupportedOperationException();
    }

    /**
     * gets subterm at index i
     */
    @NotNull Term sub(int i);


    @NotNull
    default Compound compound(int i) {
        return ((Compound) sub(i));
    }

    @NotNull
    @Override
    default Iterator<Term> iterator() {
        throw new UnsupportedOperationException();
    }

    /**
     * returns subterm automatically casted as compound (Use with caution)
     */
    @Nullable
    default public <C extends Compound> C cterm(int i) {
        return (C) sub(i);
    }


    @Override
    @Nullable
    default Term sub(int i, @Nullable Term ifOutOfBounds) {
        return size() <= i ? ifOutOfBounds : sub(i);
    }

    @Nullable
    default boolean subEquals(int i, @NotNull Term x) {
        return size() <= i ? false : sub(i).equals(x);
    }

    @Override
    default boolean isDynamic() {
        return
                hasAll(EvalBits) &&
                        ((op() == INH && subIs(0, PROD) && subIs(1, ATOM)) /* potential function */
                                ||
                                (OR(Termlike::isDynamic))); /* possible function in subterms */
    }

    @Override
    default int subCount(Op o) {
        if (!hasAll(o.bit))
            return 0; //structure doesnt contain that op

        switch (o) {
            case VAR_DEP:
                return varDep();
            case VAR_INDEP:
                return varIndep();
            case VAR_QUERY:
                return varQuery();
            case VAR_PATTERN:
                return varPattern();
        }
        return intValue(0, (sum, x) -> {
            return (x.op() == o) ? (sum + 1) : sum;
        });
    }

    /**
     * int reduction operation
     */
    default int intValue(int x, IntObjectToIntFunction<Term> reduce) {
        int l = size();
        for (int t = 0; t < l; t++)
            x = reduce.intValueOf(x, sub(t));
        return x;
    }

    default @NotNull TreeSet<Term> toSortedSet() {
        int s = size();
        TreeSet u = new TreeSet();
        forEach(u::add);
        return u;
    }

    /**
     * @return a Mutable Set, unless empty
     */
    default @NotNull Set<Term> toSet() {
        int s = size();
        if (s > 0) {
            UnifiedSet u = new UnifiedSet(s);
            forEach(u::add);
            return u;
        } else {
            return Collections.emptySet();
        }
//        return new DirectArrayUnenforcedSet<Term>(Terms.sorted(toArray())) {
//            @Override
//            public boolean removeIf(Predicate<? super Term> filter) {
//
//                return false;
//            }
//        };
    }

    default @NotNull Set<Term> toSet(Predicate<Term> ifTrue) {
        int s = size();
        if (s > 0) {
            UnifiedSet u = new UnifiedSet(s);
            forEach(x -> {
                if (ifTrue.test(x)) u.add(x);
            });
            return u;
        } else {
            return Collections.emptySet();
        }
//        return new DirectArrayUnenforcedSet<Term>(Terms.sorted(toArray())) {
//            @Override
//            public boolean removeIf(Predicate<? super Term> filter) {
//
//                return false;
//            }
//        };
    }

    /**
     * returns sorted ready for commutive
     */
    static @Nullable Term[] intersect(@NotNull TermContainer a, @NotNull TermContainer b) {
        if ((a.structure() & b.structure()) == 0)
            return null; //nothing in common
        else {
            MutableSet<Term> si = Sets.intersect(a.toSet(), b.toSet());
            int ssi = si.size();
            if (ssi == 0)
                return null;

            Term[] c = si.toArray(new Term[si.size()]);
            if (ssi > 1)
                Arrays.sort(c);

            return c;
        }
    }

//    Predicate2<Object, SetIterable> subtermIsCommon = (Object yy, SetIterable xx) -> {
//        return xx.contains(yy);
//    };
//    Predicate2<Object, SetIterable> nonVarSubtermIsCommon = (Object yy, SetIterable xx) -> {
//        return yy instanceof Variable ? false : xx.contains(yy);
//    };

    /**
     * recursively
     */
    @NotNull
    static boolean hasCommonSubtermsRecursive(@NotNull Compound a, @NotNull Compound b, boolean excludeVariables) {

        int commonStructure = a.structure() & b.structure();
        if (excludeVariables)
            commonStructure = commonStructure & ~(Op.VariableBits); //mask by variable bits since we do not want them

        if (commonStructure == 0)
            return false;

        Set<Term> scratch = new HashSet(/*a.size() + b.size()*/);
        a.termsToSetRecurse(commonStructure, scratch, true);
        return b.termsToSetRecurse(commonStructure, scratch, false);
    }

    @NotNull
    public static boolean isSubtermOfTheOther(@NotNull Term a, @NotNull Term b, boolean recurse, boolean excludeVariables) {

        if ((excludeVariables) && (a instanceof Variable || b instanceof Variable))
            return false;

        return recurse ?
                a.containsRecursively(b) || b.containsRecursively(a) :
                a.contains(b) || b.contains(a);
    }


    /**
     * Check the subterms (first level only) for a target term
     *
     * @param t The term to be searched
     * @return Whether the target is in the current term
     */
    @Override
    default boolean contains(@NotNull Termlike t) {
        return !impossibleSubTerm(t) && OR(t::equals);
    }

    default boolean containsRecursively(@NotNull Term y) {
        if (!impossibleSubTerm(y)) {
            int s = size();
            for (int i = 0; i < s; i++) {
                Term x = sub(i);
                if (x.equals(y) || ((x instanceof Compound) && (((Compound) x).containsRecursively(y)))) {
                    return true;
                }
            }
        }
        return false;
    }

//    default boolean containsTermAtemporally(@NotNull Term b) {
//        b = b.unneg();
//        if (!impossibleSubTerm(b)) {
//            int s = size();
//            for (int i = 0; i < s; i++) {
//                if (Terms.equalAtemporally(term(i),b)) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }

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

        Set<Term> scratch = new HashSet(a.size());
        a.termsToSet(commonStructure, scratch, true);
        return b.termsToSet(commonStructure, scratch, false);

    }


    /**
     * recursively
     */
    @NotNull
    static boolean commonSubtermOrContainment(@NotNull Term a, @NotNull Term b) {

        boolean aCompound = a instanceof Compound;
        boolean bCompound = b instanceof Compound;
        if (aCompound && bCompound) {
            return commonSubterms((Compound) a, ((Compound) b), false);
        } else {
            if (aCompound && !bCompound) {
                return ((Compound) a).contains(b);
            } else if (bCompound && !aCompound) {
                return ((Compound) b).contains(a);
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


//    boolean equalTerms(@NotNull TermContainer c);
//    default boolean equalTerms(@NotNull TermContainer c) {
//        int s = size();
//        if (s !=c.size())
//            return false;
//        for (int i = 0; i < s; i++) {
//            if (!sub(i).equals(c.sub(i))) {
//                sub(i).equals(c.sub(i));
//                return false;
//            }
//        }
//        return true;
//    }

    //    default boolean equalTerms(@NotNull List<Term> c) {
//        int s = size();
//        if (s !=c.size())
//            return false;
//        for (int i = 0; i < s; i++) {
//            if (!sub(i).equals(c.get(i)))
//                return false;
//        }
//        return true;
//    }
    default boolean equalTerms(@NotNull Term[] c) {
        int s = size();
        if (s != c.length)
            return false;
        for (int i = 0; i < s; i++) {
            if (!sub(i).equals(c[i]))
                return false;
        }
        return true;
    }

    default void copyInto(Collection<Term> target) {
        forEach(target::add);
    }


    default public Term[] toArray() {
        int s = size();
        switch (s) {
            case 0:
                return Term.EmptyArray;
            case 1:
                return new Term[]{sub(0)};
            case 2:
                return new Term[]{sub(0), sub(1)};
            default:
                return toArray(new Term[s], 0, s);
        }
    }

    default Term[] toArray(Term[] x, int from, int to) {
//        if (s == 0)
//            return Term.EmptyArray;
//
//        if (x == null || x.length!=s)
//            x = new Term[s];

        for (int i = from, j = 0; i < to; i++, j++)
            x[j] = this.sub(i);

        return x;
    }

    @NotNull
    default Term[] terms(@NotNull IntObjectPredicate<Term> filter) {
        List<Term> l = $.newArrayList(size());
        int s = size();
        int added = 0;
        for (int i = 0; i < s; i++) {
            Term t = sub(i);
            if (filter.accept(i, t)) {
                l.add(t);
                added++;
            }
        }
        return added > 0 ? Term.EmptyArray : l.toArray(new Term[added]);
    }


    default void forEach(Consumer<? super Term> action, int start, int stop) {
        for (int i = start; i < stop; i++)
            action.accept(sub(i));
    }


    default void forEachAtomic(@NotNull Consumer<? super Atomic> action) {
        forEach(x -> {
            if (x instanceof Atomic)
                action.accept((Atomic) x);
        });
    }

    default void forEachCompound(@NotNull Consumer<? super Compound> action) {
        forEach(x -> {
            if (x instanceof Compound)
                action.accept((Compound) x);
        });
    }

    @Override
    default void forEach(Consumer<? super Term> action) {
        forEach(action, 0, size());
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
            sb.append(t.sub(i));
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
            t[j++] = sub(i);
        return t;
    }

    /**
     * follows normal indexOf() semantics; -1 if not found
     */
    default int indexOf(@NotNull Term t) {
        if (!impossibleSubTerm(t)) {
            int s = size();
            for (int i = 0; i < s; i++) {
                if (t.equals(sub(i)))
                    return i;
            }
        }
        return -1;
    }
//    default int indexOfAtemporally(Term t) {
//        t = t.unneg(); //unneg before testing impossible
//        if (!impossibleSubTerm(t)) {
//            Term at = $.terms.atemporalize(t);
//            int s = size();
//            for (int i = 0; i < s; i++) {
//                if (Terms.equalAtemporally(at, term(i)))
//                    return i;
//            }
//        }
//        return -1;
//    }


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


    /**
     * allows the subterms to hold a different hashcode than hashCode when comparing subterms
     */
    default int hashCodeSubTerms() {
        return hashCode();
    }

    /**
     * returns true if evaluates true for any terms
     *
     * @param p
     */
    @Override
    default boolean OR(@NotNull Predicate<Term> p) {
        int s = size();
        for (int i = 0; i < s; i++) {
            if (p.test(sub(i)))
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
    default boolean AND(@NotNull Predicate<Term> p) {
        int s = size();
        for (int i = 0; i < s; i++)
            if (!p.test(sub(i)))
                return false;
        return true;
    }

    default boolean ANDrecurse(@NotNull Predicate<Term> p) {
        int s = size();
        for (int i = 0; i < s; i++)
            if (!sub(i).ANDrecurse(p))
                return false;
        return true;
    }

    default boolean ORrecurse(@NotNull Predicate<Term> p) {
        int s = size();
        for (int i = 0; i < s; i++)
            if (sub(i).ORrecurse(p))
                return true;
        return false;
    }

    default int count(@NotNull Predicate<Term> match) {
        int s = size();
        int count = 0;
        for (int i = 0; i < s; i++) {
            if (match.test(sub(i))) {
                count++;
            }
        }
        return count;
    }

    /**
     * note: if the function returns null, null will not be added to the result set
     */
    @NotNull
    default Set<Term> unique(@NotNull Function<Term, Term> each) {
        Set<Term> r = new HashSet(size());
        int s = size();
        for (int i = 0; i < s; i++) {
            Term e = each.apply(sub(i));
            if (e != null)
                r.add(e);
        }
        return r;
    }


    @NotNull
    static TermContainer the(@NotNull Op op, int dt, @NotNull Term... tt) {
        return Op.subterms(theTermArray(op, dt, tt));
    }

    @NotNull
    static Term[] theTermArray(@NotNull Op op, int dt, @NotNull Term... tt) {
        return mustSortAndUniquify(op, dt, tt.length) ?
                Terms.sorted(tt) :
                tt;
    }

    /**
     * non-zero or non-iternal dt disqualifies any reason for needing a TermSet
     */
    public static boolean mustSortAndUniquify(@NotNull Op op, int dt, int num) {
        return num > 1 && op.commutative && (concurrent(dt));
    }

    @Override
    default boolean isTemporal() {
        return OR(Term::isTemporal);
    }


    default boolean isSorted() {
        int s = size();
        if (s < 2) return true;

        for (int i = 1; i < s; i++) {
            if (sub(i - 1).compareTo(sub(i)) != -1)
                return false;
        }
        return true;
    }


//    default int compareTo(@NotNull Termlike o) {
//        return compareTo(this, o);
//    }

    static int compare(@NotNull TermContainer a, @NotNull Termlike b) {

        int diff;

        int s;
        if ((diff = Integer.compare((s = a.size()), b.size())) != 0)
            return diff;

//        if ((diff = (a.hashCode() - b.hashCode())) != 0)
//            return diff;

        if ((diff = Integer.compare(a.structure(), b.structure())) != 0)
            return diff;

        if ((diff = Integer.compare(a.volume(), b.volume())) != 0)
            return diff;

        TermContainer B = (TermContainer) b;

        int inequalVariable = -1; //only need to compare the first non-equal variable term
        for (int i = 0; i < s; i++) {
            Term x = a.sub(i);
            Term y = B.sub(i);
            if (x instanceof Variable && y instanceof Variable) {
                if (inequalVariable == -1 && !x.equals(y))
                    inequalVariable = i; //test below; allow differing non-variable terms to determine sort order first

            } else {
                int d = x.compareTo(y);
                if (d != 0) {
                    return d;
                }
            }
        }

        //2nd-stage:
        if (inequalVariable != -1) {
            return a.sub(inequalVariable).compareTo(B.sub(inequalVariable));
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
        int l = input.size();
        for (int i = 0; i < l; i++) {
            Term x = input.sub(i);
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
    static Set<Term> toSetExcept(@NotNull TermContainer c, @NotNull MutableSet<Term> except) {

//        return c.value(null, (x, s) -> {
//
//        });

        int cs = c.size();
        Set<Term> s = null;
        for (int i = 0; i < cs; i++) {
            Term x = c.sub(i);
            if (!except.contains(x)) {
                if (s == null) s = new UnifiedSet(cs - i /* possible remaining items that could be added*/);
                s.add(x);
            }
        }
        return s == null ? Collections.emptySet() : s;
    }

    /**
     * constructs a new container with the matching elements missing
     * TODO elide creating a new vector if nothing would change
     */
    @NotNull
    default TermContainer asFiltered(Predicate<Term> p) {
        if (!(this instanceof TermContainer))
            throw new UnsupportedOperationException("only implemented for TermVector instance currently");

        List<Term> c = $.newArrayList(size());
        if (OR(x -> p.test(x) && c.add(x)))
            return TermVector.the(c);
        else
            return Terms.ZeroProduct;
    }

    /**
     * stream of each subterm
     */
    default Stream<Term> subStream() {
        return IntStream.range(0, size()).mapToObj(this::sub);
    }

    //TODO
//    default Stream<? extends Term> streamRecursive() {
//        return IntStream.range(0, size()).
//                mapToObj(x -> {
//                    Term s = sub(x);
//                    if (s instanceof TermContainer)
//                        return ((TermContainer) s).stream();
//                    else
//                        return Stream.empty();
//                }).collect(Collectors.);
//    }


    default boolean unifyLinear(TermContainer Y, @NotNull Unify u) {
        /**
         * a branch for comparing a particular permutation, called from the main next()
         */
        int s = size();
        switch (s) {
            case 0:
                return true; //shouldnt ever happen

            case 1:
                return u.unify(sub(0), Y.sub(0));

//                case 2: {
//
//                    int i = u.random.nextInt(1);
//                    if (u.unify(sub(i), Y.sub(i))) {
//                        i = 1 - i;
//                        return u.unify(sub(i), Y.sub(i));
//                    } else {
//                        return false;
//                    }
//                }

            default: {
                //TODO unify variables last after matching all constants by saving them to a secondary list as they are encountered in the below loop

                //begin at random offset to shuffle the order of the match sequence
                int j = u.random.nextInt(s);
                for (int i = 0; i < s; i++) {
                    if (!u.unify(sub(j), Y.sub(j)))
                        return false;
                    if (++j == s)
                        j = 0;
                }
                return true;
            }
        }

    }

    default boolean unifyCommute(TermContainer y, @NotNull Unify subst) {
        //if there are no variables of the matching type, then it seems CommutivePermutations wouldnt match anyway
        if (unifyPossible(subst.type)) {

            Set<Term> xs = /*toSorted*/toSet();
            Set<Term> ys = y./*toSorted*/toSet();
            forEach(s -> {
                if (!subst.matchType(s)) {
                    ys.remove(s);
                    xs.remove(s);
                }
            });

            //special case
            {
                //ex: {x,%1} vs. {x,z} --- there is actually no combination here
                //Predicate<Term> notType = (x) -> !subst.matchType(x);
                if (xs.size() == 1 && ys.size() == 1) {
                    return subst.unify(xs.iterator().next(), ys.iterator().next());
                }
            }

            //subst.termutes.add(new CommutivePermutations(TermVector.the(xs), TermVector.the(ys)));
            subst.termutes.add(new CommutivePermutations(this, y));
            return true;
        }
        return false;
    }


    default boolean recurseSubTerms(BiPredicate<Term, Compound> whileTrue, Compound parent) {
        int s = size();
        for (int i = 0; i < s; i++) {
            if (!sub(i).recurseTerms(whileTrue, parent))
                return false;
        }
        return true;
    }

    /**
     * match a range of subterms of Y.
     */
    @NotNull
    default public Term[] toArraySubRange(int from, int to) {
        if (from == 0 && to == size()) {
            return toArray();
        } else {

            int s = to - from;

            Term[] l = new Term[to - from];

            int x = 0, y = from;
            for (int i = 0; i < s; i++) {
                l[x++] = sub(y++);
            }

            return l;
        }
    }

    @Override
    default void recurseTerms(@NotNull Consumer<Term> v) {
        forEach(s -> s.recurseTerms(v));
    }

    /**
     * returns a sorted and de-duplicated version of this container
     */
    default TermContainer sorted() {
        int s = size();
        if (s <= 1)
            return this;


        Term[] tt = Terms.sorted(toArray());
        if (equalTerms(tt))
            return this;
        else
            return Op.subterms(tt);
    }

}
