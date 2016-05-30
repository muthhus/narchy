package nars.bag;

import nars.budget.Budgeted;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 5/29/16.
 */
public class StrongBLink<X> extends ArrayBLink<X> {

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
