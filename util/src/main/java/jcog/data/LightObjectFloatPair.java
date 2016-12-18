package jcog.data;

import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;

public final class LightObjectFloatPair<Z> implements ObjectFloatPair<Z> {

    private final float val;
    private final Z the;

    public LightObjectFloatPair(Z the, float val) {
        this.val = val;
        this.the = the;
    }

    @Override
    public Z getOne() {
        return the;
    }

    @Override
    public float getTwo() {
        return val;
    }

    @Override
    public int compareTo(ObjectFloatPair<Z> zObjectFloatPair) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return the + "=" + val;
    }
}
