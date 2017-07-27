package nars.term;

import nars.Op;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Features exhibited by, and which can classify terms
 * and termlike productions
 */
public interface Termlike extends Termed {

//    /**
//     * volume = total number of terms = complexity + # total variables
//     */
//    @Override
//    default int volume() { return 0; }

    /**
     * total number of leaf terms, excluding variables which have a complexity of zero
     */
//    @Override
//    default int complexity() { return term().complexity(); }

//    @Override
//    default int structure() { return 0; }

    /**
     * number of subterms. if atomic, size=0
     */
    @Override
    int size();

    /** (first-level only, non-recursive)
     *  if contained within; doesnt match this term (if it's a term);
     *  false if term is atomic since it can contain nothing
     *
     */
    boolean contains(Termlike t);

    default boolean containsRecursively(Term t) {
        return contains(t);
    }

    default boolean containsRecursively(Term t, Predicate<Term> inSubtermsOf) {
        return contains(t);
    }


    /** whether any subterms (recursively) have
     *  non-DTernal temporal relation */
    default boolean isTemporal() {
        return false;
    }

    default boolean hasAll(int structuralVector) {
        return Op.hasAll(structure(), structuralVector);
    }

    default boolean hasAny(int structuralVector) {
        return (structure() & structuralVector) != 0;
    }

    /** tests if contains a term in the structural hash
     *  WARNING currently this does not detect presence of pattern variables
     * */
    default boolean hasAny(@NotNull Op op) {
        return (op == Op.VAR_PATTERN) ? (varPattern() > 0) : hasAny(op.bit);
    }

    default boolean impossibleSubTerm(@NotNull Termlike target) {
        //if the OR produces a different result compared to subterms,
        // it means there is some component of the other term which is not found
        //return ((possibleSubtermStructure | existingStructure) != existingStructure);
        return  (!hasAll(target.structure())) ||
                (impossibleSubTermVolume(target.volume()));
    }

    /** if it's larger than this term it can not be equal to this.
     * if it's larger than some number less than that, it can't be a subterm.
     */
    default boolean impossibleSubTermOrEqualityVolume(int otherTermsVolume) {
        return otherTermsVolume > volume();
    }

    /** tries to get the ith subterm (if this is a TermContainer),
     *  or of is out of bounds or not a container,
     *  returns the provided ifOutOfBounds */
    @Nullable <T extends Term> T sub(int i, @Nullable T ifOutOfBounds);

    default boolean impossibleSubTermVolume(int otherTermVolume) {
//        return otherTermVolume >
//                volume()
//                        - 0|1 /* 0 if subterms, 1 for the compound itself */
//                        - (size() - 1) /* each subterm has a volume >= 1, so if there are more than 1, each reduces the potential space of the insertable */

        /*
        otherTermVolume > volume - 1 - (size - 1)
                        > volume - size
         */
        return otherTermVolume > volume() - size();
    }


    /**
     * @param meta 6-element array that accumulates the metadata
     * @return returns hashcode if a Term, but TermContainers may return zero
     */
    default void init(@NotNull int[] meta) {

        meta[0] += varDep();
        meta[1] += varIndep();
        meta[2] += varQuery();

        meta[3] += varPattern();
        meta[4] += volume();
        meta[5] |= structure();

    }

    default boolean impossibleSubTermOrEquality(@NotNull Term target) {
        return ((!hasAll(target.structure())) ||
                (impossibleSubTermOrEqualityVolume(target.volume())));
    }

    /** recurses all subterms (0th and 1st-layer only) while the result of the predicate is true;
     *  returns true if all true
     *
     * @param v*/
    boolean AND(Predicate<Term> v);

    /** returns false if the supplied predicate fails for any of the recursive subterms of the specified type */
    boolean ANDrecurse(@NotNull Predicate<Term> v);

    void recurseTerms(@NotNull Consumer<Term> v);


    /** recurses all subterms until the result of the predicate becomes true;
     *  returns true if any true
     *
     * @param v*/
    boolean OR(Predicate<Term> v);

    boolean ORrecurse(@NotNull Predicate<Term> v);


//    /** returns the number of subterms (1st layer) having the provided operator */
//    default int subCount(Op o) {
//        return 0;
//    }

    /** total # of variables, excluding pattern variables */
    default int vars() {
        return varDep() + varIndep() + varQuery();
    }

//    @Override int varIndep();
//    @Override int varDep();
//    @Override int varQuery();
//    @Override int varPattern();

        /** # of contained independent variables in subterms (1st layer only) */
    @Override
    default int varIndep() { return 0; }
    /** # of contained dependent variables in subterms (1st layer only) */
    @Override
    default int varDep() { return 0; }
    /** # of contained query variables in subterms (1st layer only) */
    @Override
    default int varQuery() { return 0; }
    /** # of contained pattern variables in subterms (1st layer only) */
    @Override
    default int varPattern() { return 0; }



//    default boolean unifyPossible(@Nullable Op t) {
//        return (t == null) ? hasAny(Op.VariableBits) : hasAny(t);
//    }

    /** used to decide if a compound is "potentially" dynamic, or
     *  whether it can safely be cached/memoized -- or if it must be evaluated.
     *  if unsure, err on the side of caution and return true.
     */
    default boolean isDynamic() {
        return false;
    }

    /** return whether a subterm op at an index is an operator.
     * if there is no subterm or the index is out of bounds, returns false.
     */
    default boolean subIs(int i, Op o) {
        Term x = sub(i, null);
        return x != null && x.op() == o;
    }
    default boolean subIs(int i, Term maybeEquals) {
        Term x = sub(i, null);
        return x != null && x.equals(maybeEquals);
    }

    /** compares this term's op with 'thisOp', then performs subIs(i, sub) */
    default boolean subIs(Op thisOp, int i, Term sub) {
        if (op()!=thisOp)
            return false;

        return subIs(i, sub);
    }

    /** if type is null, returns total # of variables */
    default int vars(@Nullable Op type) {
        if (type == null)
            return vars() + varPattern();

        switch (type) {
            case VAR_PATTERN: return varPattern();
            case VAR_QUERY: return varQuery();
            case VAR_DEP: return varDep();
            case VAR_INDEP: return varIndep();
        }

        throw new UnsupportedOperationException();
    }



}
