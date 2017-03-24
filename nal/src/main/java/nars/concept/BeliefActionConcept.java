package nars.concept;

import nars.$;
import nars.NAR;
import nars.Task;
import nars.task.ImmutableTask;
import nars.term.Compound;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static nars.Op.BELIEF;

/**
 * Action Concept which acts on belief state directly (no separate feedback involved)
 */
public class BeliefActionConcept extends ActionConcept {



    private final Consumer<Truth> action;


    public BeliefActionConcept(@NotNull Compound term, @NotNull NAR n, Consumer<Truth> action) {
        super(term, n);
        this.action = action;
    }

    @Override
    public @Nullable Task curiosity(float conf, NAR nar) {
        return GoalActionConcept.curiosity(term(), BELIEF, conf, nar);
    }

    @Override
    public Task apply(NAR nar) {

        Truth belief = beliefIntegrated.commitAverage();
        action.accept(belief);

        Truth goal = goalIntegrated.commitAverage();
        if (goal!=null) {
            //allow any goal desire to influence belief to some extent
            float rate = 1f;
            Truth t = $.t(goal.freq(), goal.conf() * rate);
            if (t!=null) {
                long now = nar.time();
                int dur = nar.dur();
                return new ImmutableTask(term(), BELIEF, t, now, now, (now + dur), new long[]{nar.time.nextStamp()});
            }
        }


        return null;
    }


}
