package jcog.pri;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Immutable PriReference
 */
public class PLink<X> extends Pri implements PriReference<X> {

    @NotNull protected final X id;

    public PLink(@NotNull X x, float p) {
        super(p);
        this.id = x;
    }

    @Nullable @Override
    public PLink<X> clone() {
        float p = pri();
        return (p==p) ? new PLink<>(id, p) : null;
    }

    @Override
    public boolean equals(@NotNull Object that) {
        return ((this == that) || this.id.equals(that)) ||
                ((that instanceof Supplier) && id.equals(((Supplier) that).get()));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    @NotNull
    final public X get() {
        return id;
    }

    @Override
    public String toString() {
        return id + "=" + pri();
    }

}
