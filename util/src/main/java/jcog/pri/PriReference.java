package jcog.pri;

import java.util.function.Supplier;

/**
 * prioritized reference
 */
public interface PriReference<X> extends Priority, Supplier<X> {

    //TODO public static class AtomicPriReference<X>
}

