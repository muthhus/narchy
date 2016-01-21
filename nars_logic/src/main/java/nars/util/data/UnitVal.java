package nars.util.data;

import nars.truth.DefaultTruth;
import nars.truth.Truth;

/**
 * Mutable holder and manipulator of a float value restricted to range 0...+1.0
 */
public class UnitVal {
    private float v;
    private float dv;

    public UnitVal() {
        this(0.5f, 0.1f);
    }

    public UnitVal(float v, float dv) {
        this.v = v;
        this.dv = dv;
    }

    public float get() {
        return v;
    }

    public Truth isTrue() {
        return new DefaultTruth(v, 0.9f);
    }

    public boolean equals(float target, float tolerance) {
        return Math.abs(v - target) <= tolerance;
    }

    public void inc(boolean positive) {
        v = Util.clamp(v + (positive ? 1 : -1) * dv);
    }

    public void setOne() {
        v = 1;
    }

    public void setZero() {
        v = 0;
    }

    public void random() {
        v = (float)Math.random();
    }
}
