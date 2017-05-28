package nars.op.stm;

import jcog.data.FloatParam;
import jcog.data.MutableInteger;
import jcog.pri.Pri;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import static nars.attention.Crosslink.crossLink;

/**
 * Short-term Memory Belief Event Induction.
 * Creates links between sequences of perceived events
 * Empties task buffer when plugin is (re)started.
 */
public final class STMTemporalLinkage extends STM {

    @NotNull
    public final Deque<Task> stm;

    final FloatParam strength = new FloatParam(1f, 0f, 1f);

    public STMTemporalLinkage(@NotNull NAR nar, int capacity) {
        super(nar, new MutableInteger(capacity));

        allowNonInput = false;
        strength.setValue( 1f/capacity );

        //stm = Global.THREADS == 1 ? new ArrayDeque(this.capacity.intValue()) : new ConcurrentLinkedDeque<>();
        stm = new ArrayDeque<>(capacity);
        //stm = new ConcurrentLinkedDeque<>();
    }

    @Override
    public void clear() {
        synchronized (stm) {
            stm.clear();
        }
    }

    @Override
    public final void accept(@NotNull Task t) {

        if (!t.isBeliefOrGoal()) {
            return;
        }


        int stmCapacity = capacity.intValue();


//        if (!currentTask.isTemporalInductable() && !anticipation) { //todo refine, add directbool in task
//            return false;
//        }

        //new one happened and duration is already over, so add as negative task
        //nal.emit(Events.EventBasedReasoningEvent.class, currentTask, nal);

        //final long now = nal.memory.time();


        Term tt = t.unneg();


        List<Task> queued;
        synchronized (stm) {
            int s = stm.size();
            if (s > 0) {
                queued = $.newArrayList(s);
                int numExtra = Math.max(0, (s) - stmCapacity);

                Iterator<Task> ss = stm.iterator();
                while (ss.hasNext()) {

                    Task previousTask = ss.next();

                    if ((numExtra > 0) || (previousTask.isDeleted())) {
                        numExtra--;
                        ss.remove();
                    } else {
                        if (!tt.equals(previousTask.unneg()))
                            queued.add(previousTask);
                    }


                }
            } else {
                queued = null;
            }

            stm.add(t);
        }

        if (queued != null) {

            float strength = this.strength.floatValue();
            float tPri = t.priSafe(0);
            if (tPri > 0) {
                for (int i = 0, queuedSize = queued.size(); i < queuedSize; i++) {
                    Task u = queued.get(i);
                    /** current task's... */
                    Concept concept = t.concept(nar);
                    if (concept != null) {
                        float interStrength= tPri * u.priSafe(0) * strength;
                        if (interStrength > Pri.EPSILON)
                            crossLink(concept, t, u, interStrength, interStrength, nar);
                    }
                }
            }
        }

    }


}
