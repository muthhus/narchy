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

    private final Quat4f up = Quat4f.angle(0,0,1,0);
    private final Quat4f tmp = new Quat4f();

    private float zTolerance = 2f;

    private float zSpeed = 1f;
    float rotateRate = 0.25f;

    public Flatten() {
    }

    @Override
    public void update(SpaceGraph<X> g, AbstractSpace<X, Spatial<X>> src, float dt) {
        src.forEach(this);
    }

    @Override
    public void accept(Spatial<X> ss) {

        if (ss instanceof SimpleSpatial) {
            SimpleSpatial s = (SimpleSpatial) ss;
            //v3 f = v();
            //locate(s, f);
            //s.move(f, rate);

            Dynamic b = s.body;
            if (b == null)
                return;

            float tz = b.transform().z;
            if (Math.abs(tz) > zTolerance) {
                b.force(v( 0, 0, -tz*zSpeed*b.mass()));
            }
            s.rotate(up, rotateRate, tmp);


            //dampening: keep upright and eliminate z-component of linear velocity
//            b.linearVelocity.scale(
//                    1f, 1f, 0.9f
//            );
            b.angularVelocity.scale(0.9f);
        }
    }

    //TODO abstract this
    protected void locate(SimpleSpatial s, v3 f) {
        f.set(s.x(), s.y(), 0.9f);
    }

}
