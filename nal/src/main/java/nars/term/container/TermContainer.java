package nars.term.container;

import nars.$;
import nars.Op;
import nars.derive.mutate.CommutivePermutations;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.Terms;
import nars.term.subst.Unify;
import nars.term.var.Variable;
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

import static nars.Op.ZeroProduct;
import static nars.Op.concurrent;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;


/**
 * Methods common to both Term and Subterms
 * T = subterm type
 */
public interface TermContainer extends Termlike, Iterable<Term> {

    @NotNull TermVector NoSubterms = new ArrayTermVector((Term[]) new Term[]{});


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




    @Override
    default Term sub(int i, Term ifOutOfBounds) {
        return size() <= i ? ifOutOfBounds : sub(i);
    }

    @Nullable
    default boolean subEquals(int i, @NotNull Term x) {
        return size() > i && sub(i).equals(x);
    }


//    @Override
//    default int subCount(Op o) {
//        if (!hasAll(o.bit))
//            return 0; //structure doesnt contain that op
//
//        switch (o) {
//            case VAR_DEP:
//                return varDep();
//            case VAR_INDEP:
//                return varIndep();
//            case VAR_QUERY:
//                return varQuery();
//            case VAR_PATTERN:
//                return varPattern();
//        }
//        return intValue(0, (sum, x) -> {
//            return (x.op() == o) ? (sum + 1) : sum;
//        });
//    }

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
            return new UnifiedSet(0);
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
            for (int i = 0; i < s; i++) {
                @NotNull Term x = sub(i);
                if (ifTrue.test(x))
                    u.add(x);
            }
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
            //TODO sort a and b so that less comparisons are made (ie. if b is smaller than a, compute a.toSet() first
            Set<Term> as = a.toSet();
            Set<Term> si = b.toSet(as::contains); //(MutableSet<Term>) as Sets.intersect(a.toSet(), b.toSet());
            int ssi = si.size();
            if (ssi == 0)
                return null;

            Term[] c = si.toArray(new Term[ssi]);
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
    static boolean hasCommonSubtermsRecursive(@NotNull Term a, @NotNull Term b, boolean excludeVariables) {

        int commonStructure = a.structure() & b.structure();
        if (excludeVariables)
            commonStructure = commonStructure & ~(Op.VariableBits); //mask by variable bits since we do not want them

        if (commonStructure == 0)
            return false;

        Set<Term> scratch = new HashSet(/*a.size() + b.size()*/);
        a.subterms().recurseTermsToSet(commonStructure, scratch, true);
        return b.subterms().recurseTermsToSet(commonStructure, scratch, false);
    }

    @NotNull default Set<Term> recurseTermsToSet() {
        return recurseTermsToSet(null);
    }

    /**
     * gets the set of unique recursively contained terms of a specific type
     * TODO generalize to a provided lambda predicate selector
     */
    @NotNull
    default Set<Term> recurseTermsToSet(@Nullable Op onlyType) {
        if (onlyType!=null && !hasAny(onlyType))
            return Sets.mutable.empty();

        Set<Term> t = new HashSet(volume());//$.newHashSet(volume() /* estimate */);

        //TODO use an additional predicate to cull subterms which don't contain the target type
        recurseTerms((t1) -> {
            if (onlyType == null || t1.op() == onlyType) //TODO make recurseTerms by Op then it can navigate to subterms using structure hash
                t.add(t1);
        });
        return t;
    }


    /**
     * returns whether the set operation caused a change or not
     */
    @NotNull
    default boolean termsToSet(int inStructure, @NotNull Collection<Term> t, boolean addOrRemoved) {
        boolean r = false;

        TermContainer tt = subterms();
        int l = tt.size();
        for (int i = 0; i < l; i++) {
            @NotNull Term s = tt.sub(i);
            if (inStructure == -1 || ((s.structure() & inStructure) > 0)) {
                r |= (addOrRemoved) ? t.add(s) : t.remove(s);
                if (!addOrRemoved && r) //on removal we can exit early
                    return true;
            }
        }
        return r;
    }

    @NotNull
    default boolean recurseTermsToSet(int inStructure, @NotNull Collection<Term> t, boolean addOrRemoved) {
        final boolean[] r = {false};
        Predicate<Term> selector = (s) -> {

            if (!addOrRemoved && r[0]) { //on removal we can exit early
                return false;
            }

            if (inStructure == -1 || ((s.structure() & inStructure) > 0)) {
                r[0] |= (addOrRemoved) ? t.add(s) : t.remove(s);
            }

            return true;
        };

        if (inStructure != -1)
            recurseTerms((p) -> p.hasAny(inStructure), selector, null);
        else
            recurseTerms(any -> true, selector, null);

        return r[0];
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
        int s = size();
        if (s > 0 && !impossibleSubTerm(y)) {
            for (int i = 0; i < s; i++) {
                Term x = sub(i);
                if (x.equals(y) || (x.containsRecursively(y))) {
                    return true;
                }
            }
        }
        return false;
    }

    default boolean containsRecursively(@NotNull Term y, Predicate<Term> subTermOf) {
        int s = size();
        if (s > 0 && !impossibleSubTerm(y)) {
            for (int i = 0; i < s; i++) {
                Term x = sub(i);
                if (x.equals(y) || (x.containsRecursively(y, subTermOf)))
                    return true;
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

    default boolean equalTerms(@NotNull List<Term> c) {
        int s = size();
        if (s != c.size())
            return false;
        for (int i = 0; i < s; i++) {
            if (!sub(i).equals(c.get(i)))
                return false;
        }
        return true;
    }

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


    /**
     * an array of the subterms, which an implementation may allow
     * direct access to its internal array which if modified will
     * lead to disaster. by default, it will call 'toArray' which
     * guarantees a clone. override with caution
     */
    default Term[] theArray() {
        return toArray();
    }

    /**
     * an array of the subterms
     * this is meant to be a clone always
     */
    default Term[] toArray() {
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
    static Term[] theTermArray(@NotNull Op op, int dt, @NotNull Term... tt) {
        return mustSortAndUniquify(op, dt, tt.length) ?
                Terms.sorted(tt) :
                tt;
    }

    /**
     * non-zero or non-iternal dt disqualifies any reason for needing a TermSet
     */
    static boolean mustSortAndUniquify(@NotNull Op op, int dt, int num) {
        return num > 1 && op.commutative && (concurrent(dt));
    }

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

    static int compare(@NotNull TermContainer a, @NotNull TermContainer b) {

        if (a.equals(b)) return 0;
        int diff;
        if ((diff = Integer.compare(a.volume(), b.volume())) != 0)
            return diff;

        int s;
        if ((diff = Integer.compare((s = a.size()), b.size())) != 0)
            return diff;

//        if ((diff = (a.hashCode() - b.hashCode())) != 0)
//            return diff;

//        if ((diff = Integer.compare(a.structure(), b.structure())) != 0)
//            return diff;

        int inequalVariable = -1; //only need to compare the first non-equal variable term
        for (int i = 0; i < s; i++) {
            Term x = a.sub(i);
            Term y = b.sub(i);
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
            return a.sub(inequalVariable).compareTo(b.sub(inequalVariable));
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
     * equality is compared by instance for speed
     */
    @NotNull
    static Term[] exceptThe(@NotNull Term[] input, Term a, @NotNull Term[] output) {
//        int targetLen = input.size() - 1;
//        if (output.length!= targetLen) {
//            throw new RuntimeException("wrong size");
//        }
        int j = 0;
        for (Term x : input) {
            if (x != a)
                output[j++] = x;
        }

        assert(j == output.length):"permute underflow";


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
            return ZeroProduct;
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

    /** matches in the correct ordering conditions for CONJ */
    static boolean unifyConj(TermContainer X, int Xdt, TermContainer Y, int Ydt, @NotNull Unify u) {
        boolean xUnknown = Xdt == XTERNAL;
        boolean yUnknown = Ydt == XTERNAL;
        if (xUnknown || yUnknown) {
            //if either is XTERNAL then the order is unknown so match commutively
            return X.unifyCommute(Y, u);
        }

        boolean xEternal = Xdt == DTERNAL;
        boolean yEternal = Ydt == DTERNAL;

        if (xEternal || yEternal) {
            //both eternal, match commutively
            return X.unifyCommute(Y, u);
        }
//        if (xEternal ^ yEternal) {
//            //one is eternal, the other is not
//            return false;
//        }

        //both temporal here, so compare in the right sequence:
        boolean xReversed = (Xdt < 0);
        boolean yReversed = (Ydt < 0);
        if (xReversed ^ yReversed) {
            X = X.reverse(); //just need to reverse one
        }
        return X.unifyLinear(Y, u);
    }

    default boolean unifyLinear(TermContainer Y, @NotNull Unify u) {
        /**
         * a branch for comparing a particular permutation, called from the main next()
         */
        int s = size();
        switch (s) {
            case 0:
                return true; //shouldnt ever happen

            case 1:
                return sub(0).unify(Y.sub(0), u);

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
                for (int i = s - 1; i >= 0; i--) {
                    if (!sub(j).unify(Y.sub(j), u))
                        return false;
                    if (i > 0 && ++j == s)
                        j = 0;
                }
                return true;
            }
        }

    }

    default boolean unifyCommute(TermContainer y, @NotNull Unify u) {

        if (y.equals(this))
            return true;

        //if there are no variables of the matching type, then it seems CommutivePermutations wouldnt match anyway
//        if (!unifyPossible(subst.type)) {
//            return false; //TODO this still may not be the final answer
//        }

        //lexic sorted so that the formed termutator has a canonical representation, preventing permuted duplicates in the termute chain
        SortedSet<Term> xs = /*toSorted*/toSortedSet();
        SortedSet<Term> ys = y./*toSorted*/toSortedSet();
        //xs.removeIf(s -> !subst.matchType(s) && ys.remove(s));
        xs.removeIf(ys::remove);

        //subst.termutes.add(new CommutivePermutations(TermVector.the(xs), TermVector.the(ys)));
        int xss = xs.size();
        int yss = ys.size();


        if (yss == 1) {
            //special case
            //  ex: {x,%1} vs. {x,z} --- there is actually no combination here
            //  Predicate<Term> notType = (x) -> !subst.matchType(x);
            //another case:
            //  xss > 1, yss=1: because of duplicates in ys that were removed; instead apply ys.first() to each of xs
            //  note for validation: the reverse will not work (trying to assign multiple different terms to the same variable in x)
            Term yy = ys.first();
            for (Term x : xs) {
                if (!x.unify(yy, u))
                    return false;
            }
            return true; //they all unified
        } else if (yss == xss) {

            u.termutes.add(new CommutivePermutations(
                    TermVector.the(xs), TermVector.the(ys)
            ));
            return true;
        } else /* yss!=xss */ {
            return false; //TODO this may possibly be handled
        }
    }


    /**
     * match a range of subterms of Y.
     */
    @NotNull
    default Term[] toArraySubRange(int from, int to) {
        if (from == 0 && to == size()) {
            return toArray();
        } else {

            int s = to - from;

            Term[] l = new Term[to - from];

            int y = from;
            for (int i = 0; i < s; i++) {
                l[i] = sub(y++);
            }

            return l;
        }
    }

    default boolean recurseSubTerms(BiPredicate<Term, Term> whileTrue, Compound parent) {
        int s = size();
        for (int i = 0; i < s; i++) {
            if (!sub(i).recurseTerms(whileTrue, parent))
                return false;
        }
        return true;
    }

    @Override
    default void recurseTerms(@NotNull Consumer<Term> v) {
        forEach(s -> s.recurseTerms(v));
    }

    default boolean recurseTerms(Predicate<Term> parentsMust, Predicate<Term> whileTrue, Term parent) {
        return AND(s -> s.recurseTerms(parentsMust, whileTrue, parent));
    }

    default TermContainer reverse() {
        return size() > 1 ? new ReverseTermContainer(this) : this;
    }

    //    /**
//     * returns a sorted and de-duplicated version of this container
//     */
//    default TermContainer sorted() {
//        int s = size();
//        if (s <= 1)
//            return this;
//
//
//        Term[] tt = Terms.sorted(toArray());
//        if (equalTerms(tt))
//            return this;
//        else
//            return Op.subterms(tt);
//    }

}
