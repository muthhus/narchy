package nars.derive.match;

import nars.term.Term;
import nars.term.var.AbstractVariable;
import org.jetbrains.annotations.NotNull;

import static nars.Op.VAR_PATTERN;

/**
 * Created by me on 12/5/15.
 */
public class EllipsisZeroOrMore extends Ellipsis {

    public EllipsisZeroOrMore(@NotNull AbstractVariable /*Variable*/ name) {
        super(name, 0);
    }


//    @Override
//    public @NotNull Variable clone(@NotNull AbstractVariable newVar, VariableNormalization normalizer) {
////        if (newVar.hashCode()==hash)
////            return this;
//        return new EllipsisZeroOrMore(newVar);
//    }

    @NotNull
    @Override
    public final String toString() {
        return super.toString() + "..*";
    }


    final static int RANK = Term.opX(VAR_PATTERN, 4 /* different from normalized variables with a subOp of 0 */);
    @Override public int opX() { return RANK;    }

}
