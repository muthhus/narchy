package nars.term.subst;

import nars.$;
import nars.Op;
import nars.Param;
import nars.index.term.TermIndex;
import nars.premise.Derivation;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Less powerful one-match only unification
 */
public final class SubUnify extends Unify {

    private @Nullable Term xterm;
    private @Nullable Derivation target;

    @Nullable private Term result;
    int retries = Param.SubUnificationMatchRetries;


    public SubUnify(TermIndex index, Op type, Random r) {
        super(index, type, r, Param.SubUnificationStackMax, Param.SubUnificationTermutesMax);
    }

    public SubUnify(@NotNull Unify parent, @Nullable Op type) {
        this(parent.index, type, parent.random);
    }


    /**
     * terminates after the first match
     */
    @Override
    public boolean onMatch() {
        //apply the match before the xy/yx mapping gets reverted after leaving the termutator
        if (xterm != null) {
            Subst s;
            if (target != null) {
                target.replaceAllXY(this);
                s = target;
            } else {
                s = this;
            }
            result = transform(xterm, s);
        }

        return (result == null) && --retries > 0;

    }

    public boolean tryMatch(@NotNull Term x, @NotNull Term y) {
        tryMatch(null, null, x, y);
        return !xy.isEmpty();
    }

    @Nullable
    public Term tryMatch(@Nullable Derivation target, @Nullable Term xterm, @NotNull Term x, @NotNull Term y) {
        this.xterm = xterm;
        this.target = target;
        this.result = null;
        unify(x, y, true, true);
        return result;
    }

}
