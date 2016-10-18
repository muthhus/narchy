package nars.link;

import nars.budget.Budget;
import nars.budget.Budgeted;
import org.jetbrains.annotations.NotNull;

import static nars.budget.Budget.validBudgetValue;

/**
 * Created by me on 9/6/16.
 */
@Deprecated public class ArrayBLink<X> implements BLink<X> {

    public X id;
    public float[] f;

    public ArrayBLink() {

    }

    public ArrayBLink(X id, float[] f) {
        this.id = id;
        this.f = f;
    }

    @Override
    public final float priIfFiniteElseZero() {
        float p = f[0]; return /*Float.isFinite(p)*/ (p==p) ? p : 0;
    }

    @Override
    public final float priIfFiniteElseNeg1() {
        float p = f[0]; return /*Float.isFinite(p)*/ (p==p) ? p : -1;
    }

    @Override
    public final X get() {
        return id;
    }


    @Override
    public Budget setBudget(float p, float d, float q) {
        f[0] = validBudgetValue(p);
        f[1] = validBudgetValue(d);
        f[2] = validBudgetValue(q);
        return this;
    }

    @Override
    public final void setPriority(float p) {
        f[0] = validBudgetValue(p);
    }

    @Override
    public final void setDurability(float d) {
        f[1] = validBudgetValue(d);
    }

    @Override
    public final void setQuality(float q) {
        f[2] = validBudgetValue(q);
    }

    @Override
    public final @NotNull Budget clone() {
        throw new UnsupportedOperationException();
    }




    @Override
    public final boolean delete() {
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
