package nars.control;

import nars.NAR;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

abstract public class CycleService extends NARService implements Consumer<NAR> {

    protected final AtomicBoolean busy = new AtomicBoolean(false);

    public CycleService(NAR nar) {
        super(nar);
    }

    @Override
    protected void start(NAR nar)  {
        super.start(nar);
        ons.add(nar.onCycle(this));
    }

    @Override public void accept(NAR nar) {
        if (busy.compareAndSet(false, true)) {
            try {
                run(nar);
            } finally {
                busy.set(false);
            }
        }
    }

    abstract protected void run(NAR nar);

}
