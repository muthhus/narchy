package bulletphys.util;

import bulletphys.dynamics.DynamicsWorld;
import nars.util.Util;
import org.apache.commons.lang3.mutable.MutableFloat;

import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;

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

    @Override
    public boolean animate(float dt) {

        if (x!=x) {
            //invalidated
            super.set(target);
        } else {
            //interpLinear(dt);
            interpLERP(dt);
        }

        return running;
    }

    public void interpLERP(float dt) {
        float rate = speed.floatValue() * dt;
        super.set(
                Util.lerp(target.x, x, rate),
                Util.lerp(target.y, y, rate),
                Util.lerp(target.z, z, rate)
        );
    }

    public void interpLinear(float dt) {
        //HACK use constant velocity
        float dx = target.x - x;
        float dy = target.y - y;
        float dz = target.z - z;

        float rate = speed.floatValue() * dt;
        float lenSq = dx*dx+dy*dy+dz*dz;
        float len = (float)Math.sqrt(lenSq);

        if (len < rate) {
            //within one distance
            //System.out.println(dt + " " + "target==" + target);
            super.set(target);
        } else {
            float v = rate/len;
            //System.out.println(dt + " " + "target.." + target + " " + len + " " + v);

            super.set(
              x + dx * v,
              y + dy * v,
              z + dz * v );
        }
    }

    public void set(Vector3f v) {
        //if invalidated, use the target value immediately
        if (x != x) super.set(v);
        target.set(v);
    }

    @Override
    public void set(float x, float y, float z) {
        //if invalidated, use the target value immediately
        if (x != x) super.set(x, y, z);
        target.set(x, y, z);
    }

    @Override
    public void set(float[] v) {
        //if invalidated, use the target value immediately
        if (x != x) super.set(v);
        target.set(v);
    }

}
