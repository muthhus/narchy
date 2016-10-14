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
public abstract class Leak<B>  {

    //private static final Logger logger = LoggerFactory.getLogger(MutaTaskBag.class);
    public final MutableFloat rate;
    public final Bag<B> bag;

    public Leak(Bag<B> bag, float rate, NAR n) {
         this(bag, new MutableFloat(rate), n);
    }

    public Leak(@NotNull Bag<B> bag, @NotNull MutableFloat rate, NAR n) {
        this.bag = bag;
        this.rate = rate;
        n.onTask(task -> {
            input(task, bag::putLink);
        });
        n.onFrame(this::next);
    }

    /** transduce an input to a series of created BLink's to be inserted */
    @Nullable abstract protected void input(@NotNull Task task, Consumer<BLink<B>> each);

    /** returns a value, in relation to the 'rate' parameter, which is subtracted
     * from the rate each iteration. this can allow proportional consumption of
     * a finitely allocated resource.
     */
    abstract protected float accept(@NotNull BLink<B> b);

    /** next iteration, each frame */
    protected void next(NAR nar) {

        bag.commit();

        for (float r = rate.floatValue();
             (r > 0) && !bag.isEmpty() && ((r >= 1f) || (nar.random.nextFloat() < r));
             ) {
            @Nullable BLink<B> t = bag.pop();
            if (t!=null) {
                accept(t);
                r-=1f;
            }
        }

    }




}
