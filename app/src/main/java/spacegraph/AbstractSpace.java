package spacegraph;

import nars.$;
import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;

import java.util.Collections;
import java.util.List;

/**
 * maintains a set of objects which are used as input for representation in a SpaceGraph
 * @param X input "key" object type
 * @param Y visualized "value" spatial type
 */
abstract public class AbstractSpace<X, Y> implements Iterable<Y> {

    //final AtomicBoolean busy = new AtomicBoolean(true);
    private long now;
    private float dt;

    final List<SpaceTransform> transforms = $.newArrayList();

    public AbstractSpace with(SpaceTransform... t) {
        Collections.addAll(this.transforms, t);
        return this;
    }

    public void start(SpaceGraph<X> space) {

    }

    public void stop() {


    }



    public float dt() {
        return dt;
    }

    /**
     * for thread-safe usage
     */
    public void updateIfNotBusy(Runnable proc) {
        float last = this.now;
        this.dt = (this.now = now()) - last;
        proc.run();
    }

    abstract public long now();


    /** needs to call update(space) for each active item */
    public void update(SpaceGraph<X> s) {

        List<SpaceTransform> ll = this.transforms;
        for (int i1 = 0, layoutSize = ll.size(); i1 < layoutSize; i1++) {
            ll.get(i1).update(s, this, dt);
        }

    }


    public abstract int size();

    /** get the i'th object in the display list; the order is allowed to change each frame but not in-between updates */
    public abstract Y get(int i);

    abstract public int forEachIntSpatial(int offset, IntObjectPredicate<Spatial<X>> each);

}
