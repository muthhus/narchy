package nars.term.transform;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;

/**
 * Created by me on 6/1/15.
 */
public abstract class VariableTransform implements CompoundTransform {

//    @Override
//    public @Nullable Termed apply(Term t) {
//        if (t instanceof Variable) {
//            return apply((Variable)t);
//        }
//        return t;
//    }

    @Override public final Term transform(Compound t, Op op, int dt) {
        return t.hasAny(Op.varBits) ? CompoundTransform.super.transform(t, op, dt) : t;
    }

}
