package nars.op.data;

import nars.index.TermIndex;
import nars.nal.op.BinaryTermOperator;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;

public class union extends BinaryTermOperator {

    public union() {
        super("union");
    }

    @NotNull
    @Override public Term apply(@NotNull Term a, Term b, @NotNull TermIndex i) {
        if (!(a instanceof Compound) || !(b instanceof Compound))
            return null;

        return i.builder().union(a.op(), (Compound) a, (Compound) b );
    }

}
