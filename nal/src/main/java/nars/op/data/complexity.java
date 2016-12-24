package nars.op.data;

import nars.$;
import nars.term.Term;
import nars.term.transform.Functor;
import nars.term.var.Variable;
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
        if (!(x instanceof Variable))
            return $.the(x.complexity());
        return null;
    }
}
