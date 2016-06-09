package nars.link;

import nars.budget.Budgeted;
import org.jetbrains.annotations.NotNull;


public class StrongBLink<X> extends DefaultBLink<X> {

    ///** the referred item */
    public final X id;

    public StrongBLink(X id, @NotNull Budgeted b, float scal) {
        super(id, b, scal);
        this.id = id;
    }

    @NotNull
    @Override
    public final X get() {
        return id;
    }

}
