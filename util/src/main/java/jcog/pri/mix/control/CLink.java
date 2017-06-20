package jcog.pri.mix.control;

import jcog.pri.Priority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;

/** Priority implementatio nwhich proxies to another and attaches a bitmap feature vector */
public class CLink<X extends Priority> extends RoaringBitmap implements Priority {

    public final X ref;

    public CLink(X ref, int... initialBits) {
        super();
        this.ref = ref;
        for (int i : initialBits)
            add(i);
    }

    @Override
    public int hashCode() {
        return ref.hashCode();
    }

    @Override
    public boolean equals(@NotNull Object o) {
        if (this == o) return true;
        if (this.ref == o) return true;
        CLink c = (CLink)o; //assumed
        return ref.equals(c.ref);
    }

    @Override
    public float setPri(float p) {
        return ref.setPri(p);
    }

    @Override
    public @Nullable Priority clonePri() {
        return ref.clonePri();
    }

    @Override
    public float pri() {
        return ref.pri();
    }

    @Override
    public boolean delete() {
        if (ref.delete()) {
            clear();
            return true;
        }
        return false;
    }

    @Override
    public boolean isDeleted() {
        return ref.isDeleted();
    }
}
