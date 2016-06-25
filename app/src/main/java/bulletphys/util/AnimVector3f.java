package bulletphys.util;

import bulletphys.dynamics.DynamicsWorld;
import nars.util.Util;
import org.apache.commons.lang3.mutable.MutableFloat;

import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;

import static io.netty.handler.codec.ProtocolDetectionResult.invalid;


public class AnimVector3f extends Vector3f implements Animated {

    final Vector3f target = new Vector3f();
    final MutableFloat speed;
    private boolean running = true;

    public AnimVector3f(DynamicsWorld w, float speed) {
        this(Float.NaN, Float.NaN, Float.NaN, w, speed);
    }

    public AnimVector3f(Vector3f current, DynamicsWorld w, float speed) {
        this(current.x, current.y, current.z, w, speed);
    }

    public AnimVector3f(float x, float y, float z, DynamicsWorld w, float speed) {
        super(x, y, z);
        target.set(this);
        this.speed = new MutableFloat(speed);
        w.addAnimation(this);
    }

    public void stop() {
        running = false;
    }

    public void invalidate() {
        super.set(Float.NaN, Float.NaN, Float.NaN);
    }

    public boolean animate(float dt) {

        if (x!=x) {
            //invalidated
            super.set(target);
        } else {
            //HACK use constant velocity
            float rate = speed.floatValue();
            super.set(
                    Util.lerp(target.x, x, rate),
                    Util.lerp(target.y, y, rate),
                    Util.lerp(target.z, z, rate)
            );
        }

        return running;
    }

    public void set(Vector3f v) {
        //if invalidated, use the target value immediately
        if (x != x) super.set(v);
        target.set(v);
    }

    public void set(float x, float y, float z) {
        //if invalidated, use the target value immediately
        if (x != x) super.set(x, y, z);
        target.set(x, y, z);
    }

    public void set(float[] v) {
        //if invalidated, use the target value immediately
        if (x != x) super.set(v);
        target.set(v);
    }

}
