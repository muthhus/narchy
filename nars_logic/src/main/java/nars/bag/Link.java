package nars.bag;

import java.util.function.Supplier;

/**
 * weighted link (reference)
 */
public interface Link<X> extends Supplier<X> {

    /** relative score metric used to rank items */
    float getScore();

}
