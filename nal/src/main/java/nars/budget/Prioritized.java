package nars.budget;

import nars.NAR;
import org.jetbrains.annotations.NotNull;

/**
 * something which has a priority floating point value
 */
public interface Prioritized {

    float pri();

    default float priSafe(float valueIfInactive) {
        float p = pri();
        return p == p ? p : valueIfInactive;
    }

    /** the result of this should be that pri() is not finite (ex: NaN)
     * returns false if already deleted (allowing overriding subclasses to know if they shold also delete) */
    boolean delete();

    default boolean isDeleted() {
        float p = pri();
        return p!=p; //fast NaN check
    }


    static float priSum(@NotNull Iterable<? extends Prioritized> c) {
        float totalPriority = 0;
        for (Prioritized i : c)
            totalPriority += i.priSafe(0);
        return totalPriority;
    }

    static void normalizePriSum(@NotNull Iterable<? extends Budgeted> l, float total) {

        float priSum = Prioritized.priSum(l);
        float mult = total / priSum;
        for (Budgeted b : l) {
            b.budget().priMult(mult);
        }

    }

    /**
     * randomly selects an item from a collection, weighted by priority
     */
    static <P extends Prioritized> P selectRandomByPriority(@NotNull NAR memory, @NotNull Iterable<P> c) {
        float totalPriority = priSum(c);

        if (totalPriority == 0) return null;

        float r = memory.random.nextFloat() * totalPriority;

        P s = null;
        for (P i : c) {
            s = i;
            r -= s.priSafe(0);
            if (r < 0)
                return s;
        }

        return s;

    }

}
