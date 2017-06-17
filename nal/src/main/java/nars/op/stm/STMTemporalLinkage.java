package nars.op.stm;

import com.conversantmedia.util.concurrent.DisruptorBlockingQueue;
import jcog.data.FloatParam;
import jcog.data.MutableInteger;
import jcog.pri.PLink;
import jcog.pri.Pri;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.task.NALTask;
import nars.term.Term;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

import static nars.Op.COMMAND;


/**
 * Short-term Memory Belief Event Induction.
 * Creates links between sequences of perceived events
 * Empties task buffer when plugin is (re)started.
 */
public final class STMTemporalLinkage extends STM {

    @NotNull
    public final DisruptorBlockingQueue<Task> stm;

    final FloatParam strength = new FloatParam(1f, 0f, 1f);


    public STMTemporalLinkage(@NotNull NAR nar, int capacity) {
        this(nar, capacity, false);
    }

    public STMTemporalLinkage(@NotNull NAR nar, int capacity, boolean allowNonInput) {
        super(nar, new MutableInteger(capacity));

        this.allowNonInput = allowNonInput;
        strength.setValue(1f / capacity);

        //stm = Global.THREADS == 1 ? new ArrayDeque(this.capacity.intValue()) : new ConcurrentLinkedDeque<>();
        //stm = new ArrayDeque<>(capacity);
        stm = new DisruptorBlockingQueue<Task>(capacity);
        for (int i = 0; i < capacity; i++) {
            stm.add(null);
        }

        //stm = new ConcurrentLinkedDeque<>();
        //this.in = nar.in.stream(this);
    }

    @Override
    public void clear() {
        stm.clear();
    }

    @Override
    public final void accept(@NotNull Task t) {

        if (!t.isBeliefOrGoal()) {
            return;
        }


        int stmCapacity = capacity.intValue();


        Term tt = t.term();


        List<Task> queued;
        /*synchronized (stm)*/

        float strength = this.strength.floatValue();
        float tPri = t.priSafe(0);

        Iterator<Task> ss = stm.iterator();
        while (ss.hasNext()) {
            Task u = ss.next();
            if (u == null) continue; //skip null's and dummy's
            link(t, strength, tPri, u);
        }

        stm.poll();
        stm.offer(t);
    }


    protected void link(@NotNull Task t, float strength, float tPri, Task u) {


        /** current task's... */
        float interStrength = tPri * u.priSafe(0) * strength;
        if (interStrength >= Pri.EPSILON) {
            Concept ac = t.concept(nar);
            if (ac != null) {
                Concept bc = u.concept(nar);
                if (bc != null && !bc.equals(ac)) { //null or same concept?

                    //TODO handle overflow?
                    bc.termlinks().put(new PLink(ac, interStrength));
                    ac.termlinks().put(new PLink(bc, interStrength));

                    //tasklinks, not sure:
                    //        tgtConcept.tasklinks().put( new RawPLink(srcTask, scaleSrcTgt));
                    //        srcConcept.tasklinks().put( new RawPLink(tgtTask, scaleTgtSrc));

                }
            }

            //in.input(s); //<- spams the executor
        }
    }
}