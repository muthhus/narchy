package jcog.bag;

import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 2/17/17.
 */
public class RawPLink<X> implements PLink<X> {

    private final X id;
    float pri;

    public RawPLink(X x, float p) {
        this.id = x;
        setPriority(p);
    }

    @Override
    public boolean equals(Object obj) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPriority(float p) {
        this.pri = Priority.validPriority(p);
    }

    @Override
    @NotNull
    public X get() {
        return id;
    }

    @Override
    public String toString() {
        return id + "=" + pri;
    }

    @Override
    public float pri() {
        return pri;
    }

    @Override
    public boolean delete() {
        float pri = this.pri;
        if (pri == pri) {
            this.pri = Float.NaN;
            return true;
        }
        return false;
    }
}
