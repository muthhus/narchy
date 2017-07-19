package nars.derive.meta.op;

import nars.$;
import nars.derive.meta.PrediTerm;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 5/21/16.
 */
public final class MatchOneSubtermPrototype extends UnificationPrototype {

    /**
     * either 0 (task) or 1 (belief)
     */
    private final int subterm;

    private final boolean finish;

    public MatchOneSubtermPrototype(@NotNull Compound x, int subterm, boolean finish) {
        super( $.func(subterm==0 ? "task" : "belief", id(x)), x );
        this.subterm = subterm;
        this.finish = finish;
    }

    @NotNull
    @Override
    protected PrediTerm build(PrediTerm eachMatch) {
        return new MatchOneSubterm(id, subterm, pattern, finish ? eachMatch : null);
    }

}
