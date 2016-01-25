package nars.nal.op;

import nars.nal.meta.PremiseMatch;
import nars.term.Compound;
import nars.term.Term;
import nars.term.transform.subst.FindSubst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

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

        FindSubst umap = unifies(op, x, y, r.premise.memory().random);

        return ((umap != null) && (!umap.isEmpty())) ?
                subst(r, umap, term) :
                term;
    }

    static FindSubst unifies(@NotNull Term op, @NotNull Term x, @NotNull Term y, Random rng) {

        OneMatchFindSubst sub = new OneMatchFindSubst(op, rng);
        boolean matched = sub.tryMatch(x, y);

        if (matched) {
            return sub;
        }
        return null;


    }

    private final static class OneMatchFindSubst extends FindSubst {

        private boolean matched = false;

        public OneMatchFindSubst(Term op, Random rng) {
            super(substitute.getOp(op), rng);
        }

        /** terminates after the first match */
        @Override public boolean onMatch() {
            matched = true;
            return false;
        }

        public boolean tryMatch(Term x, Term y) {
            matchAll(x, y, true);
            return matched;
        }
    }
}
