package nars.term.subst;

import nars.NAR;
import nars.Op;
import nars.nal.meta.PremiseEval;
import nars.nal.op.substitute;
import nars.term.Term;
import nars.util.version.Versioned;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 5/24/16.
 */
public final class OneMatchFindSubst extends FindSubst {

    private @NotNull Term xterm;
    private @NotNull PremiseEval r;
    @Nullable
    private Term result;

    public OneMatchFindSubst(@NotNull NAR nar) {
        super(nar.index, null, nar.random); //HACK
    }

    @Override
    public Versioned<Term> get() {
        return r.get();
    }

    /**
     * terminates after the first match
     */
    @Override
    public boolean onMatch() {
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
