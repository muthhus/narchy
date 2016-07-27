package nars.nal.op;

import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;


@FunctionalInterface  public interface TermTransform {

    @NotNull Term function(@NotNull Compound args);

}
