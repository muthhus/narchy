package nars.op.data;

import nars.$;
import nars.Op;
import nars.nal.TermBuilder;
import nars.nal.op.BinaryTermOperator;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** all X which are in the first term AND not in the second term */
public class differ extends BinaryTermOperator {


    /**
     * returns null if empty set
     */
    @Nullable
    public static Term difference(@NotNull Op op, @NotNull Compound a, @NotNull Compound b) {
        return $.terms.builder().difference(op, a, b);
    }

    @NotNull
    public static Term difference(@NotNull TermBuilder t, @NotNull Compound a, @NotNull TermContainer b) {
        return t.difference(a.op(), a, b);
    }

    @NotNull
    @Override
    public Term apply(@NotNull Term a, @NotNull Term b) {
        ensureCompounds(a, b);

        return $.terms.builder().difference( a.op(), (Compound) a, (Compound) b );
    }
}
