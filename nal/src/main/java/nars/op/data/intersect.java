package nars.op.data;

import nars.$;
import nars.term.Compound;
import nars.term.Term;
import nars.term.transform.BinaryTermOperator;
import org.jetbrains.annotations.NotNull;

public class intersect extends BinaryTermOperator {

    public intersect() {
        super("intersect");
    }

    @NotNull
    @Override public Term apply(@NotNull Term a, @NotNull Term b) {
        ensureCompounds(a, b);

        return $.terms.intersect(a.op(), (Compound)a, (Compound) b);
    }


}
