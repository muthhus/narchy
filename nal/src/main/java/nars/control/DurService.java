package nars.control;

import nars.NAR;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

import static nars.time.Tense.ETERNAL;

/** executes approximately once every N durations */
abstract public class DurService extends CycleService {

    private final MutableFloat durations;
    private long now;

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
            runDur(nar);
        }
    }

    abstract protected void runDur(NAR nar);
}
