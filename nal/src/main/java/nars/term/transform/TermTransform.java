package nars.term.transform;

import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;


@FunctionalInterface  public interface TermTransform {

    @NotNull Term function(@NotNull Compound args);

}
