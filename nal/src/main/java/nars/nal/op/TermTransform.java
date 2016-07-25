package nars.nal.op;

import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


@FunctionalInterface  public interface TermTransform {

    @NotNull Term function(@NotNull Compound args);

}
