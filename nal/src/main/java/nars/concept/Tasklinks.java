package nars.concept;

import jcog.pri.PLinkUntilDeleted;
import jcog.pri.Prioritized;
import nars.NAR;
import nars.Task;

import java.util.Collection;

public class Tasklinks {

    public static void linkTask(Task t, float activationApplied, Concept cc, NAR nar) {


        cc.tasklinks().putAsync(
                new PLinkUntilDeleted<>(t, activationApplied)
                //new PLink<>(t, activation)
        );

        if (activationApplied >= Prioritized.EPSILON_VISIBLE) {
            nar.eventTask.emit(t);
        }

        float conceptActivation = activationApplied * nar.evaluate(t.cause());

        nar.emotion.onActivate(t, conceptActivation, cc, nar);

        nar.activate(cc, conceptActivation);

    }

    public static void linkTask(Task task, Collection<Concept> targets) {
        int numSubs = targets.size();
        if (numSubs == 0)
            return;

        float tfa = task.priElseZero();
        float tfaEach = tfa / numSubs;


        for (Concept target : targets) {

            target.tasklinks().putAsync(
                    new PLinkUntilDeleted(task, tfaEach)
            );
//                target.termlinks().putAsync(
//                        new PLink(task.term(), tfaEach)
//                );


        }
    }
}
