package nars.term.subst;

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
public class SubUnify extends Unify {

    private @Nullable Term xterm;
    private @Nullable Derivation target;

    @Nullable private Term result;


    public SubUnify(TermIndex index, Op type, Random r, int ttl) {
        super(index, type, r, Param.SubUnificationStackMax, ttl);
    }

    public SubUnify(@NotNull Unify parent, @Nullable Op type) {
        super(parent.index, type, parent.random, parent.versioning);
    }


    /**
     * terminates after the first match
     */
    @Override
    public boolean onMatch() {
        //apply the match before the xy/yx mapping gets reverted after leaving the termutator
        int start = target!=null ? target.now() : -1;

        boolean fail = false;
        final Unify s = target!=null ? target : this;
        if (xterm != null) {

            if (target != null) {
                if (!target.put(this)) {
                    fail = true;
                }
            }

            if (!fail) {
                result = s.transform(xterm, index);
                if (result == null)
                    fail = true;
            }
        } else {
            return false; //??
        }


        if (fail) {
            s.revert(start);
            return s.live(); //try again if ttl > 0
        } else {
            return false; //success, done
        }
    }

    public void tryMatch(@NotNull Term x, @NotNull Term y) {
        this.xterm = null;
        this.target = null;
        this.result = null;
        unify(x, y, true, true);
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
