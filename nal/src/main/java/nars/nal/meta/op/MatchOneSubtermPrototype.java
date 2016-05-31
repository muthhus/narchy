package nars.nal.meta.op;

import com.gs.collections.api.map.ImmutableMap;
import nars.$;
import nars.Op;
import nars.nal.meta.ProcTerm;
import nars.nal.meta.constraint.MatchConstraint;
import nars.term.Term;
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

    public MatchOneSubtermPrototype(@NotNull Term x, @Nullable ImmutableMap<Term, MatchConstraint> constraints, int subterm, boolean finish) {
        super(
                (subterm == 0 ?
                        $.p(id(x, constraints), Op.Imdex) :
                        $.p(Op.Imdex, id(x, constraints))),
                x
                , constraints);
        this.subterm = subterm;
        this.finish = finish;
    }

    @Nullable
    @Override
    protected ProcTerm build(ProcTerm eachMatch) {
        return new MatchOneSubterm(id, subterm, pattern, constraints, finish ? eachMatch : null);
    }

}
