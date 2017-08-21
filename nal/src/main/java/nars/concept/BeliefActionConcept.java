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
    public Stream<Task> update(long now, int dur, NAR nar) {

        long nowStart = now - dur/2;
        long nowEnd = now + dur/2;

        Truth belief =
                this.beliefs().truth(nowStart, nowEnd, nar);
                //beliefIntegrated.commitAverage();
        action.accept(belief);

        Truth goal =
                this.goals().truth(nowStart, nowEnd, nar);
                //goalIntegrated.commitAverage();
        if (goal!=null) {
            //allow any goal desire to influence belief to some extent
            float rate = 1f;
            DiscreteTruth t = new DiscreteTruth(goal.freq(), goal.conf() * rate);
            if (t!=null) {
                NALTask y = new NALTask(term(), BELIEF, t, now, nowStart, nowEnd, new long[]{nar.time.nextStamp()});
                y.pri(nar.priorityDefault(BELIEF));
                return Stream.of(y);
            }
        }


        return Stream.empty();
    }


}
