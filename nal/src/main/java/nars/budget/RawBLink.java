package nars.budget;

import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 7/2/16.
 */
public class RawBLink<X> extends RawBudget {

    @NotNull
    public final X x;

    public RawBLink(@NotNull X x, float p, float d, float q) {
        super(p, d, q);
        if (x == null)
            throw new NullPointerException();
        this.x = x;
    }


    public void deleteFast() {
        priority = Float.NaN;
    }


}
