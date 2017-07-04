package nars.op.data;

import nars.Op;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import static nars.Op.Null;

/** all X which are in the first term AND not in the second term */
public class differ extends Functor.BinaryFunctor {

    public differ() {
        super("differ");
    }


    @Override
    public Term apply(@NotNull Term a, @NotNull Term b) {

        if (a instanceof Compound && b instanceof Compound){
            Term y = Op.difference(a.op(), (Compound) a, (Compound) b);
            if (y.equals(a))
                return Null; //prevent identical fall-through
            return y;
        }

        return null;

    }
}
