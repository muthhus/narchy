package nars.term.transform;

import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/** I = input term type, T = transformable subterm type */
public interface CompoundTransform<I extends Compound, T extends Term> extends Predicate<Term> {

    Term apply(@Nullable I parent, @NotNull T subterm);

    /** enable predicate determined by the superterm, tested before processing any subterms */
    default boolean testSuperTerm(@NotNull I terms) {
        return true;
    }

}
