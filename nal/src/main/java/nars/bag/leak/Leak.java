package nars.bag.leak;

import jcog.bag.Bag;
import jcog.bag.PLink;
import jcog.event.On;
import nars.NAR;
import nars.Task;
import nars.budget.Budget;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;


/**
 * interface for controlled draining of a bag
 */
public abstract class Leak</* TODO: A, */X, V extends PLink<X>> implements Consumer<Task> {

    //private static final Logger logger = LoggerFactory.getLogger(MutaTaskBag.class);
    @NotNull
    public final MutableFloat rate;
    @NotNull
    public final Bag<X,V> bag;
    private final On onTask, onReset, onCycle;
    private long last;

    public Leak(@NotNull Bag<X,V> bag, float rate, @NotNull NAR n) {
         this(bag, new MutableFloat(rate), n);
    }


    /**
     * rate = max successful leaks per duration
     */
    public Leak(@NotNull Bag<X,V> bag, @NotNull MutableFloat rate, @NotNull NAR n) {
        this.bag = bag;
        this.rate = rate;
        onTask = n.onTask(this);

        //TODO make Weak
        onReset = n.onReset((nn)->bag.clear());
        onCycle = n.onCycle(this::next);
    }

    @Override
    public void accept(Task task) {
        try {
            in(task, bag::put);
        } catch (Budget.BudgetException e) {
            //was deleted before the link could be made
        }
    }

    /** transduce an input to a series of created BLink's to be inserted */
    abstract protected void in(@NotNull Task task, Consumer<V> each);

    /** returns a cost value, in relation to the 'rate' parameter, which is subtracted
     * from the rate each iteration. this can allow proportional consumption of
     * a finitely allocated resource.
     */
    abstract protected float onOut(@NotNull V b);

    /** next iteration, each frame */
    protected void next(@NotNull NAR nar) {

        bag.commit();

        long last = this.last;
        long now = nar.time();

        if (now == last)
            return; //no time yet

        float durDelta = Math.min(1f, (now - last) / nar.dur()); //limit to one in case of lag

        boolean leaked = false;
        //for each full integer = 1 instanceof a 100% prob selection
        // each fraction of an integer = some probability of a next one occurring
        for (float r = rate.floatValue() * durDelta;
             (r > 0) &&
             bag.size() >= minSizeForLeak() &&
             ((r >= 1) || ((r < 1f) && (nar.random.nextFloat() < r)));
             ) {
            @Nullable V t = bag.pop();
            if (t!=null) {
                float cost = onOut(t);
                r -= cost;
                leaked = true;
            }
        }

        if (leaked)
            this.last = now;

    }

    /** minimum bag size allowed before leak */
    public int minSizeForLeak() {
        return 1;
    }


    public void setCapacity(int capacity) {
        bag.setCapacity(capacity);
    }

    public void clear() {
        bag.clear();
    }

    public void stop() {
        onTask.off();
        onReset.off();
        onCycle.off();
    }
}
