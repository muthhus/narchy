package nars.exe;

import jcog.event.On;
import nars.NAR;
import nars.control.Activate;
import org.apache.commons.lang3.mutable.MutableInt;

public class SynchExec extends UniExec {

    public On onCycle;

    public final MutableInt activationsPerCycle = new MutableInt();

    public SynchExec(int capacity, int firePerCycle) {
        super(capacity);
        activationsPerCycle.setValue(firePerCycle);
    }

    private Thread lastThread = null;

    @Override
    public synchronized void start(NAR nar) {
        super.start(nar);

        onCycle = nar.onCycle((n)->{
            if (lastThread == null || lastThread!=Thread.currentThread()) {
                lastThread = Thread.currentThread();
                Activate.BatchActivate.enable();
            }

            run(activationsPerCycle.intValue() );

            Activate.BatchActivate.get().commit(n);
        });
    }



    @Override
    public synchronized void stop() {
        onCycle.off();
        super.stop();
    }
}
