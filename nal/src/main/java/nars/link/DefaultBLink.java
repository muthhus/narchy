package nars.link;

import nars.Global;
import nars.budget.Budgeted;
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


    public DefaultBLink(X id, float p, float d, float q) {
        init(p, d, q);
    }

    public DefaultBLink(X id, @NotNull Budgeted b) {
        this(id, b, 1f);
    }

    public DefaultBLink(X id, @NotNull Budgeted b, float scale) {
        init(b, scale);
    }

    @Override public void init(float p, float d, float q) {
        PRI = clamp(p);
        DUR = clamp(d);
        QUA = clamp(q);
    }

    @Override
    public boolean delete() {
        float p = pri();
        if (p==p) {
            //not already deleted
            PRI = (Float.NaN);
            changed = true;
            return true;
        }
        return false;
    }


    @Override public final float priDelta() {
        return dPri;
    }

    @Override
    public boolean commit() {
        if (changed) {
            float p = PRI;
            if (p == p) /* not NaN */ {
                PRI = clamp( p  + dPri);   dPri = 0;
                DUR = clamp(DUR + dDur);   dDur = 0;
                QUA = clamp(QUA + dQua);   dQua = 0;
            }
            changed = false;
            return true;
        }
        return false;
    }

    @Override
    public final float pri() {
        return PRI;
    }
    
    @Override
    public final void _setPriority(float p) {
        dPri += p - PRI;
        changed = true;
    }

    @Override
    public final float dur() {
        return DUR;
    }

    @Override
    public final void _setDurability(float d) {
        dDur += d - DUR;
        changed = true;
    }

    @Override
    public final float qua() {
        return QUA;
    }

    @Override
    public final void _setQuality(float q) {
        dQua += q - QUA;
        changed = true;
    }



    @Override public String toString2() {
        return toString() + "+/-:" + dPri + ';' + dDur + ';' + dQua;
    }


}
