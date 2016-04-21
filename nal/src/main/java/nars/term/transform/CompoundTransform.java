package nars.term.transform;

import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/** I = input term type, T = transformable subterm type */
public interface CompoundTransform<I extends Compound, T extends Term> extends Predicate<Term> {

    @Nullable Termed<?> apply(I parent, T subterm);

    /** enable predicate determined by the superterm, tested before processing any subterms */
    default boolean testSuperTerm(@NotNull I terms) {
        return true;
    }

}
