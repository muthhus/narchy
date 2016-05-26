package nars.nal.meta.op;

import com.gs.collections.api.map.ImmutableMap;
import nars.$;
import nars.Op;
import nars.nal.meta.PremiseEval;
import nars.nal.meta.constraint.MatchConstraint;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 5/21/16.
 */
public final class MatchOneSubterm extends MatchTerm {

    /**
     * either 0 (task) or 1 (belief)
     */
    private final int subterm;

    private final nars.nal.meta.op.@Nullable MatchOneSubterm callback;

    public MatchOneSubterm(@NotNull Term x, @Nullable ImmutableMap<Term, MatchConstraint> constraints, int subterm, boolean finish) {
        super(
                (subterm == 0 ?
                        $.p(id(x, constraints), Op.Imdex) :
                        $.p(Op.Imdex, id(x, constraints))),
                x
                , constraints);
        this.subterm = subterm;
        this.callback = finish ? this : null;
    }

    @Override
    @Deprecated
    public final boolean booleanValueOf(@NotNull PremiseEval p) {
        p.matchAll(x, p.taskbelief[subterm] /* current term */, callback, constraints);
        return true;
    }
}
