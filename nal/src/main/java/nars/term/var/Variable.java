package nars.term.var;

import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;

/**
 * similar to a plain atom, but applies altered operating semantics according to the specific
 * varible type, as well as serving as something like the "marker interfaces" of Atomic, Compound, ..
 *
 * implemented by both raw variable terms and variable concepts
 **/
public interface Variable extends Atomic {


    /** an ID by which this variable can be uniquely identified,
     * among the other existing variables with the same ID but
     * from other variable op's #$?%
     */
    int id();

    @Override
    int hashCode();


    @Override
    boolean equals(Object o);

    //    @Override
//    default int volume() {
//        //TODO decide if this is the case for zero-or-more ellipsis
//        return 1;
//    }

    /**
     * The syntactic complexity of a variable is 0, because it does not refer to
     * any concept.
     *
     * @return The complexity of the term, an integer
     */
    @Override
    default int complexity() {
        return 0;
    }


    @Override
    default int init(@NotNull int[] meta) {
        int i;
        switch (op()) {
            case VAR_DEP:
                i = 0;
                break;
            case VAR_INDEP:
                i = 1;
                break;
            case VAR_QUERY:
                i = 2;
                break;
            case VAR_PATTERN:
                i = 3;
                break;
            default:
                throw new UnsupportedOperationException();
        }
        meta[i] ++;
        meta[4] ++;
        meta[5] |= structure();
        return hashCode();
    }

}
