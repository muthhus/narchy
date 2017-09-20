package jcog.util;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/** atomic switching double buffer */
public class DoubleBuffer<X> extends AtomicBoolean {

    private final X a;
    private final X b;

    public DoubleBuffer(Supplier<X> builder) {
        this.a = builder.get();
        this.b = builder.get();
    }


    public X write() {
        return getAndSet(!get()) ? a : b;
    }
    public X preWrite() {
        return !get() ? a : b;
    }
    public X read() {
        return get() ? a : b;
    }


}
