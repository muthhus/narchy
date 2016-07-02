package nars.op.data;

import nars.$;
import nars.nal.op.BinaryTermOperator;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class differ extends BinaryTermOperator/*implements BinaryOperator<Term>*/ {


    @Nullable
    @Override
    public Term apply(@NotNull Term a, Term b) {
        if (!(a instanceof Compound) || !(b instanceof Compound))
            return null;

        return TermContainer.difference($.terms.builder(),
                (Compound) a, (Compound) b
        );
    }
}
