package nars.term;

import nars.Op;
import org.jetbrains.annotations.NotNull;

/** has, or is associated with a specific term */
@FunctionalInterface
public interface Termed<T extends Term>  {

    @NotNull T term();

    @NotNull
    default Op op() { return term().op(); }

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
