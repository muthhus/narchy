package nars.op;

import nars.NAR;
import nars.Task;
import jcog.bag.Bag;
import jcog.bag.PLink;
import nars.budget.Budget;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;


/**
 * interface for controlled draining of a bag
 */
public abstract class Leak</* TODO: A, */X, V extends PLink<X>>  {

    //private static final Logger logger = LoggerFactory.getLogger(MutaTaskBag.class);
    @NotNull
    public final MutableFloat rate;
    @NotNull
    public final Bag<X,V> bag;

    public Leak(@NotNull Bag<X,V> bag, float rate, @NotNull NAR n) {
         this(bag, new MutableFloat(rate), n);
    }

    public Leak(@NotNull Bag<X,V> bag, @NotNull MutableFloat rate, @NotNull NAR n) {
        this.bag = bag;
        this.rate = rate;
        n.onTask(task -> {
            try {
                in(task, bag::put);
            } catch (Budget.BudgetException e) {
                //was deleted before the link could be made
            }
        });
        n.onReset((nn)->bag.clear());
        n.onCycle(this::next);
    }

    /** transduce an input to a series of created BLink's to be inserted */
    abstract protected void in(@NotNull Task task, Consumer<V> each);

    /** returns a cost value, in relation to the 'rate' parameter, which is subtracted
     * from the rate each iteration. this can allow proportional consumption of
     * a finitely allocated resource.
     */
    abstract protected float onOut(@NotNull V b);

    /** next iteration, each frame */
    protected final void next(@NotNull NAR nar) {

        bag.commit();

        //for each full integer = 1 instanceof a 100% prob selection
        // each fraction of an integer = some probability of a next one occurring
        for (float r = rate.floatValue();
             (r > 0) &&
             !bag.isEmpty() &&
             ((r >= 1) || ((r < 1f) && (nar.random.nextFloat() < r)));
             ) {
            @Nullable V t = bag.pop();
            if (t!=null) {
                float cost = onOut(t);
                r -= cost;
            }
        }

    }


    public void setCapacity(int capacity) {
        bag.setCapacity(capacity);
    }

    public void clear() {
        bag.clear();
    }
}
