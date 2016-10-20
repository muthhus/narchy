package nars.util;

import nars.util.event.On;

import java.util.function.Consumer;

/**
 * Created by me on 10/20/16.
 */
public interface Iterative<X> {
    On onFrame(Consumer<X> x);
}
