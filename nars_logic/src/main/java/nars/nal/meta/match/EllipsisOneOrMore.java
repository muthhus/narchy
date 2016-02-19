package nars.nal.meta.match;

import nars.$;
import nars.Op;
import nars.term.Term;
import nars.term.transform.VariableNormalization;
import nars.term.variable.Variable;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 12/5/15.
 */
public class EllipsisOneOrMore extends Ellipsis {

    public EllipsisOneOrMore(@NotNull Variable /*Variable*/ name) {
        this(name, "..+");
    }


    @Override
    public @NotNull Variable clone(@NotNull Variable newVar, VariableNormalization normalizer) {
        return new EllipsisOneOrMore(newVar);
    }

    public EllipsisOneOrMore(@NotNull Variable name, String s) {
        super(name, s);
    }

    @Override
    public boolean valid(int collectable) {
        return collectable > 0;
    }
}
