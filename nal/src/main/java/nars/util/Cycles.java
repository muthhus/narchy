package nars.util;

import jcog.event.On;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Created by me on 10/20/16.
 */
public interface Cycles<X> {
    @NotNull On onCycle(Consumer<X> x);
}
