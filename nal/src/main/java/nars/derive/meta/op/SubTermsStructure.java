package nars.derive.meta.op;

import nars.Op;
import nars.derive.meta.AtomicPredicate;
import nars.premise.Derivation;
import org.jetbrains.annotations.NotNull;

/**
 * requires both subterms to match a minimum bit structure
 */
public final class SubTermsStructure extends AtomicPredicate<Derivation> {

    public final int bits;
    @NotNull
    private final transient String id;


    public SubTermsStructure(int bits) {
        this(Op.VAR_PATTERN, bits);
    }

    public SubTermsStructure(@NotNull Op matchingType, int bits) {

        this.bits = SubTermStructure.filter(matchingType, bits);

        id = "SubTermsStruct:" +
                Integer.toBinaryString(bits);
    }


    @NotNull
    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean test(@NotNull Derivation ff) {
        /*Compound t = ff.term;
        return !t.term(subterm).impossibleStructureMatch(bits);*/
        //if the OR produces a different result compared to subterms,
        // it means there is some component of the other term which is not found
        //return ((possibleSubtermStructure | existingStructure) != existingStructure);
        //if the OR produces a different result compared to subterms,
        // it means there is some component of the other term which is not found
        //return ((possibleSubtermStructure | existingStructure) != existingStructure);
        return Op.hasAll(ff.termSub1Struct, bits) && Op.hasAll(ff.termSub0Struct, bits);
    }
}
