package nars.term;

import nars.Op;
import org.jetbrains.annotations.Nullable;

/** has, or is associated with a specific term */
@FunctionalInterface
public interface Termed<TT extends Term>  {

    TT term();

    @Nullable
    default Op op() { return term().op(); }

    default boolean isAny(int vector) { return term().isAny(vector); }

    default int opRel() {
        return term().opRel();
    }

    default boolean levelValid(int nal) {
        return term().levelValid(nal);
    }

    default boolean isNormalized() {
        return term().isNormalized();
    }

    default boolean isCompound() {
        return term().isCompound();
    }
}
