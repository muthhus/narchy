//package nars.bag;
//
//import nars.budget.Budget;
//import nars.budget.RawBudget;
//import org.jetbrains.annotations.NotNull;
//
//import java.lang.ref.ReferenceQueue;
//import java.lang.ref.WeakReference;
//
//
//public class WeakBudget<X> extends WeakReference<X> implements Budget {
//
//    private float p, d, q;
//
//    public WeakBudget(X referent, ReferenceQueue<? super X> queue, float p, float d, float q) {
//        super(referent, queue);
//        this.p = p;
//        this.d = d;
//        this.q = q;
//    }
//
//    @Override
//    public final X get() {
//        if (p == p) {
//            X x = super.get();
//            if (x == null) {
//                p = Float.NaN;
//            }
//            return x;
//        } else {
//            return null;
//        }
//    }
//
//    @Override
//    public float pri() {
//        return p;
//    }
//
//    @Override
//    public boolean isDeleted() {
//        return p != p;
//    }
//
//    @Override
//    public float qua() {
//        return q;
//    }
//
//    @Override
//    public float dur() {
//        return d;
//    }
//
//    @Override
//    public final boolean delete() {
//        if (p==p) {
//            p = Float.NaN;
//            clear();
//            return true;
//        }
//        return false;
//    }
//
//    @Override
//    public void _setPriority(float p) {
//        this.p = p;
//    }
//
//    @Override
//    public void _setDurability(float d) {
//        this.d = d;
//    }
//
//    @Override
//    public void _setQuality(float q) {
//        this.q = q;
//    }
//
//    @Override
//    public @NotNull Budget clone() {
//        return new RawBudget(p, d, q);
//    }
//
//
//}
