package spacegraph;

import jcog.list.FasterList;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * maintains a set of objects which are used as input for representation in a SpaceGraph
 * @param X input "key" object type
 * @param Y visualized "value" spatial type
 */
abstract public class AbstractSpace<X, Y>  {

    //final AtomicBoolean busy = new AtomicBoolean(true);
    private long now;
    private long dt;

    final List<SpaceTransform> transforms = new FasterList();

    public AbstractSpace with(SpaceTransform... t) {
        Collections.addAll(this.transforms, t);
        return this;
    }

    public void start(SpaceGraph<X> space) {

    }

    public void stop() {


    }



    abstract public void forEach(Consumer<? super Y> var1);


    public float dt() {
        return dt;
    }

    final AtomicBoolean busy = new AtomicBoolean(false);
    /**
     * for thread-safe usage
     */
    public void updateIfNotBusy(Runnable proc) {
        if (busy.compareAndSet(false, true)) {
            try {
                long last = this.now;
                this.dt = (this.now = System.currentTimeMillis()) - last;
                proc.run();
            } finally {
                busy.set(false);
            }
        }
    }


    /** needs to call update(space) for each active item */
    public void update(SpaceGraph<X> s) {

        List<SpaceTransform> ll = this.transforms;
        for (int i1 = 0, layoutSize = ll.size(); i1 < layoutSize; i1++) {
            ll.get(i1).update(s, dt);
        }

    }

    public abstract int size();

}
