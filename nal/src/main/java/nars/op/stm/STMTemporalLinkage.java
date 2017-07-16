package nars.op.stm;

import com.conversantmedia.util.concurrent.DisruptorBlockingQueue;
import jcog.data.FloatParam;
import jcog.data.MutableInteger;
import jcog.pri.PLink;
import jcog.pri.Pri;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import org.jetbrains.annotations.NotNull;


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

        stm = new DisruptorBlockingQueue<>(capacity+1 /* extra slot for good measure */);
        for (int i = 0; i < capacity+1; i++)
            stm.add(null); //fill with nulls initially


    }

    @Override
    public void clear() {
        stm.clear();
    }

    @Override
    public final void accept(@NotNull Task t) {

        if (!t.isBeliefOrGoal())
            return;

        float strength = this.strength.floatValue();
        float tPri = t.priSafe(0);
        if (tPri == 0)
            return;

        for (Task u : stm) {
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
            Concept ac = t.concept(nar, false);
            if (ac != null) {
                Concept bc = u.concept(nar, false);
                if (bc != null && !bc.equals(ac)) { //null or same concept?

                    //TODO handle overflow?
                    bc.termlinks().put(new PLink(ac.term(), interStrength));
                    ac.termlinks().put(new PLink(bc.term(), interStrength));

                    //tasklinks, not sure:
                    //        tgtConcept.tasklinks().put( new RawPLink(srcTask, scaleSrcTgt));
                    //        srcConcept.tasklinks().put( new RawPLink(tgtTask, scaleTgtSrc));

                }
            }

            //in.input(s); //<- spams the executor
        }
    }
}