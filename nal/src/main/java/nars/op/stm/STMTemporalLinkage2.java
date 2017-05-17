package nars.op.stm;

import jcog.bag.impl.hijack.PLinkHijackBag;
import jcog.event.On;
import jcog.pri.PLink;
import jcog.pri.RawPLink;
import nars.NAR;
import nars.Task;
import nars.bag.leak.TaskLeak;
import nars.concept.Concept;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static nars.attention.Crosslink.crossLink;

/**
 * Short-term Memory Belief Event Induction.
 * Creates links between sequences of perceived events
 * Empties task buffer when plugin is (re)started.
 *
 */
public class STMTemporalLinkage2 extends TaskLeak<Task,PLink<Task>> {


    @NotNull private final On onReset;
    private final boolean allowNonInput;
    private final int linksPerLeak;
    private final NAR nar;
    float strength = 1f;

    public STMTemporalLinkage2(@NotNull NAR nar, int capacity, int leaksPerCycle, int linksPerLeak) {
        super(new PLinkHijackBag<>(capacity, 2), leaksPerCycle, nar);
        this.nar = nar;
        this.allowNonInput = true;
        this.linksPerLeak = linksPerLeak;

        this.onReset = nar.eventReset.on((n)-> clear());
    }

    @Override
    protected void in(@NotNull Task t, Consumer<PLink<Task>> each) {
        if (t.isBeliefOrGoal() && !t.isEternal()) {
            each.accept(new RawPLink(t,
             t.priSafe(0)
                //t.conf() /* scale by confidence */
            ));
        }
    }


    @Override
    protected float onOut(@NotNull PLink<Task> popped) {
        Task x = popped.get();

        Concept cx = x.concept(nar);
        if (cx==null)
            return 0;

        bag.sample(linksPerLeak, pending -> {
            link(x, cx, pending.get());
            return true;
        });

        return 1f;
    }

    protected void link(Task x, Concept cx, Task y) {

        float xPri = x.priSafe(0);
        crossLink(cx, x, y, strength * xPri, strength * y.priSafe(0), nar);
    }




}
