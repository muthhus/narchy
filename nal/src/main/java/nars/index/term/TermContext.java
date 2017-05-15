package nars.index.term;

import nars.Op;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * interface necessary for evaluating terms
 */
public interface TermContext {

    /** TODO rename: eval( */
    Term the(@NotNull Op op, int dt, Term[] subs);

    /** TODO rename: eval( */
    Termed get(Term x, boolean createIfAbsent);

    /**
     * internal get procedure: get if not absent
     */
    @Nullable
    default Termed get(@NotNull Term t) {
        return get(t, false);
    }

    default Termed getIfPresentElse(@NotNull Term t) {
        Termed y = get(t, false);
        if (y != null)
            return y;
        else
            return t;
    }

}