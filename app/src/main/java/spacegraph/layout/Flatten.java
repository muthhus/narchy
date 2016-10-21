package spacegraph.layout;

import spacegraph.*;
import spacegraph.math.Quat4f;

import java.util.function.Consumer;

/**
 * TODO generalize to arbitrary plane sizes and orientations
 */
public class Flatten<O> implements SpaceTransform<O>, Consumer<Spatial<O>> {

    private final Quat4f up;
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

        float[] f = new float[3];
        if (ss instanceof SimpleSpatial) {
            SimpleSpatial s = (SimpleSpatial) ss;
            locate(s, f);
            s.move(f[0], f[1], f[2], 0.95f );

            s.rotate(up, 0.95f, tmp);
        }
    }

    //TODO abstract this
    protected void locate(SimpleSpatial s, float[] f) {
        f[0] = s.x();
        f[1] = s.y();
        f[2] = 0.9f;
    }
}
