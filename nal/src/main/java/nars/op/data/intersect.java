package nars.op.data;

import nars.$;
import nars.nal.op.BinaryTermOperator;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class intersect extends BinaryTermOperator {

    @Nullable
    @Override public Term apply(@NotNull Term a, Term b) {
        if (!(a instanceof Compound) || !(b instanceof Compound))
            return null;

        return $.terms.builder().intersect(a.op(), (Compound)a, (Compound) b);
    }

}
