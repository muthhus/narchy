package bulletphys.util;

import bulletphys.dynamics.DynamicsWorld;
import nars.util.Util;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.math3.util.MathUtils;

import javax.vecmath.Vector3f;


public class AnimFloat extends MutableFloat implements Animated {

    float target;
    final MutableFloat speed;
    private boolean running = true;

    public AnimFloat(DynamicsWorld w, float speed) {
        this(Float.NaN, w, speed);
    }

    public AnimFloat(float current, DynamicsWorld w, float speed) {
        super(Float.NaN);
        setValue(current);
        target = current;
        this.speed = new MutableFloat(speed);
        w.addAnimation(this);
    }

    public void stop() {
        running = false;
    }

    public void invalidate() {
        super.setValue(Float.NaN);
    }

    @Override
    public boolean animate(float dt) {

        float x = floatValue();
        if (x!=x) {
            //invalidated
            super.setValue(target);
        } else {
            //interpLinear(dt);
            interpLERP(dt);
        }

        return running;
    }

    public void interpLERP(float dt) {
        float rate = speed.floatValue() * dt;
        //System.out.println(target + " " + floatValue() + " " + rate);
        super.setValue(
                Util.lerp(target, floatValue(), rate)
        );
    }

    @Override
    public void setValue(float value) {
        if (!Float.isFinite(floatValue()))
            super.setValue(value);
        target = value;
    }

}
