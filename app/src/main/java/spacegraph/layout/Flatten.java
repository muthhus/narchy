package spacegraph.layout;

import spacegraph.*;
import spacegraph.math.Quat4f;
import spacegraph.math.v3;
import spacegraph.phys.Dynamic;

import java.util.function.Consumer;

import static spacegraph.math.v3.v;

/**
 * TODO generalize to arbitrary plane sizes and orientations
 */
public class Flatten<X> implements SpaceTransform<X>, Consumer<Spatial<X>> {

    private final Quat4f up;
    private final Quat4f tmp = new Quat4f();

    public Flatten() {
        up = Quat4f.angle(0,0,1,0);
    }

    @Override
    public void update(SpaceGraph<X> g, AbstractSpace<X, Spatial<X>> src, float dt) {
        src.forEach(this);
    }

    @Override
    public void accept(Spatial<X> ss) {

        if (ss instanceof SimpleSpatial) {
            SimpleSpatial s = (SimpleSpatial) ss;
            v3 f = v();
            locate(s, f);
            s.move(f, 0.5f );
            s.rotate(up, 0.5f, tmp);

            //eliminate z-component of linear velocity
            Dynamic b = s.body;
            if (b !=null) {
                b.linearVelocity.scale(
                        1f, 1f, 0.9f
                );
                b.angularVelocity.scale(0.9f);
            }
        }
    }

    //TODO abstract this
    protected void locate(SimpleSpatial s, v3 f) {
        f.set(s.x(), s.y(), 0.9f);
    }

}
