package nars.nal.meta.match;

import nars.$;
import nars.Op;
import nars.term.Term;
import nars.term.transform.VariableNormalization;
import nars.term.variable.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 12/5/15.
 */
public class EllipsisOneOrMore extends Ellipsis {

    public EllipsisOneOrMore(@NotNull Variable /*Variable*/ name) {
        super(name); //TODO decide if EllipsisTransform, a subclass, needs its own uniqueness hashmask
    }


    @Override
    public @NotNull Variable clone(@NotNull Variable newVar, VariableNormalization normalizer) {
//        if (newVar.hashCode()==hash)
//            return this;
        return new EllipsisOneOrMore(newVar);
    }


    @Override
    public boolean valid(int collectable) {
        return collectable > 0;
    }

    @Override
    public String toString() {
        return super.toString() + "..+";
    }





}
