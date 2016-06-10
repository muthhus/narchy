package nars.bag;

import nars.Global;
import nars.budget.Budget;
import nars.budget.Budgeted;
import nars.budget.UnitBudget;
import nars.link.Link;
import nars.util.data.FastBitSet;
import org.jetbrains.annotations.NotNull;

import static nars.util.data.Util.clamp;

/**
 * Created by me on 5/19/16.
 */
public class BLinkPool<X> {

    @NotNull
    final FastBitSet changed;
    @NotNull
    final float[] b;
    private final int cap;

    public BLinkPool(int capacity) {
        this.cap = capacity;
        this.b = new float[7 * capacity];
        this.changed = new FastBitSet(capacity);
    }

    @NotNull
    public BLinkI get(int n) {
        return new BLinkI(n);
    }


    /** priority */
    final static int PRI = 0;
    /** delta pri */
    final static int DPRI = 1;

    /** durability */
    final static int DUR = 2;
    /** delta dur */
    final static int DDUR = 3;

    /** quality */
    final static int QUA = 4;
    /** delta qua */
    final static int DQUA = 5;



    /** time of last forget */
    final static int LASTFORGET = 6;

    /**
     * Buffered Budget Link (an entry in a bag) - Indexed version
     */
    public final class BLinkI extends Budget implements Link<X> {

        final int o; //offset

        protected BLinkI(int o) {
            this.o = o;
        }

//        public BLinkI(int o, float p, float d, float q) {
//            this(o);
//            init(p, d, q);
//        }
//
//        public BLinkI(int o, @NotNull Budgeted bu) {
//            this(o, bu, 1f);
//        }
//
//        public BLinkI(int o, @NotNull Budgeted bu, float scale) {
//            this(o);
//            init(bu, scale);
//        }

        @NotNull @Override
        public X get() {
            //TODO
            return null;
        }

        private void init(@NotNull Budgeted c, float scale) {
            init(c.pri() * scale, c.dur(), c.qua());
        }

        public void init(float p, float d, float q) {
            float[] b = BLinkPool.this.b;
            int x = o * 7;
            b[x++] = clamp(p);
            b[x++] = 0;
            b[x++] = clamp(d);
            b[x++] = 0;
            b[x++] = clamp(q);
            b[x++] = 0;
            b[x++] = Float.NaN;
        }

        public void setChanged(boolean c) {
            changed.set(o, c);
        }

        public boolean isChanged() {
            return changed.get(o);
        }

        @Override
        public final boolean delete() {
            if (!isDeleted()) {
                b[PRI] = Float.NaN;
                setChanged(true);
                return true;
            }
            return false;
        }

        /** TODO return false to signal to the bag to remove this item */
        public final boolean commit() {
            if (isChanged()) {
                float[] b = BLinkPool.this.b;
                int x = o * 7;
                b[x] = clamp(b[x] + b[++x]); b[x++] = 0;
                b[x] = clamp(b[x] + b[++x]); b[x++] = 0;
                b[x] = clamp(b[x] + b[++x]); b[x++] = 0;
                setChanged(false);
                return true;
            }
            return false;
        }

        @Override
        public final float pri() {
            return b[o * 7 /*+ PRI*/];
        }

        @Override
        public final boolean isDeleted() {
            float p = pri();
            return (p!=p); //fast NaN test
        }

        protected final void setValue(int x, float v) {
            float[] b = BLinkPool.this.b;
            int xx = (o * 7) + 2 * x;
            float delta = v - b[xx];
            b[xx + 1] += delta;
            setChanged(true);
        }
        protected final float getValue(int x) {
            float[] b = BLinkPool.this.b;
            int xx = (o * 7) + 2 * x;
            return b[xx];
        }

        @Override
        public final void _setPriority(float p) {
            setValue(0, p);
        }

        @Override
        public final float dur() {
            return getValue(1);
        }

        @Override
        public final void _setDurability(float d) {
            setValue(1, d);
        }

        @Override
        public final float qua() {
            return getValue(2);
        }

        @Override
        public void _setQuality(float q) {
            setValue(2, q);
        }

        @Override
        public final float setLastForgetTime(float currentTime) {
            float[] b = BLinkPool.this.b;
            int x = o * 7 + LASTFORGET;
            float lastForget = b[x];
            float diff = (lastForget != lastForget /* NaN test */) ? Global.SUBFRAME_EPSILON : (currentTime - lastForget);
            b[x] = currentTime;
            return diff;
        }

        @Override
        public float getLastForgetTime() {
            return b[LASTFORGET];
        }


        @Override
        public @NotNull UnitBudget clone() {
            throw new UnsupportedOperationException();
            //return new UnitBudget(this);
        }


        @Override public boolean equals(Object obj) {
//        /*if (obj instanceof Budget)*/ {
//            return equalsBudget((Budget) obj);
//        }
//        return id.equals(((BagBudget)obj).id);

            //return obj == this;

            throw new UnsupportedOperationException();
        }

        @Override public int hashCode() {
            throw new UnsupportedOperationException();
        }


        @NotNull
        @Override
        public String toString() {
            return get() + "=" + getBudgetString();
        }



//    public void charge(float overflow) {
//        assert(overflow > 0);
//        b[6] += overflow;
//    }
//    public float drain() {
//        float[] b = this.b;
//        float o = b[6];
//        if (o > 0) {
//            b[6] = 0; //clear
//        }
//        return o;
//    }

//    static boolean nonZero(float x) {
//        //return (Math.abs(x) > Global.BUDGET_EPSILON);
//        return x!=0f;
//    }
    }

}
