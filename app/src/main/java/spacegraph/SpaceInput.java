package spacegraph;

import nars.util.data.list.FasterList;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * maintains a set of objects which are used as input for representation in a SpaceGraph
 */
abstract public class SpaceInput<O, M extends Spatial<O>> {

    protected List<M> visible = new FasterList(0);
    final AtomicBoolean busy = new AtomicBoolean(true);
    protected SpaceGraph space;
    private float now;
    private float dt;


    public void start(SpaceGraph space) {
        this.space = space;
    }

    public void stop() {
        this.space = null;
    }

    public final List<M> visible() {
        return visible;
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

    /**
     * rewinds the buffer of visible items, when collecting a new batch
     */
    public List<M> rewind(int capacity) {
        visible.forEach(Spatial::inactivate);
        return visible = new FasterList<>(capacity);
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
            update();
        }
    }

    abstract protected void updateImpl();

    abstract public float now();


}
