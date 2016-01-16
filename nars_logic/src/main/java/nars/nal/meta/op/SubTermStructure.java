package nars.nal.meta.op;

import nars.Op;
import nars.term.compound.Compound;
import nars.term.transform.FindSubst;
import org.jetbrains.annotations.NotNull;

/**
 * requires a specific subterm to have minimum bit structure
 */
public final class SubTermStructure extends PatternOp {
    public final int subterm;
    public final int bits;
    @NotNull
    private final transient String id;


    public SubTermStructure(@NotNull Op matchingType, int subterm, int bits) {
        this.subterm = subterm;

        if (matchingType != Op.VAR_PATTERN)
            bits &= (~matchingType.bit());
        //bits &= ~(Op.VariableBits);

        this.bits = bits;
        id = "(subterm:" + subterm + "):(struct:" +
                Integer.toString(bits, 16) + ')';
    }


    @NotNull
    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean run(@NotNull FindSubst ff) {
        Compound t = (Compound) ff.term.get();
        return !t.term(subterm).impossibleStructureMatch(bits);
    }
}
