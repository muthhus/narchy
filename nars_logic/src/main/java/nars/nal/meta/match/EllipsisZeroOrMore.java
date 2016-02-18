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
public class EllipsisZeroOrMore extends Ellipsis {
    public EllipsisZeroOrMore(@NotNull Term /*Variable*/ name) {
        super(name, "..*");
    }

    @NotNull
    @Override
    public EllipsisZeroOrMore normalize(int serial) {
        return new EllipsisZeroOrMore($.v(Op.VAR_PATTERN, serial));
    }

    @Override
    public boolean valid(int collectable) {
        return collectable >= 0;
    }

    @Override
    public @NotNull Term clone(@NotNull Variable newVar, VariableNormalization normalizer) {
        return new EllipsisZeroOrMore(newVar);
    }
}
