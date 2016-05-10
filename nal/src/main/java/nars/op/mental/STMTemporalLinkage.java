package nars.op.mental;

import nars.Global;
import nars.NAR;
import nars.Symbols;
import nars.concept.Concept;
import nars.task.Task;
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

    final boolean beliefs = true;
    final boolean goals = false;

    public STMTemporalLinkage(@NotNull NAR nar) {

        this.nar = nar;

        stm = Global.THREADS == 1 ? new ArrayDeque(capacity()) : new ConcurrentLinkedDeque<>();

        nar.eventTaskProcess.on(this);
        nar.eventReset.on(n -> stm.clear());

    }

    public int capacity() {
        return nar.shortTermMemoryHistory.intValue();
    }


    public boolean temporallyInductable(@NotNull Task newEvent) {
        if ((!newEvent.isDeleted() && newEvent.isInput() && !newEvent.isEternal())) {
            switch (newEvent.punc()) {
                case Symbols.BELIEF: return beliefs;
                case Symbols.GOAL: return goals;
            }
        }
        return false;

        //if (Tense.containsMentalOperator(newEvent)) return true;
    }


    @Override
    public final void accept(@NotNull Task t) {

        if (!temporallyInductable(t)) {
            return;
        }

        /** current task's... */
        Concept concept = t.concept(nar);
        if (concept == null)
            return;

        int stmSize = capacity();


//        if (!currentTask.isTemporalInductable() && !anticipation) { //todo refine, add directbool in task
//            return false;
//        }

        //new one happened and duration is already over, so add as negative task
        //nal.emit(Events.EventBasedReasoningEvent.class, currentTask, nal);

        //final long now = nal.memory.time();



        int numExtra = Math.max(0, stm.size() - stmSize);


        Iterator<Task> ss = stm.iterator();

        while (ss.hasNext()) {

            Task previousTask = ss.next();

            if ((numExtra > 0) || (previousTask.isDeleted())) {
                numExtra--;
                ss.remove();
            } else {

                //is this valid ?
//                if (Terms.equalSubTermsInRespectToImageAndProduct(previousTask.term(), t.term())) {
//                    //the premise would be invalid anyway
//                    continue;
//                }

                if (previousTask.term().equals(t.term()))
                    continue;

                float strength =
                        1f;
                        //t.conf() * previousTask.conf(); //scale strength of the tasklink by the confidence intersection

                if (strength > 0)
                    concept.crossLink(t, previousTask, strength, nar);

            }


        }

        stm.add(t);

        return;
    }



}
