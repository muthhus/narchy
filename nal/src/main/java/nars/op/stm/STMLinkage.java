package nars.op.stm;

import com.conversantmedia.util.concurrent.DisruptorBlockingQueue;
import jcog.data.FloatParam;
import jcog.pri.PLink;
import jcog.pri.Pri;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.control.TaskService;
import org.jetbrains.annotations.NotNull;


/**
 * Short-term Memory Belief Event Induction.
 * Creates links between sequences of perceived events
 * Empties task buffer when plugin is (re)started.
 */
public final class STMLinkage extends TaskService {

    @NotNull
    public final DisruptorBlockingQueue<Task> stm;

    final FloatParam strength = new FloatParam(1f, 0f, 1f);
    private final boolean allowNonInput;

    public STMLinkage(@NotNull NAR nar, int capacity) {
        this(nar, capacity, false);
    }

    public STMLinkage(@NotNull NAR nar, int capacity, boolean allowNonInput) {
        super(nar);

        this.allowNonInput = allowNonInput;
        strength.setValue(1f / capacity);

        stm = new DisruptorBlockingQueue<>(capacity+1 /* extra slot for good measure */);
        for (int i = 0; i < capacity+1; i++)
            stm.add(null); //fill with nulls initially


    }

    static boolean stmLinkable(@NotNull Task newEvent, boolean allowNonInput) {
        return ( !newEvent.isEternal() && (allowNonInput || newEvent.isInput()));
    }

    @Override
    public void clear() {
        stm.clear();
    }

    @Override
    public final void accept(@NotNull Task t) {

        if (!t.isBeliefOrGoal())
            return;
        if (!STMLinkage.stmLinkable(t, allowNonInput))
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


    protected void link(@NotNull Task ta, float strength, float tPri, Task tb) {


        /** current task's... */
        float interStrength = tPri * tb.priSafe(0) * strength;
        if (interStrength >= Pri.EPSILON) {
            Concept ca = ta.concept(nar, false);
            if (ca != null) {
                Concept cb = tb.concept(nar, false);
                if (cb != null && !cb.equals(ca)) { //null or same concept?

                    //TODO handle overflow?
                    cb.termlinks().put(new PLink(ca.term(), interStrength));
                    ca.termlinks().put(new PLink(cb.term(), interStrength));

                    //tasklinks, not sure:
                    cb.tasklinks().putAsync( new PLink<>(ta, interStrength));
                    ca.tasklinks().putAsync( new PLink<>(tb, interStrength));

                }
            }

            //in.input(s); //<- spams the executor
        }
    }
}