package nars.link;

import nars.Param;
import nars.budget.Budgeted;
import nars.util.Util;
import org.jetbrains.annotations.NotNull;

import static nars.util.Util.clamp;

/**
 * Buffered/Budgeted Link (an entry in a bag)
 * equalsTo/hashCode proxies to the wrapped element, X id
 *
 * Acts as a "budget vector" containing an accumulating delta
 * that can be commit()'d on the next udpate
 */
abstract public class DefaultBLink<X> extends BLink<X> {

    /** changed status bit */
    boolean changed;

    /** priority */
    private float PRI;
    /** delta pri */
    private float dPri;

    /** durability */
    private float DUR;
    /** delta dur */
    private float dDur;

    /** quality */
    private float QUA;
    /** delta qua */
    private float dQua;


    public DefaultBLink(@NotNull X id, float p, float d, float q) {
        init(p, d, q);
    }

    public DefaultBLink(@NotNull X id, @NotNull Budgeted b) {
        this(id, b, 1f);
    }

    public DefaultBLink(@NotNull X id, @NotNull Budgeted b, float scale) {
        init(b, scale);
    }


    @Override public void init(float p, float d, float q) {
        PRI = clamp(p);
        DUR = clamp(d);
        QUA = clamp(q);
    }

    @Override
    public boolean delete() {
        float p = PRI;
        if (p==p) {
            //not already deleted
            this.PRI = (Float.NaN);
            return true;
        }
        return false;
    }


    @Override public final float priDelta() {
        return dPri;
    }

    @Override
    public void commit() {
        if (changed) {
            float p = PRI;
            if (p == p) /* not NaN */ {

                PRI = clamp( p  + dPri);   dPri = 0;
                DUR = clamp(DUR + dDur);   dDur = 0;
                QUA = clamp(QUA + dQua);   dQua = 0;


            }
            changed = false;
        }
    }

    @Override
    public final float pri() {
        return PRI;
    }

    @Override
    public float priNext() {
        return Util.clamp(PRI + dPri);
    }

    @Override
    public float durNext() {
        return Util.clamp(DUR + dDur);
    }

    public float quaNext() {
        return Util.clamp(QUA + dQua);
    }

    @Override
    public final void _setPriority(float p) {
        float delta = p - priNext();
        if (delta >= Param.BUDGET_EPSILON || delta <= -Param.BUDGET_EPSILON) {
            dPri += delta;
            changed = true;
        } /*else {
            throw new RuntimeException("insignificant priority change detected");
        }*/
    }

    @Override
    public final float dur() {
        return DUR;
    }

    @Override
    public final void _setDurability(float d) {
        float delta = d - durNext();
        if (delta >= Param.BUDGET_EPSILON || delta <= -Param.BUDGET_EPSILON) {
            dDur += delta;

            changed = true;
        }
    }

    @Override
    public final float qua() {
        return QUA;
    }

    @Override
    public final void _setQuality(float q) {
        float delta = q - quaNext();
        if (delta >= Param.BUDGET_EPSILON || delta <= -Param.BUDGET_EPSILON) {
            dQua += delta;
            changed = true;
        }
    }



    @NotNull
    @Override public String toString2() {
        return toString() + "+/-:" + dPri + ';' + dDur + ';' + dQua;
    }


}
