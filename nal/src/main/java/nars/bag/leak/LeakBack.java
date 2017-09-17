package nars.bag.leak;

import nars.NAR;
import nars.Task;
import nars.control.CauseChannel;

/** LeakOut subclass which has support for a return input channel
 * whose value adjusts the throttle rate of the Leak. */
abstract public class LeakBack extends TaskLeak {

    final static float INITIAL_RATE = 1f;

    private final CauseChannel<Task> out;

    public LeakBack(int capacity, NAR nar) {
        super(capacity, INITIAL_RATE, nar);
        this.out = nar.newCauseChannel(this);
    }

    public void feedback(Task x) {
        out.input(x);
    }

    @Override
    protected void run(NAR nar) {

        float g = out.gain();
        inputRate( g );
        //System.out.println(this + " " + g);

        super.run(nar);
    }
}
