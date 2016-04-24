package nars.nal.op;

import nars.Op;
import nars.nal.meta.PremiseEval;
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
    @Override public Term function(@NotNull Compound p, @NotNull PremiseEval r) {
        final Term[] xx = p.terms();
        final Term term = xx[0];
        final Term opT = xx[1];
        final Term x = xx[2];
        if (xx.length < 4) {
            throw new UnsupportedOperationException();
        }
        final Term y = xx[3];

        Op op = substitute.getOp(opT);
        if (op == null)
            //throw new RuntimeException("unrecognizd subst type: " + type);
            return null;

        OneMatchFindSubst omf = new OneMatchFindSubst(op, r, term);
        Term mm = omf.tryMatch(x, y);
        omf.delete();
        return mm;
    }

    private final static class OneMatchFindSubst extends FindSubst {

        private final @NotNull Term xterm;
        private final @NotNull PremiseEval r;
        @Nullable private Term result;

        public OneMatchFindSubst(@NotNull Op op, @NotNull PremiseEval r, @NotNull Term xterm) {
            super(op, r.premise.nar().random, r);
            this.xterm = xterm;
            this.r = r;
        }


        /** terminates after the first match */
        @Override public boolean onMatch() {
            //apply the match before the xy/yx mapping gets reverted after leaving the termutator
            r.replaceAllXY(this);
            result = substitute.resolve(r, r, xterm);

            return false;
        }

        @Nullable
        public Term tryMatch(@NotNull Term x, @NotNull Term y) {
            matchAll(x, y, true);
            return result;
        }

    }
}
