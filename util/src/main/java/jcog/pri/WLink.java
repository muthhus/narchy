package jcog.pri;

/**
 * Weighted reference
 * similar to PLink but has no assumption or limitation to the 0..1.0 range
 */
public class WLink<T> extends PLink<T>  {

    public WLink(T n, float weight) {
        super(n, weight);
    }

}
