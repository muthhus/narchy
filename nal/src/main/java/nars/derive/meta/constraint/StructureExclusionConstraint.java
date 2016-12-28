package nars.derive.meta.constraint;

import nars.term.Term;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;


public final class StructureExclusionConstraint implements MatchConstraint {

    private final int structure;

//    public OpNotContainedConstraint(@NotNull Op o) {
//        this(o.bit);
//    }

    public StructureExclusionConstraint(int opVector) {
        this.structure = opVector;
    }

    @Override
    public boolean invalid(@NotNull Term assignee, @NotNull Term value, @NotNull Unify f) {
        return value.hasAny(structure);
    }

    @NotNull
    @Override
    public String toString() {
        return "structureExclusion:" + Integer.toHexString(structure);
    }
}

