package nars.op.time;

import nars.Global;
import nars.NAR;
import nars.Symbols;
import nars.concept.Concept;
import nars.task.Task;
import nars.util.data.MutableInteger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Short-term Memory Belief Event Induction.
 * Creates links between sequences of perceived events
 * Empties task buffer when plugin is (re)started.
 */
public final class STMTemporalLinkage extends STM {

    @NotNull public final Deque<Task> stm;


    public STMTemporalLinkage(@NotNull NAR nar, int capacity) {
        super(nar, new MutableInteger(capacity));

        //stm = Global.THREADS == 1 ? new ArrayDeque(this.capacity.intValue()) : new ConcurrentLinkedDeque<>();
        stm = new ArrayDeque<>(capacity);
        //stm = new ConcurrentLinkedDeque<>();

        start();

    }

    @Override
    public void clear() {
        stm.clear();
    }

    @Override
    public final void accept(@NotNull Task t) {

        if (!temporallyInductable(t) || t.punc() != Symbols.BELIEF) {
            return;
        }

        /** current task's... */
        Concept concept = t.concept(nar);
        if (concept == null)
            return;

        int stmSize = capacity.intValue();


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

    }



}
