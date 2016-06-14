package nars.term;

import nars.Op;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * Features exhibited by, and which can classify terms
 * and termlike productions
 */
public interface Termlike extends Comparable<Termlike> {

    /**
     * volume = total number of terms = complexity + # total variables
     */
    int volume();

    /**
     * total number of leaf terms, excluding variables which have a complexity of zero
     */
    int complexity();

    int structure();

    /**
     * number of subterms. if atomic, size=0
     */
    int size();

    /** if contained within; doesnt match this term (if it's a term);
     *  false if term is atomic since it can contain nothing
     *
     * @param t*/
    boolean containsTerm(Termlike t);

    boolean hasTemporal();

    default boolean hasAll(int structuralVector) {
        int s = structure();
        return Op.hasAll(s, structuralVector);
    }

    default boolean hasAny(int structuralVector) {
        return Op.hasAny(structure(), structuralVector);
    }
    /** tests if contains a term in the structural hash
     *  WARNING currently this does not detect presence of pattern variables
     * */
    default boolean hasAny(@NotNull Op op) {
//        if (op == Op.VAR_PATTERN)
//            return Variable.hasPatternVariable(this);
        return hasAny(op.bit);
    }

    default boolean impossibleSubterm(@NotNull Termlike target) {
        //if the OR produces a different result compared to subterms,
        // it means there is some component of the other term which is not found
        //return ((possibleSubtermStructure | existingStructure) != existingStructure);
        return  this==target ||
                ((!Op.hasAll(structure(), target.structure()))) ||
                (impossibleSubTermVolume(target.volume()));
    }

    /** if it's larger than this term it can not be equal to this.
     * if it's larger than some number less than that, it can't be a subterm.
     */
    default boolean impossibleSubTermOrEqualityVolume(int otherTermsVolume) {
        return otherTermsVolume > volume();
    }


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


    default boolean impossibleSubTermOrEquality(@NotNull Term target) {
        return ((!Op.hasAll(structure(), target.structure())) ||
                (impossibleSubTermOrEqualityVolume(target.volume())));
    }

    /** recurses all subterms while the result of the predicate is true;
     *  returns true if all true
     *
     * @param v*/
    boolean and(Predicate<Term> v);

    /** recurses all subterms until the result of the predicate becomes true;
     *  returns true if any true
     *
     * @param v*/
    boolean or(Predicate<Term> v);

    /** total # of variables, excluding pattern variables */
    int vars();

    /** # of contained independent variables */
    int varIndep();
    /** # of contained dependent variables */
    int varDep();
    /** # of contained query variables */
    int varQuery();
    /** # of contained pattern variables */
    int varPattern();


}
