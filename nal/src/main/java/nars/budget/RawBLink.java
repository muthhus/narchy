package nars.budget;

/**
 * Created by me on 7/2/16.
 */
public class RawBLink<X> extends RawBudget {
    public final X x;

    public RawBLink(X x, float p, float d, float q) {
        super(p, d, q);
        this.x = x;
    }
}
