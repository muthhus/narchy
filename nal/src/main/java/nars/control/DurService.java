package nars.control;

import nars.NAR;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

/** executes approximately once every N durations */
abstract public class DurService extends CycleService {

    public final MutableFloat durations;
    private long now;
    private final AtomicBoolean busy = new AtomicBoolean(false);

    public DurService(NAR n, float durs) {
        this(n, new MutableFloat(durs));
    }

    public DurService(NAR n, MutableFloat durations) {
        super(n);
        this.durations = durations;
        this.now = n.time();
    }

    public DurService(@NotNull NAR nar) {
        this(nar, new MutableFloat(1f));
    }


    @Override public final void accept(NAR nar) {
        long lastNow = this.now;
        long now = nar.time();
        if (now - lastNow >= durations.floatValue() * nar.dur()) {
            this.now = now;
            if (busy.compareAndSet(false, true)) {
                try {
                    runDur(nar);
                } finally {
                    busy.set(false);
                }
            }

        }
    }

    abstract protected void runDur(NAR nar);
}
