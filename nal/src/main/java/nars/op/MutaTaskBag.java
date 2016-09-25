package nars.op;

import nars.NAR;
import nars.Task;
import nars.bag.Bag;
import nars.link.BLink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * a bag which collects tasks for per-frame processing of some of them
 */
public abstract class MutaTaskBag<B extends BLink>  {

    //private static final Logger logger = LoggerFactory.getLogger(MutaTaskBag.class);
    private final NAR nar;
    private final float selectionRate;
    public final Bag<B> bag;

    public MutaTaskBag(float selectionRate, Bag<B> bag, NAR n) {
        this.bag = bag;
        this.nar = n;
        this.selectionRate = selectionRate;
        n.onTask(task -> {
            B b = filter(task);
            if (b != null)
                bag.put(b);
        });
        n.onFrame(this::next);
    }

    @Nullable
    abstract protected B filter(@NotNull Task task);

    /** next iteration, each frame */
    protected void next(NAR n) {
        int tRemain = (int)Math.ceil(selectionRate * bag.capacity());
        int tRemainStart = tRemain;
        //long start = System.currentTimeMillis();

        bag.commit(); //prepare items for sampling

        while (tRemain > 0) {
            B t = bag.pop();
            if (t == null)
                break; //bag should be empty here

            accept(t);
            tRemain--;
        }

        int processed = tRemainStart - tRemain;
        if (processed > 0) {

            bag.commit(); //clear space until next cycle

            //long end = System.currentTimeMillis();
            //float time = (end-start)/1000f;
            //logger.info("buffer size=" + bag.size() + "/cap=" + bag.capacity() + " -=> " + processed + " in " + n2(time) + " sec");
        }
    }

    abstract protected void accept(B b);


}
