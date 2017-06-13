package jcog.pri;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.function.Supplier;

public abstract class AbstractPLink<X> extends Pri implements PriReference<X> {

    public AbstractPLink(float p) {
        super(p);
    }


    @Override
    public boolean equals(@NotNull Object that) {
        if (this == that) return true;

        final X x = get();
        return
            (x!=null)
                &&
            (
                x.equals(that)
                    ||
                ((that instanceof Supplier) && x.equals(((Supplier) that).get()))
            );
    }


    @Override
    public abstract int hashCode();

    @Override
    abstract public X get();

    @Override
    public String toString() {
        return get() + "=" + pri();
    }

}
