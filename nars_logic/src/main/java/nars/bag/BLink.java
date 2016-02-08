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

    private final float[] b = new float[6];
    public final X id;
    private long lastForget = Tense.TIMELESS;
    boolean changed = false;

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

    @Override
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
        b[1] = clamp(d);
        b[2] = clamp(q);
    }

    @Override
    public void delete() {
        clearDelta();
        b[0] = Float.NaN;
    }

    private void clearDelta() {
        float[] b = this.b;
        b[3] = b[4] = b[5] = 0;
        changed = false;
    }

    /** TODO return false to signal to the bag to remove this item */
    public void commit() {
        float[] b = this.b;
        b[0] = clamp(b[0] + b[3]);
        b[1] = clamp(b[1] + b[4]);
        b[2] = clamp(b[2] + b[5]);
        clearDelta();
        //return true;
    }

    @Override public final float getScore() {
        return pri();
    }

    @Override
    public final float getPriority() {
        return b[0];
    }

    protected void setValue(int x, float v) {
        float[] b = this.b;
        b[x+3] += clamp(v - b[x]);
        changed = true;
    }

    @Override
    public void setPriority(float p) {
        setValue(0, p);
    }

    @Override
    public float getDurability() {
        return b[1];
    }

    @Override
    public void setDurability(float d) {
        setValue(1, d);
    }

    @Override
    public float getQuality() {
        return b[2];
    }

    @Override
    public void setQuality(float q) {
        setValue(2, q);
    }

    @Override
    public long setLastForgetTime(long currentTime) {
        long lastForget = this.lastForget;
        long diff;
        if (lastForget == Tense.TIMELESS) {
            diff = 0;
        } else {
            diff = currentTime - lastForget;
            if (diff == 0) return 0;
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

    public boolean hasDelta() {
        return changed;
//        float[] b = this.b;
//        return nonZero(b[3]) || nonZero(b[4]) || nonZero(b[5]);
    }

//    static boolean nonZero(float x) {
//        //return (Math.abs(x) > Global.BUDGET_EPSILON);
//        return x!=0f;
//    }
}
