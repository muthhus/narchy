package nars.nal.op;

import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 12/12/15.
 */
public abstract class BinaryTermOperator implements TermTransform {



    @Nullable
    @Override public final Term function(@NotNull Compound x) {
        if (x.size()!=2)
            return null;

        return apply(x.term(0), x.term(1));
    }

    @Nullable
    public abstract Term apply(Term a, Term b);
}
