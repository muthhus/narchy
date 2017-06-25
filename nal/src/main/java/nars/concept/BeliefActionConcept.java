package nars.concept;

import nars.NAR;
import nars.Task;
import nars.task.NALTask;
import nars.term.Compound;
import nars.truth.DiscreteTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.stream.Stream;

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

//    @Override
//    public @Nullable Task curiosity(float conf, long next, NAR nar) {
//        return ActionConcept.curiosity(term(), BELIEF, conf, next, nar);
//    }

    @Override
    public Stream<Task> apply(NAR nar) {

        int dur = nar.dur();
        long now = nar.time();

        Truth belief =
                belief(now, dur, nar);
                //beliefIntegrated.commitAverage();
        action.accept(belief);

        Truth goal =
                goal(now, dur, nar);
                //goalIntegrated.commitAverage();
        if (goal!=null) {
            //allow any goal desire to influence belief to some extent
            float rate = 1f;
            DiscreteTruth t = new DiscreteTruth(goal.freq(), goal.conf() * rate);
            if (t!=null) {
                return Stream.of(new NALTask(term(), BELIEF, t, now, now, (now + dur), new long[]{nar.time.nextStamp()}));
            }
        }


        return Stream.empty();
    }


}
