package nars.link;

import nars.budget.Budgeted;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class StrongBLink<X> extends DefaultBLink<X> {

    ///** the referred item */
    @Nullable
    public X id;

    public StrongBLink(@NotNull X id, Budgeted b) {
        this(id, b.pri(), b.dur(), b.qua());
    }

    public StrongBLink(@NotNull X id, float p, float d, float q) {
        super(id, p, d, q);
        this.id = id;
    }

    @Override
    public boolean delete() {
        id = null;
        return super.delete();
    }

    @Nullable
    @Override
    public final X get() {
        return id;
    }

}
