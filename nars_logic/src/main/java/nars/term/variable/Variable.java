package nars.term.variable;

import nars.term.atom.Atomic;

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
    int compareTo(Object o);

    @Override
    default int volume() {
        //TODO decide if this is the case for zero-or-more ellipsis
        return 1;
    }

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


}
