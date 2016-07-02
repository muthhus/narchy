package nars.bag;

import nars.budget.Budget;
import nars.budget.RawBudget;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * Created by me on 7/2/16.
 */
public class WeakBudget<V> extends WeakReference<V> implements Budget {

    private float p, d, q;

    public WeakBudget(V referent, ReferenceQueue<? super V> queue, float p, float d, float q) {
        super(referent, queue);
        this.p = p;
        this.d = d;
        this.q = q;
    }

    @Override
    public float pri() {
        return p;
    }

    @Override
    public boolean isDeleted() {
        return p != p;
    }

    @Override
    public float qua() {
        return q;
    }

    @Override
    public float dur() {
        return d;
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public void _setPriority(float p) {
        this.p = p;
    }

    @Override
    public void _setDurability(float d) {
        this.d = d;
    }

    @Override
    public void _setQuality(float q) {
        this.q = q;
    }

    @Override
    public @NotNull Budget clone() {
        return new RawBudget(p, d, q);
    }
}
