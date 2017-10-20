package nars.bag.leak;

import jcog.data.FloatParam;
import nars.NAR;
import nars.control.CauseChannel;
import nars.task.ITask;

/** LeakOut subclass which has support for a return input channel
 * whose value adjusts the throttle rate of the Leak. */
abstract public class LeakBack extends TaskLeak {

    final static float INITIAL_RATE = 1f;

    //HACK
    public final FloatParam boost = new FloatParam(1f, 0, 2f);

    private final CauseChannel<ITask> out;

    public LeakBack(int capacity, NAR nar) {
        super(capacity, INITIAL_RATE, nar);
        this.out = nar.newCauseChannel(this);
    }

    public void feedback(ITask x) {
        out.input(x);
    }
    

    @Override public float value() {
        return out.value();
    }
}
