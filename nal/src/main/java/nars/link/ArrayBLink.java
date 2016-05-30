package nars.link;

import nars.Global;
import nars.budget.Budgeted;
import org.jetbrains.annotations.NotNull;

import static nars.util.data.Util.clamp;

/**
 * Buffered/Budgeted Link (an entry in a bag)
 * equalsTo/hashCode proxies to the wrapped element, X id
 *
 * Acts as a "budget vector" containing an accumulating delta
 * that can be commit()'d on the next udpate
 */
abstract public class ArrayBLink<X> extends BLink<X> {

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
    //final static int BUFFERED = 6;

    /** time of last forget */
    final static int LASTFORGET = 6;

    private final float[] b = new float[7];

    public ArrayBLink(X id, float p, float d, float q) {
        init(p, d, q);
    }

    public ArrayBLink(X id, @NotNull Budgeted b) {

        this(id, b, 1f);
    }

    public ArrayBLink(X id, @NotNull Budgeted b, float scale) {
        init(b, scale);
    }

    @Override public void init(float p, float d, float q) {
        float[] b = this.b;
        b[PRI] = clamp(p);
        b[DUR] = clamp(d);
        b[QUA] = clamp(q);
        b[LASTFORGET] = Float.NaN;
    }

    @Override
    public boolean delete() {
        float p = pri();
        if (p==p) {
            //not already deleted
            b[PRI] = (Float.NaN);
            changed = true;
            return true;
        }
        return false;
    }


    @Override
    public boolean commit() {
        if (changed) {
            float[] b = this.b;
            float p = b[PRI];
            if (p == p) /* not NaN */ {
                b[PRI] = clamp(   p   + b[DPRI]);   b[DPRI] = 0;
                b[DUR] = clamp(b[DUR] + b[DDUR]);   b[DDUR] = 0;
                b[QUA] = clamp(b[QUA] + b[DQUA]);   b[DQUA] = 0;
            }
            changed = false;
            return true;
        }
        return false;
    }

    @Override
    public final float pri() {
        return b[0 /*PRI*/];
    }

    protected final void setValue(int x, float v) {
        float[] b = this.b;
        int twoX = 2 * x;
        float delta = v - b[twoX];
        b[twoX + 1] += delta;
        changed = true;
    }

    @Override
    public final void _setPriority(float p) {
        setValue(0, p);
    }

    @Override
    public final float dur() {
        return b[DUR];
    }

    @Override
    public final void _setDurability(float d) {
        setValue(1, d);
    }

    @Override
    public final float qua() {
        return b[QUA];
    }

    @Override
    public final void _setQuality(float q) {
        setValue(2, q);
    }

    @Override
    @Deprecated public final float setLastForgetTime(float currentTime) {
        float[] b = this.b;
        float lastForget = b[LASTFORGET];
        float diff = (lastForget != lastForget /* NaN test */) ? Global.SUBFRAME_EPSILON : (currentTime - lastForget);
        setLastForgetTimeFast(currentTime);
        return diff;
    }

    /** doesnt compute the delta */
    @Override
    public final void setLastForgetTimeFast(float currentTime) {
        b[LASTFORGET] = currentTime;
    }

    @Override
    public final float getLastForgetTime() {
        return b[LASTFORGET];
    }



}
