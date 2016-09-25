package nars.term.subst;

import nars.Op;
import nars.index.TermIndex;
import nars.nal.meta.PremiseEval;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Created by me on 5/24/16.
 */
public final class OneMatchFindSubst extends FindSubst {

    private @Nullable Term xterm;
    private @Nullable PremiseEval target;

    @Nullable private Term result;


    public OneMatchFindSubst(TermIndex index, Op type, Random r) {
        super(index, type, r);
    }

    public OneMatchFindSubst(FindSubst parent, @Nullable Op type) {
        this(parent.index, type, parent.random);
    }


    /**
     * terminates after the first match
     */
    @Override
    public boolean onMatch() {
        //apply the match before the xy/yx mapping gets reverted after leaving the termutator
        if (xterm != null) {
            if (target != null) {
                target.replaceAllXY(this);
                result = target.resolve(xterm, target);
            } else {
                result = resolve(xterm, this);
            }
        }
        return false;
    }

    public boolean tryMatch(@NotNull Term x, @NotNull Term y) {
        tryMatch(null, null, x, y);
        return !xy.isEmpty();
    }

    @Nullable
    public Term tryMatch(@Nullable PremiseEval target, @Nullable Term xterm, @NotNull Term x, @NotNull Term y) {
        this.xterm = xterm;
        this.target = target;
        this.result = null;
        unify(x, y, true, true);
        return result;
    }

}
