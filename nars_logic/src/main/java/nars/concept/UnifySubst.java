package nars.concept;

import nars.Memory;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.transform.subst.FindSubst;
import nars.term.transform.subst.MapSubst;
import nars.term.variable.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

/** not thread safe, use 1 per thread (do not interrupt matchAll) */
public abstract class UnifySubst extends FindSubst  {

    @NotNull
    public final Memory memory;

    /** whether to accept() an unmatched result */
    private final boolean acceptUnmatched;

    @NotNull
    private Term a, b;

    int matches;

    public UnifySubst(Op varType, @NotNull Memory memory, boolean acceptUnmatched) {
        super(varType, memory.random);

        this.memory = memory;
        this.acceptUnmatched = acceptUnmatched;

    }

    public void matchAll(@NotNull Term x, @NotNull Term y, boolean finish) {
        this.a = x;
        this.b = y;
        this.matches = 0;

        if (x.hasAny(type) || y.hasAny(type)) { //no need to unify if there is actually no variable
            clear();
            super.matchAll(x, y, finish);
        }

        if (acceptUnmatched && matches == 0) {
            accept(y, y);
        }
    }

    public int matches() {
        return matches;
    }


    @Override public boolean onMatch() {

        //TODO combine these two blocks to use the same sub-method

        Term aa = applySubstituteAndRenameVariables(a, xy);
        if (aa == null) return false;
        //Op aaop = aa.op();
        //only set the values if it will return true, otherwise if it returns false the callee can expect its original values untouched
        if ((a.op() == Op.VAR_QUERY) && (aa.hasVarIndep() || aa.hasVarIndep()) ) {
            return false;
        }

//        Term bb = applySubstituteAndRenameVariables(b, yx);
//        if (bb == null) return false;
//        Op bbop = bb.op();
//        if (bbop == Op.VAR_QUERY && (bbop == Op.VAR_INDEP || bbop == Op.VAR_DEP))
//            return false;


        if (accept(a, aa))
            matches++;

        return true; //determines how many
    }

    abstract protected boolean accept(Term beliefTerm, Term unifiedBeliefTerm);

    @Nullable
    Term applySubstituteAndRenameVariables(Term t, @Nullable Map<Term,Term> subs) {
        return (subs == null) || (subs.isEmpty()) ?
                t /* no change necessary */ :
                memory.index.apply(new MapSubst(subs), t);
    }

}
