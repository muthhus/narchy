package nars.nal.meta.op;

import nars.Op;
import nars.index.PatternIndex;
import nars.nal.meta.BoolCondition;
import nars.nal.meta.constraint.MatchConstraint;
import nars.term.Term;
import org.eclipse.collections.api.map.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 5/21/16.
 */
public final class MatchOneSubtermPrototype extends MatchTermPrototype {

    /**
     * either 0 (task) or 1 (belief)
     */
    private final int subterm;

    private final boolean finish;

    public MatchOneSubtermPrototype(@NotNull Term x, @Nullable ImmutableMap<Term, MatchConstraint> constraints, int subterm, boolean finish, @NotNull PatternIndex index) {
        super(
                (subterm == 0 ?
                        index.the(Op.PROD, id(index, x, constraints), Op.Imdex) :
                        index.the(Op.PROD, Op.Imdex, id(index, x, constraints))),
                x
                , constraints);
        this.subterm = subterm;
        this.finish = finish;
    }

    @NotNull
    @Override
    protected BoolCondition build(BoolCondition eachMatch) {
        return new MatchOneSubterm(id, subterm, pattern, constraints, finish ? eachMatch : null, matchFactor);
    }

}
