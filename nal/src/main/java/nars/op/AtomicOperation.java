package nars.op;

import nars.NAR;
import nars.Task;
import nars.term.atom.Atom;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import static nars.time.Tense.ETERNAL;

/**
 * debounced and atomically/asynchronously executable operation
 */
abstract public class AtomicOperation extends Operation implements BiConsumer<Task, NAR> {

    private final float minPeriod;
    private final float expThresh;

    /**
     * time of the current rising edge, or ETERNAL if not activated
     */
    final AtomicLong rise = new AtomicLong(ETERNAL);

    long lastActivity = ETERNAL;
    public static final Logger logger = LoggerFactory.getLogger(AtomicOperation.class);

    protected AtomicOperation(@NotNull Atom atom, float minPeriod /* dur's */, float expThresh, NAR n) {
        super(atom, n);
        this.minPeriod = minPeriod;
        this.expThresh = expThresh;

    }

    @Override
    public @Nullable Task run(@NotNull Task x, @NotNull NAR n) {


        if (x.expectation() >= expThresh) {
            long now = n.time();
            int dur = n.dur();
            if (x.during(now - dur / 2, now + dur / 2)) {
                tryInvoke(x, n);
            }
        }
        return x;
    }

    /**
     * executed async
     */
    protected void invoke(Task x, NAR n) {
        try {
            accept(x, n);
        } catch (Throwable t) {
            logger.info("{} {}", this, t);
        } finally {
            //end invocation
            lastActivity = n.time();
            rise.set(ETERNAL);
        }
    }

    public boolean tryInvoke(Task x, NAR n) {

        long now = n.time();
        if (lastActivity == ETERNAL || (now - lastActivity > minPeriod * n.dur()) && rise.compareAndSet(ETERNAL, now)) {
            lastActivity = now;
            n.runLater(() -> this.invoke(x, n));
            return true;
        }
        return false;
    }

    public boolean isInvoked() {
        return rise.get() != ETERNAL;
    }

}
