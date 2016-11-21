package spacegraph.phys.util;

import nars.util.Util;
import org.apache.commons.lang3.mutable.MutableFloat;
import spacegraph.math.v3;
import spacegraph.phys.Dynamics;

public class AnimVector3f extends v3 implements Animated {

    final v3 target = new v3();
    final MutableFloat speed;
    private boolean running = true;

    public AnimVector3f(Dynamics w, float speed) {
        this(Float.NaN, Float.NaN, Float.NaN, w, speed);
    }

    public AnimVector3f(v3 current, Dynamics w, float speed) {
        this(current.x, current.y, current.z, w, speed);
    }

    public AnimVector3f(float x, float y, float z, Dynamics w, float speed) {
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


    @Override
    public void set(float x, float y, float z) {
        //if invalidated, use the target value immediately
        if (x != x) super.set(x, y, z);
        target.set(x, y, z);
    }

}
