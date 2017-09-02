package nars.control;

import nars.NAR;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

/**
 * executes approximately once every N durations
 */
abstract public class DurService extends CycleService {

    public final MutableFloat durations;
    private long now;


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


    @Override
    public final void accept(NAR nar) {
        //long lastNow = this.now;
        //long now = nar.time();
        //if (now - lastNow >= durations.floatValue() * nar.dur()) {
        if (!busy.get()) {
            nar.runLater(() -> { //asynch
                long noww = nar.time();
                if (noww - this.now >= durations.floatValue() * nar.dur()) {
                    if (busy.compareAndSet(false, true)) {
                        this.now = noww;
                        try {
                            run(nar);
                        } finally {
                            busy.set(false);
                        }
                    }
                }
            });
        }
    }
}
