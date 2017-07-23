package nars.index.term;

import nars.op.mental.AliasConcept;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * interface necessary for evaluating terms
 */
public interface TermContext {


    /** TODO rename: eval( */
    @Nullable Termed get(Term x, boolean createIfAbsent);

    /**
     * internal get procedure: get if not absent
     */
    @Nullable
    default Termed get(@NotNull Term t) {
        return get(t, false);
    }

    default Termed getIfPresentElse(@NotNull Term _else) {
        Termed y = get(_else);
        if (y != null)
            return y;
        else
            return _else;
    }

}
