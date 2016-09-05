package nars.link;

import nars.Param;
import nars.budget.Budget;
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


    /** priority */
    private float PRI;


    /** durability */
    private float DUR;


    /** quality */
    private float QUA;



    public DefaultBLink(@NotNull X id, float p, float d, float q) {
        budget(p, d, q);
    }

    public DefaultBLink(@NotNull X id, @NotNull Budgeted b) {
        this(id, b, 1f);
    }

    public DefaultBLink(@NotNull X id, @NotNull Budgeted b, float scale) {
        budget(b.pri() * scale, b.dur(), b.qua());
    }


    @Override
    public final void _setPriority(float p) {
        this.PRI = clamp(p);
    }
    @Override
    public final void _setDurability(float d) {
        this.DUR = clamp(d);
    }
    @Override
    public final void _setQuality(float q) {
        this.QUA = clamp(q);
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


    @Override
    public final @NotNull BLink budget(float p, float d, float q) {
        if (p!=p) //NaN check
            throw new BudgetException();

        PRI = clamp(p);
        DUR = clamp(d);
        QUA = clamp(q);
        return this;
    }

    @Override
    public final float pri() {
        return PRI;
    }



    @Override
    public final float dur() {
        return DUR;
    }



    @Override
    public final float qua() {
        return QUA;
    }



}
