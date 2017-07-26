package nars.derive.match;

import nars.term.Term;
import nars.term.var.AbstractVariable;
import org.jetbrains.annotations.NotNull;

import static nars.Op.VAR_PATTERN;

/**
 * Created by me on 12/5/15.
 */
public class EllipsisOneOrMore extends Ellipsis {

    public EllipsisOneOrMore(@NotNull AbstractVariable /*Variable*/ name) {
        super(name, 1); //TODO decide if EllipsisTransform, a subclass, needs its own uniqueness hashmask
    }

//    protected EllipsisOneOrMore(@NotNull AbstractVariable name, int id) {
//        super(name, 1, id);
//    }

//    @Override
//    public @NotNull Variable clone(@NotNull AbstractVariable newVar, VariableNormalization normalizer) {
////        if (newVar.hashCode()==hash)
////            return this;
//        return new EllipsisOneOrMore(newVar);
//    }

    final static int RANK = Term.opX(VAR_PATTERN, 2 /* different from normalized variables with a subOp of 0 */);
    @Override public int opX() { return RANK;    }


    @NotNull
    @Override
    public String toString() {
        return super.toString() + "..+";
    }





}
