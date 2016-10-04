package nars.nal.meta.match;

import nars.term.transform.VariableNormalization;
import nars.term.var.AbstractVariable;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 12/5/15.
 */
public class EllipsisOneOrMore extends Ellipsis {

    public EllipsisOneOrMore(@NotNull AbstractVariable /*Variable*/ name) {
        super(name, 1); //TODO decide if EllipsisTransform, a subclass, needs its own uniqueness hashmask
    }

    protected EllipsisOneOrMore(@NotNull AbstractVariable name, int id) {
        super(name, 1, id);
    }

    @Override
    public @NotNull Variable clone(@NotNull AbstractVariable newVar, VariableNormalization normalizer) {
//        if (newVar.hashCode()==hash)
//            return this;
        return new EllipsisOneOrMore(newVar);
    }




    @NotNull
    @Override
    public String toString() {
        return super.toString() + "..+";
    }





}
