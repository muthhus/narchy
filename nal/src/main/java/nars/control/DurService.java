package nars.control;

import nars.NAR;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * executes approximately once every N durations
 */
abstract public class DurService extends NARService implements Runnable {

    /** ideal duration multiple to be called, since time after implementation's procedure finished last*/
    public final MutableFloat durations;

    private final NAR nar;
    private long now;
    final AtomicBoolean busy = new AtomicBoolean(false);


    public DurService(NAR n, float durs) {
        this(n, new MutableFloat(durs));
    }

    public DurService(NAR n, MutableFloat durations) {
        super(n);
        this.durations = durations;
        this.now = n.time();
        this.nar = n;
    }

    /** simple convenient constructor */
    public static DurService build(NAR nar, Runnable r) {
        return new DurService(nar) {
            @Override protected void run(NAR n, long dt) {
                r.run();
            }
        };
    }

    public DurService(@NotNull NAR nar) {
        this(nar, new MutableFloat(1f));
    }

    @Override
    protected void start(NAR nar) {
        super.start(nar);
        nar.runLater(this); //initial
    }

    @Override
    public void run() {
        //long lastNow = this.now;
        //long now = nar.time();
        //if (now - lastNow >= durations.floatValue() * nar.dur()) {
        if (busy.compareAndSet(false, true)) {
            long last = this.now;
            this.now = nar.time();
            long delta = now - last;
            try {
                //TODO instrument here
                run(nar, delta);
                //----
            } finally {
                long next = nar.time() + Math.round(nar.dur() * durations.floatValue());
                nar.at(next, this);
                busy.set(false);
            }
        }
    }

    /** time (raw cycles, not durations) which elapsed since run was scheduled last */
    abstract protected void run(NAR n, long dt);
}
