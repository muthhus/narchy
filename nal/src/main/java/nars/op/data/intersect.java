package nars.op.data;

import nars.$;
import nars.nal.op.BinaryTermOperator;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

public class intersect extends BinaryTermOperator {

    @NotNull
    @Override public Term apply(@NotNull Term a, @NotNull Term b) {
        ensureCompounds(a, b);

        return $.terms.builder().intersect(a.op(), (Compound)a, (Compound) b);
    }


}
