package spacegraph.phys.util;

import org.apache.commons.math3.util.MathUtils;
import spacegraph.phys.dynamics.DynamicsWorld;

/**
 * Created by me on 6/25/16.
 */
public class AnimFloatAngle extends AnimFloat {

    public AnimFloatAngle(float current, DynamicsWorld w, float speed) {
        super(current, w, speed);
    }

    @Override
    public void setValue(float value) {
        float angleToRad = (float)Math.PI/180f;
        float base = super.floatValue() * angleToRad;
        value = (float) MathUtils.normalizeAngle(value * angleToRad - base, 0 /* places it nearest to current value */) + base;
        super.setValue(value/angleToRad);
    }
}
