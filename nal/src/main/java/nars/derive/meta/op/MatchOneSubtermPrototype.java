package nars.derive.meta.op;

import nars.Op;
import nars.derive.meta.BoolPred;
import nars.index.term.PatternTermIndex;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 5/21/16.
 */
public final class MatchOneSubtermPrototype extends MatchTermPrototype {

    /**
     * either 0 (task) or 1 (belief)
     */
    private final int subterm;

    private final boolean finish;

    public MatchOneSubtermPrototype(@NotNull Term x, int subterm, boolean finish, @NotNull PatternTermIndex index) {
        super( (Compound)(
                (subterm == 0 ?
                        index.the(Op.PROD, id(x), Op.Imdex) :
                        index.the(Op.PROD, Op.Imdex, id(x)))),
                x
        );
        this.subterm = subterm;
        this.finish = finish;
    }

    @NotNull
    @Override
    protected BoolPred build(BoolPred eachMatch) {
        return new MatchOneSubterm(id, subterm, pattern, finish ? eachMatch : null);
    }

}
