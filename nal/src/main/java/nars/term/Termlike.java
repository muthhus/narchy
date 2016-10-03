package nars.term;

import nars.Op;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Features exhibited by, and which can classify terms
 * and termlike productions
 */
public interface Termlike {

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
     *  (first-level only, non-recursive)
     */
    boolean containsTerm(Termlike t);

    /** whether any subterms (recursively) have a non-DTernal temporal relation */
    boolean hasTemporal();

    default boolean hasAll(int structuralVector) {
        return Op.hasAll(structure(), structuralVector);
    }

    default boolean hasAny(int structuralVector) {
        return Op.hasAny(structure(), structuralVector);
    }
    /** tests if contains a term in the structural hash
     *  WARNING currently this does not detect presence of pattern variables
     * */
    default boolean hasAny(@NotNull Op op) {
        return (op == Op.VAR_PATTERN) ? (varPattern() > 0) : hasAny(op.bit);
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

    @Nullable Term termOr(int i, @Nullable Term ifOutOfBounds);

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
    default int vars() {
        return varDep() + varIndep() + varQuery();
    }

    /** # of contained independent variables */
    int varIndep();
    /** # of contained dependent variables */
    int varDep();
    /** # of contained query variables */
    int varQuery();
    /** # of contained pattern variables */
    int varPattern();


    default long sizeVolumeStructure() {
               long l = (size()+1);
        l = (l << 16) | (volume()+1);
        l = (l << 16) | (structure()+1);
        //l = (l << 32) | hashCode();
        return l;
    }

}
