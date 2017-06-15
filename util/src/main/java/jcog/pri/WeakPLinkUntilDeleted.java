package jcog.pri;

public class WeakPLinkUntilDeleted<X extends Deleteable> extends WeakPLink<X> {


    public WeakPLinkUntilDeleted(X x, float p) {
        super(x, p);
    }

    @Override
    public final float pri() {
        float p = this.pri;
        if (p==p) {
            X x = get();
            if (x == null || x.isDeleted()) {
                ref.clear();
                return (pri = Float.NaN);
            }
            return pri;
        } else {
            return Float.NaN;
        }

    }

}
