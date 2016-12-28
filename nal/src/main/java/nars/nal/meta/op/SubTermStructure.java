package nars.nal.meta.op;

import nars.Op;
import nars.nal.Derivation;
import nars.nal.meta.AtomicBoolCondition;
import org.jetbrains.annotations.NotNull;

/**
 * requires a specific subterm to have minimum bit structure
 */
public final class SubTermStructure extends AtomicBoolCondition {
    public final int subterm;
    public final int bits;
    @NotNull
    private final transient String id;


    public SubTermStructure(int subterm, int bits) {
        this(Op.VAR_PATTERN, subterm, bits);
    }

    public SubTermStructure(@NotNull Op matchingType, int subterm, int bits) {
        this.subterm = subterm;


        this.bits = filter(matchingType, bits);
        if (this.bits == 0) {
            throw new RuntimeException("no filter effected");
        }
        id = "SubTermStruct" + subterm + ':' +
                Integer.toString(bits, 16);
    }


    @NotNull
    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean run(@NotNull Derivation ff, int now) {

        return ff.subTermMatch(subterm, bits);
    }

    static int filter(@NotNull Op matchingType, int bits) {
        if (matchingType != Op.VAR_PATTERN)
            bits &= (~matchingType.bit);

        bits &= (~Op.NEG.bit); //filter based on negation

        return bits;
    }
}
