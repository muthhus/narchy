package nars.nal.op;

import nars.Op;
import nars.nal.meta.PremiseEval;
import nars.term.Compound;
import nars.term.Term;
import nars.term.transform.subst.FindSubst;
import nars.util.version.Versioned;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/** substituteIfUnifies(term, variableType, varFrom, varTo)
 * TODO is this better named "substituteAll"
 * */
public final class substituteIfUnifies extends substitute {

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
            this.matcher = new OneMatchFindSubst(r.premise.nar().random);
        }

        return matcher.tryMatch(substitute.getOp(opT), r, term, x, y);
    }

    public final static class OneMatchFindSubst extends FindSubst {

        private @NotNull Term xterm;
        private @NotNull PremiseEval r;
        @Nullable private Term result;

        public OneMatchFindSubst(Random random) {
            super(null, random); //HACK
        }

        @Override
        public Versioned<Term> get() {
            return r.get();
        }

        /** terminates after the first match */
        @Override public boolean onMatch() {
            //apply the match before the xy/yx mapping gets reverted after leaving the termutator
            r.replaceAllXY(this);
            result = substitute.resolve(r, r, xterm);

            return false;
        }

        @Nullable
        public Term tryMatch(@NotNull Op op, @NotNull PremiseEval r, @NotNull Term xterm, @NotNull Term x, @NotNull Term y) {
            this.type = op;
            this.xterm = xterm;
            this.r = r;
            matchAll(x, y, true);
            clear();
            return result;
        }

    }
}
