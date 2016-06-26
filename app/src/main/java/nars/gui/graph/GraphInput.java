package nars.gui.graph;

import nars.util.data.list.FasterList;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 6/26/16.
 */
abstract public class GraphInput<O, M extends Atomatter<O>> {

    protected List<M> visible = new FasterList(0);
    final AtomicBoolean busy = new AtomicBoolean(true);
    protected GraphSpace grapher;
    private float now;
    private float dt;


    public void start(GraphSpace grapher) {
        this.grapher = grapher;
    }

    public void stop() {
        this.grapher = null;
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
        visible.forEach(Atomatter::inactivate);
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
