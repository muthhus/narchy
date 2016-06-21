/*
 * CompoundTerm.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.term;

import com.gs.collections.api.set.SetIterable;
import com.gs.collections.impl.list.mutable.primitive.IntArrayList;
import com.gs.collections.impl.set.mutable.UnifiedSet;
import nars.$;
import nars.Global;
import nars.Op;
import nars.nal.Tense;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.term.subst.FindSubst;
import nars.util.data.Util;
import nars.util.data.array.IntArrays;
import nars.util.data.sexpression.IPair;
import nars.util.data.sexpression.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

import static nars.nal.Tense.DTERNAL;

/**
 * a compound term
 * TODO make this an interface extending Subterms
 */
public interface Compound<T extends Term> extends Term, IPair, TermContainer<T> {


    static boolean atemporallyEqual(@NotNull Termed term, @NotNull Termed<Compound> beliefConcept) {
        //TODO can be accelerated
        Term t = term.term();
        if (t.op() == beliefConcept.op()) {
            Term b = beliefConcept.term();
            if (t.structure() == b.structure() && t.volume() == b.volume()) {
                return b.equals($.terms.atemporalize((Compound) t));
            }
        }
        return false;
    }

    /** gets the set of unique recursively contained terms of a specific type
     * TODO generalize to a provided lambda predicate selector
     * */
    @NotNull
    default Set<Term> recurseTermsToSet(@NotNull Op onlyType) {
        Set<Term> t = Global.newHashSet(volume());
        recurseTerms((t1) -> {
            if (t1.op() == onlyType)
                t.add(t1);
        });
        return t;
    }

    @NotNull
    default SetIterable<Term> recurseTermsToSet() {
        UnifiedSet<Term> t = new UnifiedSet(volume());
        recurseTerms(t::add);
        return t;
        //return t.toImmutable();
    }
    @NotNull
    default SetIterable<Term> recurseTermsToSet(int inStructure) {
        UnifiedSet<Term> t = new UnifiedSet(0);
        recurseTerms((s) -> {
            if ((s.structure() & inStructure) > 0)
                t.add(s);
        });
        return t;//.toImmutable();
    }

    @Override
    default void recurseTerms(@NotNull SubtermVisitor v) {
        v.accept(this);
        subterms().forEach(a -> a.recurseTerms(v));
    }

    @Override
    default void recurseTerms(@NotNull SubtermVisitorX v) {
        recurseTerms(v, this);
    }

    @Override
    default void recurseTerms(@NotNull SubtermVisitorX v, @Nullable Compound parent) {
        v.accept(this, parent);
        subterms().forEach(a -> a.recurseTerms(v, this));
    }

    @Override
    default int init(@NotNull int[] meta) {

        subterms().init(meta);

        meta[5] |= op().bit;

        return hashCode();
    }

    @Nullable default int[] pathTo(Term subterm) {
        if (subterm.equals(this)) return IntArrays.EMPTY_ARRAY;
        if (!containsTermRecursively(subterm)) return null;
        return pathTo(new IntArrayList(0), this, subterm);
    }

    static int[] pathTo(IntArrayList p, Term superTerm, Term target) {
        if (superTerm instanceof Compound) {
            Compound cc = (Compound)superTerm;
            for (int i = 0; i < cc.size(); i++) {
                Term s = cc.term(i);
                if (s.equals(target)) {
                    p.add(i);
                    return p.toArray();
                }
                if (s instanceof Compound) {
                    Compound cs = (Compound) s;
                    if (cs.containsTermRecursively(target)) {
                        p.add(i);
                        return pathTo(p, cs, target);
                    }
                }
            }
        }
        return null;
    }



    /**
     * unification matching entry point (default implementation)
     *
     * @param y compound to match against (the instance executing this method is considered 'x')
     * @param subst the substitution context holding the match state
     * @return whether match was successful or not, possibly having modified subst regardless
     *
     * implementations may assume that y's .op() already matches this, and that
     * equality has already determined to be false.
     * */
    default boolean match(@NotNull Compound y, @NotNull FindSubst subst) {

        TermContainer xsubs = subterms();
        int xs = xsubs.size();
        TermContainer ysubs = y.subterms();
        if (xs == ysubs.size())  {

            if (vars() + y.vars() == 0)
                return false; //no variables that could produce any matches

            @NotNull Op op = op();
            if (!op.isImage() || (dt() == y.dt())) {
                return Compound.commutative(op, xs) ?
                        subst.matchPermute(xsubs, ysubs) :
                        subst.matchLinear(xsubs, ysubs);
            }
        }

        return false;

    }

    @Override
    @NotNull
    default Compound<T> term() {
        return this;
    }

    @Override
    default void append(@NotNull Appendable p) throws IOException {
        TermPrinter.append(this, p);
    }


    @Override
    default int compareTo(@NotNull Termlike o) {

        if (this == o) return 0;

        int diff = Integer.compare(structure(), o.structure());
        if (diff != 0)
            return diff;

        Compound c = (Compound)o; //(o.term());

        int diff3 = Integer.compare(this.dt(), c.dt());
        if (diff3 != 0)
            return diff3;

        return this.subterms().compareTo(c.subterms());
    }


    /**
     * extracts a subterm provided by the address tuple
     * returns null if specified subterm does not exist
     */
    @Nullable
    default Term subterm(@NotNull int... path) {
        Term ptr = this;
        for (int i : path) {
            if ((ptr = ptr.termOr(i, null))==null)
                return null;
        }
        return ptr;
    }

    default Term termOr(int i, @Nullable Term ifOutOfBounds) {
        return subterms().termOr(i, ifOutOfBounds);
    }


    @NotNull
    TermContainer<T> subterms();


    /*@Override
    default String toString() {
        return toStringBuilder().toString();
    }*/

    @Nullable
    @Override
    default Object _car() {
        //if length > 0
        return term(0);
    }

    /**
     * cdr or 'rest' function for s-expression interface when arity > 1
     */
    @Nullable
    @Override
    default Object _cdr() {
        int len = size();
        switch (len) {
            case 1:
                throw new RuntimeException("Pair fault");
            case 2:
                return term(1);
            case 3:
                return new Pair(term(1), term(2));
            case 4:
                return new Pair(term(1), new Pair(term(2), term(3)));
        }

        //this may need tested better:
        Pair p = null;
        for (int i = len - 2; i >= 0; i--) {
            p = new Pair(term(i), p == null ? term(i + 1) : p);
        }
        return p;
    }


    @NotNull
    @Override
    default Object setFirst(Object first) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    default Object setRest(Object rest) {
        throw new UnsupportedOperationException();
    }


    @Override
    default int varDep() {
        return subterms().varDep();
    }

    @Override
    default int varIndep() {
        return subterms().varIndep();
    }

    @Override
    default int varQuery() {
        return subterms().varQuery();
    }

    @Override
    default int varPattern() {
        return subterms().varPattern();
    }

    @Override
    default int vars() {
        return subterms().vars();
    }



    @NotNull
    @Override
    default T term(int i) {
        return subterms().term(i);
    }

    @NotNull
    @Override
    default T[] terms() {
        return subterms().terms();
    }






    @Override
    default void forEach(@NotNull Consumer<? super T> c) {
        subterms().forEach(c);
    }


    @Override
    default int structure() {
        return subterms().structure() | op().bit;
    }


    @Override
    default int size() {
        return subterms().size();
    }

    @Override
    default int complexity() {
        return subterms().complexity(); //already has +1 for this compound
    }

    @Override
    default int volume() {
        return subterms().volume();  //already has +1 for this compound
    }

    @Override
    default boolean impossibleSubTermVolume(int otherTermVolume) {
        return subterms().impossibleSubTermVolume(otherTermVolume);
    }


    @Override
    default boolean isCommutative() {
        return commutative(op(), size());
    }

    public static boolean commutative(Op op, int size) {
        return op.commutative && size > 1;
    }

    @Override
    default void forEach(@NotNull Consumer<? super T> action, int start, int stop) {
        subterms().forEach(action, start, stop);
    }

    @Override
    default Iterator<T> iterator() {
        return subterms().iterator();
    }

    @Override
    default void copyInto(@NotNull Collection<Term> set) {
        subterms().copyInto(set);
    }

    @Override
    default boolean isTerm(int i, @NotNull Op o) {
        return subterms().isTerm(i, o);
    }




//    @Nullable
//    @Override
//    default Ellipsis firstEllipsis() {
//        //return subterms().firstEllipsis();
//        return null;
//    }



    @Nullable
    default Term last() {
        int s = size();
        return s == 0 ? null : term(s - 1);
    }



    @Override
    boolean isNormalized();

//    /** whether the anonymized form of this term equals x */
//    @Override default boolean equalsAnonymously(@NotNull Term x) {
//
//        if ((opRel()==x.opRel()) && (structure()==x.structure()) && (volume()==x.volume())) { //some simple pre-tests to hopefully avoid needing to anonymize
//
//            return anonymous().equals(x);
//        }
//
//        return false;
//    }

    /** sets temporal relation value (TEMPORARY). returns new value */
    @NotNull
    default Compound dt(int cycles) {

        if (cycles == dt()) return this;

        GenericCompound g = new GenericCompound(op(), cycles, (TermVector)subterms());
        if (isNormalized()) g.setNormalized();
        return g;
    }

    /** gets temporal relation value */
    int dt();

    default boolean temporal() {
        return dt()!= Tense.DTERNAL;
    }



    /** similar to a indexOf() call, this will search for a int[]
     * path to the first subterm occurrence of the supplied term,
     * or null if none was found
     */
    @Nullable
    default int[] isSubterm(@NotNull Term t) {
        if (containsTerm(t)) {
            IntArrayList l = new IntArrayList();

            if (isSubterm(this, t, l)) {

                return Util.reverse(l);
            }
        }
        return null;
    }


    static boolean isSubterm(@NotNull Compound container, @NotNull Term t, @NotNull IntArrayList l) {
        Term[] x = container.terms();
        int s = x.length;
        for (int i = 0; i < s; i++) {
            Term xx = x[i];
            if (xx.equals(t) || ((xx.containsTerm(t)) && isSubterm((Compound)xx, t, l))) {
                l.add(i);
                return true;
            } //else, try next subterm and its subtree
        }

        return false;
    }

    default boolean containsTermRecursively(@NotNull Term b) {
        if (this.equals(b))
            return true;

        if (impossibleSubTermOrEquality(b))
            return false;

        int s = size();
        for (int i = 0; i < s; i++) {
            Term x = term(i);
            if (x instanceof Compound) {
                if (((Compound)x).containsTermRecursively(b))
                    return true;
            }
            else {
                if (x.equals(b))
                    return true;
            }
        }

        return false;
    }

    @Override
    default boolean equalsIgnoringVariables(@NotNull Term other) {
        if (!(other instanceof Compound))
            return false;

        int s = size();

        if ((((Compound)other).dt() == dt()) && (other.size() == s)) {
            Compound o = (Compound)other;
            for (int i = 0; i < s; i++) {
                if (!term(i).equalsIgnoringVariables(o.term(i)))
                    return false;
            }
            return true;
        }
        return false;
    }

    @Override
    default boolean hasTemporal() {
        return hasAny(Op.TemporalBits) && ((dt() != DTERNAL) || or(Term::hasTemporal));
    }

    //    public int countOccurrences(final Term t) {
//        final AtomicInteger o = new AtomicInteger(0);
//
//        if (equals(t)) return 1;
//
//        recurseTerms((n, p) -> {
//            if (n.equals(t))
//                o.incrementAndGet();
//        });
//
//        return o.get();
//    }


//    public static class InvalidTermConstruction extends RuntimeException {
//        public InvalidTermConstruction(String reason) {
//            super(reason);
//        }
//    }


//    /**
//     * single term version of makeCompoundName without iteration for efficiency
//     */
//    @Deprecated
//    protected static CharSequence makeCompoundName(final Op op, final Term singleTerm) {
//        int size = 2; // beginning and end parens
//        String opString = op.toString();
//        size += opString.length();
//        final CharSequence tString = singleTerm.toString();
//        size += tString.length();
//        return new StringBuilder(size).append(COMPOUND_TERM_OPENER).append(opString).append(ARGUMENT_SEPARATOR).append(tString).append(COMPOUND_TERM_CLOSER).toString();
//    }

    //    @Deprecated public static class UnableToCloneException extends RuntimeException {
//
//        public UnableToCloneException(String message) {
//            super(message);
//        }
//
//        @Override
//        public synchronized Throwable fillInStackTrace() {
//            /*if (Parameters.DEBUG) {
//                return super.fillInStackTrace();
//            } else {*/
//                //avoid recording stack trace for efficiency reasons
//                return this;
//            //}
//        }
//
//
//    }


}


//    /** performs a deep comparison of the term structure which should have the same result as normal equals(), but slower */
//    @Deprecated public boolean equalsByTerm(final Object that) {
//        if (!(that instanceof CompoundTerm)) return false;
//
//        final CompoundTerm t = (CompoundTerm)that;
//
//        if (operate() != t.operate())
//            return false;
//
//        if (getComplexity()!= t.getComplexity())
//            return false;
//
//        if (getTemporalOrder()!=t.getTemporalOrder())
//            return false;
//
//        if (!equals2(t))
//            return false;
//
//        if (term.length!=t.term.length)
//            return false;
//
//        for (int i = 0; i < term.length; i++) {
//            if (!term[i].equals(t.term[i]))
//                return false;
//        }
//
//        return true;
//    }
//
//
//
//
//    /** additional equality checks, in subclasses, only called by equalsByTerm */
//    @Deprecated public boolean equals2(final CompoundTerm other) {
//        return true;
//    }

//    /** may be overridden in subclass to include other details */
//    protected int calcHash() {
//        //return Objects.hash(operate(), Arrays.hashCode(term), getTemporalOrder());
//        return name().hashCode();
//    }

//
//    /**
//     * Orders among terms: variable < atomic < compound
//     *
//     * @param that The Term to be compared with the current Term
//\     * @return The order of the two terms
//     */
//    @Override
//    public int compareTo(final AbstractTerm that) {
//        if (this == that) return 0;
//
//        if (that instanceof CompoundTerm) {
//            final CompoundTerm t = (CompoundTerm) that;
//            if (size() == t.size()) {
//                int opDiff = this.operate().ordinal() - t.operate().ordinal(); //should be faster faster than Enum.compareTo
//                if (opDiff != 0) {
//                    return opDiff;
//                }
//
//                int tDiff = this.getTemporalOrder() - t.getTemporalOrder(); //should be faster faster than Enum.compareTo
//                if (tDiff != 0) {
//                    return tDiff;
//                }
//
//                for (int i = 0; i < term.length; i++) {
//                    final int diff = term[i].compareTo(t.term[i]);
//                    if (diff != 0) {
//                        return diff;
//                    }
//                }
//
//                return 0;
//            } else {
//                return size() - t.size();
//            }
//        } else {
//            return 1;
//        }
//    }



    /*
    @Override
    public boolean equals(final Object that) {
        return (that instanceof Term) && (compareTo((Term) that) == 0);
    }
    */


//
//
//
//
//    /**
//     * Orders among terms: variable < atomic < compound
//     *
//     * @param that The Term to be compared with the current Term
//\     * @return The order of the two terms
//     */
//    @Override
//    public int compareTo(final Term that) {
//        /*if (!(that instanceof CompoundTerm)) {
//            return getClass().getSimpleName().compareTo(that.getClass().getSimpleName());
//        }
//        */
//        return -name.compareTo(that.name());
//            /*
//            if (size() == t.size()) {
//                int opDiff = this.operate().ordinal() - t.operate().ordinal(); //should be faster faster than Enum.compareTo
//                if (opDiff != 0) {
//                    return opDiff;
//                }
//
//                for (int i = 0; i < term.length; i++) {
//                    final int diff = term[i].compareTo(t.term[i]);
//                    if (diff != 0) {
//                        return diff;
//                    }
//                }
//
//                return 0;
//            } else {
//                return size() - t.size();
//            }
//        } else {
//            return 1;
//            */
//    }



//    @Override
//    public int compareTo(final Object that) {
//        if (that == this) return 0;
//
//        // variables have earlier sorting order than non-variables
//        if (!(that instanceof Compound)) return 1;
//
//        final Compound c = (Compound) that;
//
//        int opdiff = compareClass(this, c);
//        if (opdiff != 0) return opdiff;
//
//        return compare(c);
//    }

//    public static int compareClass(final Object b, final Object c) {
//        Class c1 = b.getClass();
//        Class c2 = c.getClass();
//        int h = Integer.compare(c1.hashCode(), c2.hashCode());
//        if (h != 0) return h;
//        return c1.getName().compareTo(c2.getName());
//    }

//    /**
//     * compares only the contents of the subterms; assume that the other term is of the same operator type
//     */
//    public int compareSubterms(final Compound otherCompoundOfEqualType) {
//        return Terms.compareSubterms(term, otherCompoundOfEqualType.term);
//    }


//    final static int maxSubTermsForNameCompare = 2; //tunable
//
//    protected int compare(final Compound otherCompoundOfEqualType) {
//
//        int l = length();
//
//        if ((l != otherCompoundOfEqualType.length()) || (l < maxSubTermsForNameCompare))
//            return compareSubterms(otherCompoundOfEqualType);
//
//        return compareName(otherCompoundOfEqualType);
//    }
//
//
//    public int compareName(final Compound c) {
//        return super.compareTo(c);
//    }

//    public final void recurseSubtermsContainingVariables(final TermVisitor v, Term parent) {
//        if (hasVar()) {
//            v.visit(this, parent);
//            //if (this instanceof Compound) {
//            for (Term t : term) {
//                t.recurseSubtermsContainingVariables(v, this);
//            }
//            //}
//        }
//    }

//    @Override
//    public boolean equals(final Object that) {
//        if (this == that)
//            return true;
//
//        if (!(that instanceof Compound)) return false;
//        Compound c = (Compound) that;
//        if (contentHash != c.contentHash ||
//                structureHash != c.structureHash ||
//                volume != c.volume)
//            return false;
//
//        final int s = this.length();
//        Term[] x = this.term;
//        Term[] y = c.term;
//        if (x != y) {
//            boolean canShare =
//                    (structureHash &
//                    ((1 << Op.SEQUENCE.ordinal()) | (1 << Op.PARALLEL.ordinal()))) == 0;
//
//            for (int i = 0; i < s; i++) {
//                Term a = x[i];
//                Term b = y[i];
//                if (!a.equals(b))
//                    return false;
//            }
//            if (canShare) {
//                this.term = (T[]) c.term;
//            }
//            else {
//                this.term = this.term;
//            }
//        }
//
//        if (structure2() != c.structure2() ||
//                op() != c.op())
//            return false;
//
//        return true;
//    }

//    @Override
//    public boolean equals(final Object that) {
//        if (this == that)
//            return true;
//        if (!(that instanceof Compound)) return false;
//
//        Compound c = (Compound) that;
//        if (contentHash != c.contentHash ||
//                structureHash != c.structureHash
//                || volume() != c.volume()
//                )
//            return false;
//
//        final int s = this.length();
//        Term[] x = this.term;
//        Term[] y = c.term;
//        for (int i = 0; i < s; i++) {
//            Term a = x[i];
//            Term b = y[i];
//            if (!a.equals(b))
//                return false;
//        }
//
//        return true;
//    }

    /* UNTESTED
    public Compound clone(VariableTransform t) {
        if (!hasVar())
            throw new RuntimeException("this VariableTransform clone should not have been necessary");

        Compound result = cloneVariablesDeep();
        if (result == null)
            throw new RuntimeException("unable to clone: " + this);

        result.transformVariableTermsDeep(t);

        result.invalidate();

        return result;
    } */




//    /**
//     * true if equal operate and all terms contained
//     */
//    public boolean containsAllTermsOf(final Term t) {
//        if ((op() == t.op())) {
//            return Terms.containsAll(term, ((Compound) t).term);
//        } else {
//            return this.containsTerm(t);
//        }
//    }

//    /**
//     * Try to add a component into a compound
//     *
//     * @param t1 The compound
//     * @param t2 The component
//     * @param memory Reference to the memory
//     * @return The new compound
//     */
//    public static Term addComponents(final CompoundTerm t1, final Term t2, final Memory memory) {
//        if (t2 == null)
//            return t1;
//
//        boolean success;
//        Term[] terms;
//        if (t2 instanceof CompoundTerm) {
//            terms = t1.cloneTerms(((CompoundTerm) t2).term);
//        } else {
//            terms = t1.cloneTerms(t2);
//        }
//        return Memory.make(t1, terms, memory);
//    }



//    /**
//     * Recursively check if a compound contains a term
//     * This method DOES check the equality of this term itself.
//     * Although that is how Term.containsTerm operates
//     *
//     * @param target The term to be searched
//     * @return Whether the target is in the current term
//     */
//    @Override
//    public boolean equalsOrContainsTermRecursively(final Term target) {
//        if (this.equals(target)) return true;
//        return containsTermRecursively(target);
//    }

/**
 * override in subclasses to avoid unnecessary reinit
 */
    /*public CompoundTerm _clone(final Term[] replaced) {
        if (Terms.equals(term, replaced)) {
            return this;
        }
        return clone(replaced);
    }*/

//    @Override
//    public int containedTemporalRelations() {
//        if (containedTemporalRelations == -1) {
//
//            /*if ((this instanceof Equivalence) || (this instanceof Implication))*/
//            {
//                int temporalOrder = this.getTemporalOrder();
//                switch (temporalOrder) {
//                    case TemporalRules.ORDER_FORWARD:
//                    case TemporalRules.ORDER_CONCURRENT:
//                    case TemporalRules.ORDER_BACKWARD:
//                        containedTemporalRelations = 1;
//                        break;
//                    default:
//                        containedTemporalRelations = 0;
//                        break;
//                }
//            }
//
//            for (final Term t : term)
//                containedTemporalRelations += t.containedTemporalRelations();
//        }
//        return this.containedTemporalRelations;
//    }



//    /**
//     * Gives a set of all (unique) contained term, recursively
//     */
//    public Set<Term> getContainedTerms() {
//        Set<Term> s = Global.newHashSet(complexity());
//        for (Term t : term) {
//            s.add(t);
//            if (t instanceof Compound)
//                s.addAll(((Compound) t).getContainedTerms());
//        }
//        return s;
//    }






//    /**
//     * forced deep clone of terms
//     */
//    public ArrayList<Term> cloneTermsListDeep() {
//        ArrayList<Term> l = new ArrayList(length());
//        for (final Term t : term)
//            l.add(t.clone());
//        return l;
//    }



    /*static void shuffle(final Term[] list, final Random randomNumber) {
        if (list.length < 2)  {
            return;
        }


        int n = list.length;
        for (int i = 0; i < n; i++) {
            // between i and n-1
            int r = i + (randomNumber.nextInt() % (n-i));
            Term tmp = list[i];    // swap
            list[i] = list[r];
            list[r] = tmp;
        }
    }*/

/*        public static void shuffle(final Term[] ar,final Random rnd)
        {
            if (ar.length < 2)
                return;



          for (int i = ar.length - 1; i > 0; i--)
          {
            int index = randomNumber.nextInt(i + 1);
            // Simple swap
            Term a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
          }

        }*/

///**
// * Check whether the compound contains a certain component
// * Also matches variables, ex: (&&,<a --> b>,<b --> c>) also contains <a --> #1>
// *  ^^^ is this right? if so then try containsVariablesAsWildcard
// *
// * @param t The component to be checked
// * @return Whether the component is in the compound
// */
//return Terms.containsVariablesAsWildcard(term, t);
//^^ ???

//    /**
//     * Try to replace a component in a compound at a given index by another one
//     *
//     * @param index   The location of replacement
//     * @param subterm The new component
//     * @return The new compound
//     */
//    public Term cloneReplacingSubterm(final int index, final Term subterm) {
//
//        final boolean e = (subterm != null) && (op() == subterm.op());
//
//        //if the subterm is alredy equivalent, just return this instance because it will be equivalent
//        if (subterm != null && (e) && (term[index].equals(subterm)))
//            return this;
//
//        List<Term> list = asTermList();//Deep();
//
//        list.remove(index);
//
//        if (subterm != null) {
//            if (!e) {
//                list.add(index, subterm);
//            } else {
//                //splice in subterm's subterms at index
//                for (final Term t : term) {
//                    list.add(t);
//                }
//
//                /*Term[] tt = ((Compound) subterm).term;
//                for (int i = 0; i < tt.length; i++) {
//                    list.add(index + i, tt[i]);
//                }*/
//            }
//        }
//
//        return Memory.term(this, list);
//    }


//    /**
//     * Check whether the compound contains all term of another term, or
//     * that term as a whole
//     *
//     * @param t The other term
//     * @return Whether the term are all in the compound
//     */
//    public boolean containsAllTermsOf_(final Term t) {
//        if (t instanceof CompoundTerm) {
//        //if (operate() == t.operate()) {
//            //TODO make unit test for containsAll
//            return Terms.containsAll(term, ((CompoundTerm) t).term );
//        } else {
//            return Terms.contains(term, t);
//        }
//    }



//    @Override
//    public boolean equals(final Object that) {
//        if (!(that instanceof CompoundTerm))
//            return false;
//
//        final CompoundTerm t = (CompoundTerm)that;
//        return name().equals(t.name());
//
//        /*if (hashCode() != t.hashCode())
//            return false;
//
//        if (operate() != t.operate())
//            return false;
//
//        if (size() != t.size())
//            return false;
//
//        for (int i = 0; i < term.size(); i++) {
//            final Term c = term.get(i);
//            if (!c.equals(t.componentAt(i)))
//                return false;
//        }
//
//        return true;*/
//
//    }





//boolean transform(CompoundTransform<Compound<T>, T> trans, int depth);



//    /**
//     * returns result of applySubstitute, if and only if it's a CompoundTerm.
//     * otherwise it is null
//     */
//    default Compound applySubstituteToCompound(Map<Term, Term> substitute) {
//        Term t = Term.substituted(this,
//                new MapSubst(substitute));
//        if (t instanceof Compound)
//            return ((Compound) t);
//        return null;
//    }

//    /**
//     * from: http://stackoverflow.com/a/19333201
//     */
//    public static <T> void shuffle(final T[] array, final Random random) {
//        int count = array.length;
//
//        //probabality for no shuffle at all:
//        if (random.nextInt(factorial(count)) == 0) return;
//
//        for (int i = count; i > 1; i--) {
//            final int a = i - 1;
//            final int b = random.nextInt(i);
//            if (b!=a) {
//                final T t = array[b];
//                array[b] = array[a];
//                array[a] = t;
//            }
//        }
//    }

//    static Term unwrap(Term x, boolean unwrapLen1SetExt, boolean unwrapLen1SetInt, boolean unwrapLen1Product) {
//        if (x instanceof Compound) {
//            Compound c = (Compound) x;
//            if (c.size() == 1) {
//                if ((unwrapLen1SetInt && (c instanceof SetInt)) ||
//                        (unwrapLen1SetExt && (c instanceof SetExt)) ||
//                        (unwrapLen1Product && (c instanceof Product))
//                        ) {
//                    return c.term(0);
//                }
//            }
//        }
//
//        return x;
//    }


