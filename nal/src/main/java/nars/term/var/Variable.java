package nars.term.var;

import nars.term.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;

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
    @Nullable
    default Term normalize() {
        return this; //override: only normalize if given explicit offset with normalize(int offset) as is done during normalization
    }

    @Override
    Variable normalize(int offset);

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
    default float voluplexity() {
        return 0.5f;
    }



//    @Override
//    @Nullable
//    default Set<Variable> varsUnique(@Nullable Op type) {
//        if ((type == null || op() == type))
//            return Set.of(this);
//        else
//            return null;
//    }

}
