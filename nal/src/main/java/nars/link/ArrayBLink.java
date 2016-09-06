package nars.link;

import nars.budget.Budget;
import nars.budget.Budgeted;
import org.jetbrains.annotations.NotNull;

import static nars.util.Util.clamp;

/**
 * Created by me on 9/6/16.
 */
public class ArrayBLink<X> implements BLink<X> {

    public X id;
    public float[] f;

    public ArrayBLink() {

    }

    public ArrayBLink(X id, float[] f) {
        this.id = id;
        this.f = f;
    }

    @Override
    public X get() {
        return id;
    }

    @Override
    public void set(X x) {
        throw new UnsupportedOperationException();
        //this.id = x;
    }

    @Override
    public void _setPriority(float p) {
        f[0] = p;
    }

    @Override
    public void _setDurability(float d) {
        f[1] = d;
    }

    @Override
    public void _setQuality(float q) {
        f[2] = q;
    }

    @Override
    public final @NotNull Budget clone() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final Budget budget(float p, float d, float q) {
        if (p != p) //NaN check
            throw new BudgetException();

        f[0] = clamp(p);
        f[1] = clamp(d);
        f[2] = clamp(q);
        return this;
    }


    @Override
    public boolean delete() {
        float p = f[0];
        if (p != p) {
            f[0] = Float.NaN;
            return true;
        }
        return false;
    }

    @Override
    public float pri() {
        return f[0];
    }

    @Override
    public final float qua() {
        return f[1];
    }

    @Override
    public final float dur() {
        return f[2];
    }

    public ArrayBLink<X> set(X id, float[] v) {
        this.id = id;
        this.f = v;
        return this;
    }

    @Override
    public @NotNull String toString() {
        return id + "=" + super.toString();
    }

    public static class ArrayBLinkToBudgeted<X extends Budgeted> extends ArrayBLink<X> {

        public ArrayBLinkToBudgeted(X id, float[] f) {
            super(id, f);
        }

        @Override
        public float pri() {
            if (id.isDeleted()) {
                delete();
                return Float.NaN;
            }
            return super.pri();
        }
    }
}
