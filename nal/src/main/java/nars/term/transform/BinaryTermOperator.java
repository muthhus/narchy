package nars.term.transform;

import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 12/12/15.
 */
public abstract class BinaryTermOperator implements TermTransform {

    protected static void ensureCompounds(@NotNull Term a, @NotNull Term b) {
        if (!(a instanceof Compound) || !(b instanceof Compound))
            throw new RuntimeException("only applies to compounds");
    }


    @NotNull
    @Override public final Term function(@NotNull Compound x) {
        if (x.size()!=2)
            throw new UnsupportedOperationException("# args must equal 2");

        return apply(x.term(0), x.term(1));
    }

    @NotNull
    public abstract Term apply(Term a, Term b);
}
