package nars.derive.meta.match;

import nars.term.transform.VariableNormalization;
import nars.term.var.AbstractVariable;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 12/5/15.
 */
public class EllipsisZeroOrMore extends Ellipsis {

    public EllipsisZeroOrMore(@NotNull AbstractVariable /*Variable*/ name) {
        super(name, 0);
    }


    @Override
    public @NotNull Variable clone(@NotNull AbstractVariable newVar, VariableNormalization normalizer) {
//        if (newVar.hashCode()==hash)
//            return this;
        return new EllipsisZeroOrMore(newVar);
    }

    @NotNull
    @Override
    public final String toString() {
        return super.toString() + "..*";
    }

}
