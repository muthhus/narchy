package nars.term.transform;

import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/** I = input term type, T = transformable subterm type */
public interface CompoundTransform<I extends Compound, T extends Termed> extends Predicate<Term> {

    @Nullable
    Termed apply(I parent, T subterm, int depth);

    /** enable predicate determined by the superterm, tested before processing any subterms */
    default boolean testSuperTerm(Compound terms) {
        return true;
    }

}
