package nars.link;

import nars.budget.Budgeted;
import org.jetbrains.annotations.NotNull;


public class StrongBLink<X> extends DefaultBLink<X> {



    public StrongBLink(X id, Budgeted b) {
        this(id, b.pri(), b.dur(), b.qua());
    }

    public StrongBLink(X id, float p, float d, float q) {
        super(id, p, d, q);
        this.id = id;
    }

    @Override
    public X get() {
        return id;
    }

    public final void set(X id) {
        this.id = id;
    }
}
