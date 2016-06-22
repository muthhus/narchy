package nars.util.data;

import nars.truth.Truth;
import nars.util.Util;
import org.jetbrains.annotations.NotNull;

import static nars.$.t;

/**
 * Mutable holder and manipulator of a float value restricted to range 0...+1.0
 */
public class UnitVal {
    protected float v;
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

    @NotNull
    public Truth isTrue() {
        return t(v, 0.25f);
    }



    public void setInc(float dv) {
        this.dv = dv;
    }

    @NotNull
    public Truth inc(boolean positive) {
        return _inc(positive, dv);
    }

    @NotNull
    protected Truth _inc(boolean positive, float dv) {
        v = Util.clamp(v + (positive ? 1 : -1) * dv);
        return t(v, 0.5f);
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

    public boolean equals(float target, float tolerance) {
        return _equals(target, tolerance);
    }

    protected boolean _equals(float target, float tolerance) {
        return Math.abs(v - target) <= tolerance;
    }

    public int compare(float w, float tolerance) {
        if (_equals(w, tolerance)) return 0;
        if (v < w) return -1;
        return 1;
    }
}
