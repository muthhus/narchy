package jcog.pri;

import java.lang.ref.WeakReference;

public class WeakPLink<X> extends AbstractPLink<X> {

    final WeakReference<X> ref;
    private final int hash;

    public WeakPLink(X x, float p) {
        super(p);
        this.hash = x.hashCode();
        this.ref = new WeakReference<>(x);
    }

    @Override
    public float pri() {
        get();
        return pri;
    }

    @Override
    public boolean isDeleted() {
        float p = pri();
        return p != p;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public X get() {
        X r = ref.get();
        if (r==null) {
            pri = Float.NaN; //delete
            return null;
        } else {
            return r;
        }
    }

}
