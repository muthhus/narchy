package nars.op;

import nars.NAR;
import nars.Task;
import nars.bag.Bag;
import nars.bag.impl.CurveBag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

import static nars.util.Texts.n2;


/**
 * a bag which collects tasks for per-frame processing of some of them
 */
public class MutaTaskBag  {

    private static final Logger logger = LoggerFactory.getLogger(MutaTaskBag.class);
    private final NAR nar;
    private final float selectionRate;
    private final BiConsumer<Task,NAR> model;
    private final Bag<Task> bag;



    public MutaTaskBag(BiConsumer<Task, NAR> model, float selectionRate, CurveBag<Task> bag, NAR n) {
        this.bag = bag;
        this.nar = n;
        this.selectionRate = selectionRate;
        this.model = model;
        n.onTask(task -> {
            if (task instanceof VarIntroduction.VarIntroducedTask)
                return; //HACK dont process its own feedback
            bag.put(task);
        });
        n.onFrame(this::next);
    }

    /** next iteration, each frame */
    protected void next(NAR n) {
        int tRemain = (int)Math.ceil(selectionRate * bag.capacity());
        int tRemainStart = tRemain;
        //long start = System.currentTimeMillis();

        bag.commit(); //prepare items for sampling

        while (tRemain > 0) {
            Task t = bag.pop();
            if (t == null)
                break; //bag should be empty here

            model.accept(t, nar);
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



}
