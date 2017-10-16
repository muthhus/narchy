package jcog.pri;

import jcog.math.FloatSupplier;

import java.util.function.Supplier;

/**
 * prioritized reference
 */
public interface PriReference<X> extends Priority, Supplier<X>, FloatSupplier {

    @Override
    default float asFloat() {
        return pri();
    }
}

