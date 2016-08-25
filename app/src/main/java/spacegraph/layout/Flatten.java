package spacegraph.layout;

import spacegraph.*;
import spacegraph.math.Quat4f;

import java.util.function.Consumer;

/**
 * TODO generalize to arbitrary plane sizes and orientations
 */
public class Flatten<O> implements SpaceTransform<O>, Consumer<Spatial<O>> {

    private Quat4f up = new Quat4f();
    private final Quat4f tmp = new Quat4f();

    public Flatten() {
        up = Quat4f.angle(0,0,1,0);
    }

    @Override
    public void update(SpaceGraph<O> g, AbstractSpace<O, ?> src, float dt) {
        src.forEach(this);
    }

    @Override
    public void accept(Spatial<O> ss) {

        if (ss instanceof SimpleSpatial) {
            SimpleSpatial s = (SimpleSpatial) ss;
            s.move(s.x(), s.y(), 0, 0.75f);
            s.rotate(up, 0.9f, tmp);
        }
    }
}
