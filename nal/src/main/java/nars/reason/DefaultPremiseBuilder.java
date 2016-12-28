package nars.reason;

import nars.Task;
import nars.budget.Budget;
import nars.budget.policy.TaskBudgeting;
import nars.concept.Concept;
import nars.nal.Derivation;
import nars.nal.Premise;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 12/26/16.
 */
public class DefaultPremiseBuilder extends PremiseBuilder {

    @Override
    protected @NotNull Premise newPremise(@NotNull Concept c, @NotNull Task task, Term beliefTerm, Task belief, float qua, float pri) {
        return new DefaultPremise(c, task, beliefTerm, belief, pri, qua);
    }

    static class DefaultPremise extends Premise {
        public DefaultPremise(@NotNull Concept c, @NotNull Task task, Term beliefTerm, Task belief, float pri, float qua) {
            super(c, task, beliefTerm, belief, pri, qua);
        }

        @Override
        public @Nullable Budget budget(@NotNull Term conclusion, @Nullable Truth truth, @NotNull Derivation conclude) {
            return TaskBudgeting.derivation(truth, conclusion, conclude);
        }
    }
}
