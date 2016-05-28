package nars.nal.meta.constraint;

import nars.term.Term;
import nars.term.subst.FindSubst;
import org.jetbrains.annotations.NotNull;


@FunctionalInterface
public interface MatchConstraint {
    /**
     *
     * @param assignee X variable
     * @param value Y value
     * @param f match context
     * @return true if match is INVALID, false if VALID (reversed)
     */
    boolean invalid(@NotNull Term assignee, @NotNull Term value, @NotNull FindSubst f);
}
