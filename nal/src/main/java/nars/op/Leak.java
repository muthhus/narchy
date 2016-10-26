package nars.op;

import nars.NAR;
import nars.Task;
import nars.bag.Bag;
import nars.link.BLink;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;


/**
 * interface for controlled draining of a bag
 */
public abstract class Leak</* TODO: A, */B>  {

    //private static final Logger logger = LoggerFactory.getLogger(MutaTaskBag.class);
    @NotNull
    public final MutableFloat rate;
    @NotNull
    public final Bag<B> bag;

    public Leak(@NotNull Bag<B> bag, float rate, @NotNull NAR n) {
         this(bag, new MutableFloat(rate), n);
    }

    public Leak(@NotNull Bag<B> bag, @NotNull MutableFloat rate, @NotNull NAR n) {
        this.bag = bag;
        this.rate = rate;
        n.onTask(task -> {
            in(task, bag::putLink);
        });
        n.onFrame(this::next);
    }

    /** transduce an input to a series of created BLink's to be inserted */
    abstract protected void in(@NotNull Task task, Consumer<BLink<B>> each);

    /** returns a value, in relation to the 'rate' parameter, which is subtracted
     * from the rate each iteration. this can allow proportional consumption of
     * a finitely allocated resource.
     */
    abstract protected float onOut(@NotNull BLink<B> b);

    /** next iteration, each frame */
    protected void next(@NotNull NAR nar) {

        bag.commit();

        //for each full integer = 1 instanceof a 100% prob selection
        // each fraction of an integer = some probability of a next one occurring
        for (float r = rate.floatValue();
             (r > 0) &&
             !bag.isEmpty() &&
             ((r >= 1) || ((r < 1f) && (nar.random.nextFloat() < r)));
             ) {
            @Nullable BLink<B> t = bag.pop();
            if (t!=null) {
                onOut(t);
            }
            r-=1f;
        }

    }




}
