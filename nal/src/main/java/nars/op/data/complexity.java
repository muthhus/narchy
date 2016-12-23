package nars.op.data;

import nars.$;
import nars.index.term.TermIndex;
import nars.term.Compound;
import nars.term.Term;
import nars.term.transform.Functor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 3/6/15.
 */
public class complexity extends Functor.UnaryFunctor {

    public complexity() {
        super("complexity");
    }

    @Override
    public @Nullable Term apply(Term x) {
        return $.the(x.complexity());
    }
}
