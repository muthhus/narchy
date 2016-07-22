package spacegraph;

import nars.util.data.list.FasterList;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * maintains a set of objects which are used as input for representation in a SpaceGraph
 */
abstract public class SpaceInput<O, M extends Spatial<O>> implements Iterable<M> {

    final AtomicBoolean busy = new AtomicBoolean(true);
    protected SpaceGraph space;
    private long now;
    private float dt;


    public void start(SpaceGraph space) {
        this.space = space;
    }

    public void stop() {
        this.space = null;
    }

    public void ready() {
        busy.set(false);
    }

    public final float setBusy() {
        busy.set(true);
        return dt;
    }

    public boolean isBusy() {
        return busy.get();
    }

    public float dt() {
        return dt;
    }




    public void update() {
        float last = this.now;
        this.dt = (this.now = now()) - last;
        updateImpl();
    }

    /**
     * for thread-safe usage
     */
    public void updateIfNotBusy() {
        if (!isBusy()) {
            synchronized (busy) {
                update();
            }
        }
    }

    abstract protected void updateImpl();

    abstract public long now();


    /** needs to call update(space) for each active item */
    abstract public void update(SpaceGraph<O> space);

    public abstract int size();
}
