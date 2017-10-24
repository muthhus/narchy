package nars.exe;

import jcog.event.Ons;
import nars.NAR;
import nars.control.BatchActivation;
import org.apache.commons.lang3.mutable.MutableInt;

/** single threaded executor with activation batching */
public class SynchExec extends UniExec {

    private Ons on;

    public SynchExec(int capacity) {
        super(capacity);
    }

    private BatchActivation activation = null;

    @Override
    public synchronized void start(NAR nar) {
        super.start(nar);

        on = new Ons(nar.eventClear.on((n)->{
            if (activation !=null) {
                activation.clear();
                activation = null;
            }
        }),
        nar.onCycle((n)->{
            if (activation ==null) {
                activation = BatchActivation.get();
            }

            activation.commit(n);
        }));
    }

    @Override
    public synchronized void stop() {
        on.off();
        super.stop();
    }
}
