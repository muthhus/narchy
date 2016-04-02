package nars.op.mental;

import nars.Global;
import nars.NAR;
import nars.concept.Concept;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Terms;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

/**
 * Short-term memory Event Induction.
 * Creates links between sequences of perceived events
 * Empties task buffer when plugin is (re)started.
 */
public final class STMTemporalLinkage implements Consumer<Task> {

    @NotNull public final Deque<Task> stm;

    @NotNull private final NAR nar;

    public STMTemporalLinkage(@NotNull NAR nar) {

        this.nar = nar;

        stm = Global.THREADS == 1 ? new ArrayDeque() : new ConcurrentLinkedDeque<>();

        nar.eventTaskProcess.on(this);
        nar.eventReset.on(n -> stm.clear());

    }

    @Override
    public final void accept(Task task) {
        if (!task.isDeleted())
            inductNext(task);
    }

    public static boolean temporallyInductable(@NotNull Task newEvent) {
        if (newEvent.isInput()) return true;
        //if (Tense.containsMentalOperator(newEvent)) return true;
        return false;
    }



    public boolean inductNext(@NotNull Task t) {


        int stmSize = nar.shortTermMemoryHistory.intValue();


//        if (!currentTask.isTemporalInductable() && !anticipation) { //todo refine, add directbool in task
//            return false;
//        }

        if (t.isEternal() || (!temporallyInductable(t))) {
            return false;
        }

        //new one happened and duration is already over, so add as negative task
        //nal.emit(Events.EventBasedReasoningEvent.class, currentTask, nal);

        //final long now = nal.memory.time();



        int numExtra = Math.max(0, stm.size() - stmSize);

        /** current task's... */
        Concept concept = t.concept(nar);

        Iterator<Task> ss = stm.iterator();

        while (ss.hasNext()) {

            Task previousTask = ss.next();

            if ((numExtra > 0) || (previousTask.isDeleted())) {
                numExtra--;
                ss.remove();
            } else {

                if (Terms.equalSubTermsInRespectToImageAndProduct(previousTask.term(), t.term())) {
                    //the premise would be invalid anyway
                    continue;
                }

                concept.crossLink(t, previousTask, 1f, nar);

            }


        }

        stm.add(t);

        return true;
    }



}
