package nars.bag;

import nars.budget.Budget;
import nars.budget.Budgeted;
import nars.budget.UnitBudget;
import nars.nal.Tense;
import org.jetbrains.annotations.NotNull;

import static nars.util.data.Util.clamp;

/**
 * Budgeted Link (an entry in a bag)
 * equalsTo/hashCode proxies to the wrapped element, X id
 *
 * Acts as a "budget vector" containing an accumulating delta
 * that can be commit()'d on the next udpate
 */
public final class BLink<X> extends Budget implements Link<X> {

    /** the referred item */
    public final X id;

    /** time of last forget */
    private long lastForget = Tense.TIMELESS;

    /** changed status bit */
    boolean changed;

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

    /** overflow/backpressure buffer variable */
    final static int BUFFERED = 6;


    private final float[] b = new float[6];



    public BLink(X id) {
        this.id = id;
    }

    public BLink(X id, float p, float d, float q) {
        this(id);
        init(p, d, q);
    }

    public BLink(X id, @NotNull Budgeted b) {
        this(id);
        init(b, 1f);
    }

    public BLink(X id, @NotNull Budgeted b, float scale) {
        this(id);
        init(b, scale);
    }

    @NotNull @Override
    public X get() {
        return id;
    }

    private void init(@NotNull Budgeted c, float scale) {
        //this.lastForget = c.getLastForgetTime();
        this.lastForget = Tense.TIMELESS;

        init(c.pri() * scale, c.dur(), c.qua());
    }

    public void init(float p, float d, float q) {
        float[] b = this.b;
        b[0] = clamp(p);
        b[2] = clamp(d);
        b[4] = clamp(q);
    }

    @Override
    public final void delete() {
        b[0] = Float.NaN;
    }

    /** TODO return false to signal to the bag to remove this item */
    public final boolean commit() {
        if (changed) {
            float[] b = this.b;
            b[0] = clamp(b[0] + b[1]); b[1] = 0;
            b[2] = clamp(b[2] + b[3]); b[3] = 0;
            b[4] = clamp(b[4] + b[5]); b[5] = 0;
            changed = false;
            return true;
        }
        return false;
    }

    @Override
    public final float pri() {
        return b[0];
    }

    @Override
    public final boolean isDeleted() {
        float p = b[0];
        return (p!=p); //fast NaN test
    }

    protected final void setValue(int x, float v) {
        float[] b = this.b;
        int twoX = 2 * x;
        b[twoX + 1] += v - b[twoX];
        changed = true;
    }

    @Override
    public final void _setPriority(float p) {
        setValue(0, p);
    }

    @Override
    public final float dur() {
        return b[2];
    }

    @Override
    public final void _setDurability(float d) {
        setValue(1, d);
    }

    @Override
    public final float qua() {
        return b[4];
    }

    @Override
    public void _setQuality(float q) {
        setValue(2, q);
    }

    @Override
    public final long setLastForgetTime(long currentTime) {
        long lastForget = this.lastForget;
        long diff;
        if (lastForget == Tense.TIMELESS) {
            diff = 0;
        } else {
            diff = currentTime - lastForget;
            if (diff == 0) return 0; //return but dont set lastForget
        }
        this.lastForget = currentTime;
        return diff;
    }

    @Override
    public long getLastForgetTime() {
        return lastForget;
    }


    @NotNull
    @Override
    public UnitBudget clone() {
        return new UnitBudget(this);
    }


    @Override public boolean equals(Object obj) {
//        /*if (obj instanceof Budget)*/ {
//            return equalsBudget((Budget) obj);
//        }
//        return id.equals(((BagBudget)obj).id);
        return obj == this;
    }

    @Override public int hashCode() {
        return id.hashCode();
    }

    @NotNull
    @Override
    public String toString() {
        return id + "=" + getBudgetString();
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
