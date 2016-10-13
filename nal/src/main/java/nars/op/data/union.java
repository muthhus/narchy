package nars.op.data;

import nars.$;
import nars.term.Compound;
import nars.term.Term;
import nars.term.transform.BinaryTermOperator;
import org.jetbrains.annotations.NotNull;

public class union extends BinaryTermOperator {

    public union() {
        super("union");
    }

    @NotNull
    @Override public Term apply(@NotNull Term a, @NotNull Term b) {
        ensureCompounds(a, b);

        return $.terms.union(a.op(), (Compound) a, (Compound) b );
    }

}
