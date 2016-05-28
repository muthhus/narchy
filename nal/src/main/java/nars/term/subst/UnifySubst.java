package nars.term.subst;

import nars.Memory;
import nars.Op;
import nars.term.Term;
import nars.term.Termed;
import nars.term.subst.FindSubst;
import nars.term.subst.MapSubst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/** not thread safe, use 1 per thread (do not interrupt matchAll) */
public class UnifySubst extends FindSubst  {

    @NotNull
    public final Memory memory;


    final Collection<Termed> target;
    final int maxMatches;

    @NotNull
    private Term a, b;

    int matches;

    public UnifySubst(Op varType, @NotNull Memory memory, Collection<Termed> target, int maxMatches) {
        super(memory.index, varType, memory.random);

        this.memory = memory;
        this.maxMatches = maxMatches;
        this.target = target;

    }

    @Override
    public boolean matchAll(@NotNull Term x, @NotNull Term y, boolean finish) {
        this.a = x;
        this.b = y;
        this.matches = 0;

        if (x.hasAny(type) || y.hasAny(type)) { //no need to unify if there is actually no variable
            clear();
            return super.matchAll(x, y, finish);
        }

        return false;

    }

    public int matches() {
        return matches;
    }


    @Override public boolean onMatch() {

        //TODO combine these two blocks to use the same sub-method

        Term aa = resolve(a, xy);
//        if ((aa == null) ||
//        //Op aaop = aa.op();
//        //only set the values if it will return true, otherwise if it returns false the callee can expect its original values untouched
//            ((a.op() == Op.VAR_QUERY) && (aa.op().in(Op.VarDepOrIndep)))
//         ) {
//            return false;
//        }

//        Term bb = applySubstituteAndRenameVariables(b, yx);
//        if (bb == null) return false;
//        Op bbop = bb.op();
//        if (bbop == Op.VAR_QUERY && (bbop == Op.VAR_INDEP || bbop == Op.VAR_DEP))
//            return false;


        target.add(aa);
        //if (accept(a, aa))
        matches++;

        return matches < maxMatches; //determines how many
    }

    //abstract protected boolean accept(Term beliefTerm, Termed unifiedBeliefTerm);

    @Nullable
    Term resolve(@NotNull Term t, @Nullable Map<Term,Term> subs) {
        return (subs == null) || (subs.isEmpty()) ?
                t /* no change necessary */ :
                memory.index.resolve(t, new MapSubst(subs));
    }

}
