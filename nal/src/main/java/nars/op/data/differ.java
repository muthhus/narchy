package nars.op.data;

import nars.$;
import nars.term.Compound;
import nars.term.Term;
import nars.term.transform.BinaryTermOperator;
import org.jetbrains.annotations.NotNull;

/** all X which are in the first term AND not in the second term */
public class differ extends BinaryTermOperator {

    public differ() {
        super("differ");
    }

    @NotNull
    @Override
    public Term apply(@NotNull Term a, @NotNull Term b) {
        ensureCompounds(a, b);

        Term y = $.terms.difference( a.op(), (Compound) a, (Compound) b );
        if (y.equals(a))
            return False; //prevent identical fall-through
        return y;
    }
}
