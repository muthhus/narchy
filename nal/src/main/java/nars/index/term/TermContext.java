package nars.index.term;

import nars.Op;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

/**
 * interface necessary for evaluating terms
 */
public interface TermContext {

    /** TODO rename: eval( */
    Term the(@NotNull Op op, int dt, Term[] subs);

    /** TODO rename: eval( */
    Termed get(Term x);

}
