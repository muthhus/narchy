package nars.derive.meta.op;

import nars.derive.meta.BoolPred;
import nars.control.premise.Derivation;
import nars.term.Compound;
import nars.term.ProxyCompound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 5/26/16.
 */
abstract public class MatchTerm extends ProxyCompound implements BoolPred<Derivation> {

    @NotNull public final Term pattern;

    public final @Nullable BoolPred eachMatch;

    public MatchTerm(@NotNull Compound id, @NotNull Term pattern, @Nullable BoolPred eachMatch) {
        super(id);
        this.pattern = pattern;
        this.eachMatch = eachMatch;
    }

}
