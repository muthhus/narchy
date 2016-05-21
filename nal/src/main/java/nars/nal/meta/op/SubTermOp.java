package nars.nal.meta.op;

import nars.Op;
import nars.nal.meta.AtomicBoolCondition;
import nars.nal.meta.PremiseEval;
import org.jetbrains.annotations.NotNull;

/**
 * requires a specific subterm type
 */
public final class SubTermOp extends AtomicBoolCondition {

    public final int subterm;
    public final int op;

    @NotNull
    private final transient String id;


    public SubTermOp(int subterm, @NotNull Op op) {
        this.subterm = subterm;
        this.op = op.ordinal();
        id = subterm + ":\"" + op + '"';
    }

    @NotNull
    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean booleanValueOf(@NotNull PremiseEval ff) {
        return ff.subTermIs(subterm, op);
    }
}
