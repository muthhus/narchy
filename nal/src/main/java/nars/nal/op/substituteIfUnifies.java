package nars.nal.op;

import nars.nal.meta.PremiseEval;
import nars.term.Compound;
import nars.term.Term;
import nars.term.transform.subst.OneMatchFindSubst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** substituteIfUnifies(term, variableType, varFrom, varTo)
 * TODO is this better named "substituteAll"
 * */
public class substituteIfUnifies extends substitute {

    /** recycled
     *  TODO initialize in construtor
     * */
    private OneMatchFindSubst matcher;

    public substituteIfUnifies() {

    }

    @Nullable
    @Override public Term function(@NotNull Compound p, @NotNull PremiseEval r) {
        final Term[] xx = p.terms();
        if (xx.length < 4) {
            throw new UnsupportedOperationException();
        }
        final Term term = xx[0];
        final Term opT = xx[1];
        final Term x = xx[2];
        final Term y = xx[3];

        if (this.matcher == null) {
            this.matcher = new OneMatchFindSubst(r.premise.nar);
        }

        return matcher.tryMatch(substitute.getOp(opT), r, term, x, y);
    }

}
