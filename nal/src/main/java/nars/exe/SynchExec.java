package nars.exe;

import jcog.event.On;
import jcog.event.Ons;
import nars.NAR;
import nars.control.BatchActivate;
import org.apache.commons.lang3.mutable.MutableInt;

public class SynchExec extends UniExec {



    public final MutableInt activationsPerCycle = new MutableInt();
    private Ons on;

    public SynchExec(int capacity, int firePerCycle) {
        super(capacity);
        activationsPerCycle.setValue(firePerCycle);
    }

    private BatchActivate activate = null;

    @Override
    public synchronized void start(NAR nar) {
        super.start(nar);

        on = new Ons(nar.eventClear.on((n)->{
            if (activate!=null) {
                activate.clear();
                activate = null;
            }
        }),
        nar.onCycle((n)->{
            if (activate ==null) {
                activate = BatchActivate.get();
            }

            run(activationsPerCycle.intValue() );

            activate.commit(n);
        }));
    }



    @Override
    public synchronized void stop() {
        on.off();
        super.stop();
    }
}
