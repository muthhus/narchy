package nars.nal.op;

import nars.nal.meta.PremiseMatch;
import nars.term.Compound;
import nars.term.Term;
import nars.term.transform.subst.FindSubst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** substituteIfUnifies(term, variableType, varFrom, varTo)
 * TODO is this better named "substituteAll"
 * */
public final class substituteIfUnifies extends substitute {

    @Nullable
    @Override public Term function(@NotNull Compound p, @NotNull PremiseMatch r) {
        final Term[] xx = p.terms();
        final Term term = xx[0];
        final Term op = xx[1];
        final Term x = xx[2];
        final Term y = xx[3];

        return new OneMatchFindSubst(op, r, term).tryMatch(x, y);
    }

    private final static class OneMatchFindSubst extends FindSubst {

        private final Term xterm;
        private final PremiseMatch r;
        private Term result = null;

        public OneMatchFindSubst(Term op, PremiseMatch r, Term xterm) {
            super(substitute.getOp(op), r.premise.memory().random);
            this.xterm = xterm;
            this.r = r;
        }

        /** terminates after the first match */
        @Override public boolean onMatch() {
            //apply the match before the xy/yx mapping gets reverted after leaving the termutator
            result = subst(r, this, xterm);
            return false;
        }

        public Term tryMatch(Term x, Term y) {
            matchAll(x, y, true);
            return result;
        }


    }
}
