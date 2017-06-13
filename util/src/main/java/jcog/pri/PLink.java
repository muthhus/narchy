package jcog.pri;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Immutable PriReference
 */
public class PLink<X> extends AbstractPLink<X> {

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
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    @NotNull
    final public X get() {
        return id;
    }

}
