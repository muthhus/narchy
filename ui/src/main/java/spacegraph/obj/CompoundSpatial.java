package spacegraph.obj;

import nars.util.list.FasterList;
import spacegraph.AbstractSpatial;
import spacegraph.Spatial;
import spacegraph.phys.Collidable;
import spacegraph.phys.Dynamics;
import spacegraph.phys.constraint.TypedConstraint;

import java.util.List;
import java.util.function.Consumer;

/**
 * TODO make inherit from an AbstractSpatial not SimpleSpatial, and
 * SimpleSpatial also subclass from that
 */
public abstract class CompoundSpatial<X> extends AbstractSpatial<X> {

    private final List<Collidable> bodies = new FasterList();
    private final List<Spatial> spatials = new FasterList();
    private final List<TypedConstraint> constraints = new FasterList();

    public CompoundSpatial(X x) {
        super(x);
    }

    @Override
    public final void update(Dynamics world) {

        if (bodies.isEmpty() && spatials.isEmpty()) { //HACK TODO use a boolean flag
            create(world);
        } else {
            next(world);
        }

        for (Spatial s : spatials)
            s.update(world);

    }

    protected void next(Dynamics world) {

    }

    public TypedConstraint add(TypedConstraint c) {
        constraints.add(c);
        return c;
    }

    public Spatial add(Spatial s) {
        spatials.add(s);
        return s;
    }

    public void remove(Spatial s) {
        spatials.remove(s);
    }


    public void add(Collidable c) {
        bodies.add(c);
    }

    public void remove(Collidable c) {
        bodies.remove(c);
    }

    protected void create(Dynamics world) {

    }

    @Override
    public void forEachBody(Consumer<Collidable> c) {
        bodies.forEach(c);
        spatials.forEach(s -> s.forEachBody(c));
    }

    @Override
    public List<TypedConstraint> constraints() {
        return constraints;
    }
}
