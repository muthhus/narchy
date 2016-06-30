package nars.op.data;

import nars.$;
import nars.index.TermIndex;
import nars.nal.op.BinaryTermOperator;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;

public class union extends BinaryTermOperator {

    @NotNull
    @Override public Term apply(@NotNull Term a, Term b) {
        if (!(a instanceof Compound) || !(b instanceof Compound))
            return null;

        return $.terms.builder().union(a.op(), (Compound) a, (Compound) b );
    }

}
