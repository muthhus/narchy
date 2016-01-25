package nars.nal.op;

import nars.term.Compound;
import nars.term.Term;
import nars.term.TermBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 12/12/15.
 */
public abstract class UnaryTermOperator extends ImmediateTermTransform {

    @NotNull
    @Override public final Term function(@NotNull Compound x, TermBuilder i) {
        if (x.size()<1)
            throw new RuntimeException(this + " requires >= 2 args");

        return apply(x.term(0), i);
    }

    @NotNull
    public abstract Term apply(Term a, TermBuilder i);
}
