package nars.term.subst;

import nars.NAR;
import nars.Op;
import nars.Param;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Predicate;

/** not thread safe, use 1 per thread (do not interrupt matchAll) */
public class UnifySubst extends Unify {

    //static final Logger logger = LoggerFactory.getLogger(UnifySubst.class);

    @NotNull
    public final NAR nar;


    final Predicate<Term> target;
    private Term a;

    int matches;

    public UnifySubst(Op varType, @NotNull NAR n, Predicate<Term> target, int ttl) {
        super(n.terms, varType, n.random(), Param.UnificationStackMax, ttl);

        this.nar = n;
        this.target = target;

    }

    @Override
    public boolean unify(@NotNull Term x, @NotNull Term y,  boolean finish) {
        this.a = x;
        this.matches = 0;
        return super.unify(x, y, finish);
    }


    public int matches() {
        return matches;
    }


    @Override public boolean onMatch() {

        //TODO combine these two blocks to use the same sub-method

        //try {


            Term aa = resolve(a, xy);
            if (aa!=null && target.test(aa))
                matches++;

//        }
//        catch (InvalidTermException e) {
//            if (Param.DEBUG)
//                logger.warn("{}",e);
//
//        }

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


        //return matches < maxMatches; //determines how many
        return false;
    }


    @Nullable Term resolve(@NotNull Term t, @NotNull Map<Term,Term> subs) {
        //try {

            return /*subs.isEmpty() ?
                    t : *//* no change necessary */
                    new MapSubst(subs).transform(t, nar.terms);
//        } catch (InvalidTermException e) {
//            return null;
//        }
    }

}
