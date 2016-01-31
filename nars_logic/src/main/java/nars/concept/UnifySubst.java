package nars.concept;

import nars.Memory;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.transform.subst.FindSubst;
import nars.term.transform.subst.MapSubst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

/** not thread safe, use 1 per thread (do not interrupt matchAll) */
public abstract class UnifySubst extends FindSubst implements Consumer<Term> {

    @NotNull
    public final Memory memory;

    /** whether to accept() an unmatched result */
    private final boolean acceptUnmatched;

    @NotNull
    private Term a, b;

    int matches = 0;

    public UnifySubst(Op varType, @NotNull Memory memory, boolean acceptUnmatched) {
        super(varType, memory.random);

        this.memory = memory;
        this.acceptUnmatched = acceptUnmatched;

    }

    public void matchAll(@NotNull Term x, @NotNull Term y, boolean finish) {
        clear();
        this.a = x;
        this.b = y;
        this.matches = 0;

        super.matchAll(x, y, finish);

        if (acceptUnmatched && matches == 0) {
            accept(y);
        }
    }

    public int matches() {
        return matches;
    }


    @Override public boolean onMatch() {

        //TODO combine these two blocks to use the same sub-method

        Term aa = a;

        //FORWARD
        if (aa instanceof Compound) {

            aa = getXY(a);
            if (aa == null) aa = a;

            Op aaop = aa.op();
            if (a.op() == Op.VAR_QUERY && (aaop == Op.VAR_INDEP || aaop == Op.VAR_DEP))
                return false;

        }

        Term bb = b;

        //REVERSE
        if (bb instanceof Compound) {
            bb = applySubstituteAndRenameVariables(
                    ((Compound) b),
                    (Map<Term, Term>)yx //inverse map
            );

            if (bb==null)
                return false; //WHY?

            Op bbop = bb.op();
            if (b.op() == Op.VAR_QUERY && (bbop == Op.VAR_INDEP || bbop == Op.VAR_DEP))
                return false;
        }

        //t[0] = aa;
        //t[1] = bb;


        accept(bb);
        matches++;

        return true; //determines how many
    }

    @Nullable
    Term applySubstituteAndRenameVariables(Compound t, @Nullable Map<Term,Term> subs) {
        return (subs == null) || (subs.isEmpty()) ?
                t /* no change necessary */ :
                memory.index.apply(new MapSubst(subs), t);
    }

}
