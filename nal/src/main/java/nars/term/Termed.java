package nars.term;

import nars.Op;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** has, or is associated with a specific term */
@FunctionalInterface
public interface Termed<T extends Term>  {

    @NotNull T term();

    @Nullable
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

    @Nullable
    static Term termOrNull(@Nullable Termed x) {
        return x == null ? null : x.term();
    }

}
