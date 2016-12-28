package nars.derive.meta.op;

import nars.derive.meta.AtomicBoolCondition;
import nars.derive.meta.BoolCondition;
import nars.derive.meta.constraint.MatchConstraint;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 5/26/16.
 */
abstract public class MatchTerm extends AtomicBoolCondition {


//    @NotNull
//    private final Term id;

    @NotNull
    public final Term pattern;

    public final @Nullable MatchConstraint constraints;

    public final @Nullable BoolCondition eachMatch;

    private final String idString;

    public MatchTerm(@NotNull Term id, @NotNull Term pattern, @Nullable MatchConstraint constraints, @Nullable BoolCondition eachMatch) {
        //this.pid = pid;
        //this.id = id;
        this.idString = id.toString();
        this.pattern = pattern;
        this.constraints = constraints;
        this.eachMatch = eachMatch;
    }

    @Override
    public @NotNull final String toString() {
        return idString;
    }
}
