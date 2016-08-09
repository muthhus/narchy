package nars.nal.meta.op;

import nars.nal.meta.BoolCondition;
import nars.nal.meta.PremiseEval;
import nars.task.Task;
import nars.term.Compound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.nal.Tense.DTERNAL;
import static nars.nal.Tense.ETERNAL;

/**
 * Created by me on 7/11/16.
 */
public class IfTermLinkBefore extends events {
    public static final @Nullable BoolCondition ifTermLinkBefore = new IfTermLinkBefore();
    public static final @Nullable BoolCondition ifBeliefBefore = new IfTermLinkBefore() {
        @Override
        public String toString() {
            return "ifBeliefIsBefore";
        }
        @Override
        public boolean requireBelief() {
            return true;
        }

    };

    @NotNull
    @Override
    public String toString() {
        return "ifTermLinkBefore";
    }

    public boolean requireBelief() {
        return false;
    }

    @Override
    public boolean booleanValueOf(@NotNull PremiseEval m) {

        Task belief = m.belief;
        if (belief == null && requireBelief())
            return false;

        Task task = m.task;
        Compound tt = task.term();
        int ttdt = tt.dt();


        if ((belief != null) && (belief.occurrence() != ETERNAL) && (task.occurrence() != ETERNAL)) {
            //only allow a belief if it occurred before or during the task's specified occurrence
            if (belief.occurrence() > task.occurrence())
                return false;
        }

        if ((ttdt == DTERNAL) || (ttdt == 0)) {
            return true;
        } else {

            final int targetMatch;  //must match term
            targetMatch = ttdt < 0 ? 1 : 0;
            return tt.term(targetMatch).equals(m.beliefTerm);
        }
    }

}
