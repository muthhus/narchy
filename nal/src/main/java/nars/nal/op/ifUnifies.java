package nars.nal.op;

import nars.nal.meta.PremiseEval;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

/** if unification succeeds, just return the specified term (dont apply substitution) */
public final class ifUnifies extends substituteIfUnifies {

    @Override public Term function(@NotNull Compound p, @NotNull PremiseEval r) {
        return super.function(p, r) != null ? p.term(0) : null;
    }

}
