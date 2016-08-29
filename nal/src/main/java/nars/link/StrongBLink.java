package nars.link;

import nars.budget.Budgeted;
import org.jetbrains.annotations.NotNull;


public class StrongBLink<X> extends DefaultBLink<X> {

    ///** the referred item */
    @NotNull
    public final X id;

    public StrongBLink(@NotNull X id, Budgeted b) {
        this(id, b.pri(), b.dur(), b.qua());
    }

    public StrongBLink(@NotNull X id, float p, float d, float q) {
        super(id, p, d, q);
        this.id = id;
    }

    @NotNull
    @Override
    public final X get() {
        return id;
    }

}
