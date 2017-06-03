package spacegraph.phys.util;

import jcog.Util;
import org.apache.commons.lang3.mutable.MutableFloat;
import spacegraph.phys.Dynamics;


public class AnimFloat extends MutableFloat implements Animated {

    float target;
    final MutableFloat speed;
    private boolean running = true;

    public AnimFloat(Dynamics w, float speed) {
        this(Float.NaN, w, speed);
    }

    public AnimFloat(float current, Dynamics w, float speed) {
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
                Util.lerp(rate, floatValue(), target)
        );
    }

    @Override
    public void setValue(float value) {
        if (!Float.isFinite(floatValue()))
            super.setValue(value);
        target = value;
    }

}
