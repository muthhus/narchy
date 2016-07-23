package spacegraph;

import nars.$;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * maintains a set of objects which are used as input for representation in a SpaceGraph
 * @param X input "key" object type
 * @param Y visualized "value" spatial type
 */
abstract public class SpaceInput<X, Y extends Spatial<X>> implements Iterable<Y> {

    final AtomicBoolean busy = new AtomicBoolean(true);
    protected SpaceGraph<X> space;
    private long now;
    private float dt;

    final List<SpaceTransform> transforms = $.newArrayList();

    public SpaceInput with(SpaceTransform<X>... t) {
        Collections.addAll(this.transforms, t);
        return this;
    }

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

    /**
     * for thread-safe usage
     */
    public void updateIfNotBusy(Consumer<SpaceInput<X,Y>> proc) {
        if (!isBusy()) {
            synchronized (busy) {
                float last = this.now;
                this.dt = (this.now = now()) - last;
                proc.accept(this);
            }
        }
    }

    abstract public long now();


    /** needs to call update(space) for each active item */
    public void update(SpaceGraph<X> s) {


        this.forEach(a -> a.update(s));

        List<SpaceTransform> ll = this.transforms;
        int size = size();
        for (int i1 = 0, layoutSize = ll.size(); i1 < layoutSize; i1++) {
            ll.get(i1).update(s, this, dt);
        }

    }


    public abstract int size();

    /** get the i'th object in the display list; the order is allowed to change each frame but not in-between updates */
    public abstract Y get(int i);

}
