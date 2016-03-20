package nars.budget;

import nars.Memory;
import org.jetbrains.annotations.NotNull;

import static nars.budget.Budget.aveGeo;

//import nars.data.BudgetedStruct;

/**
 * Created by jim on 1/6/2016.
 */
public interface Budgeted  {

    static float getPrioritySum(@NotNull Iterable<? extends Budgeted> c) {
        float totalPriority = 0;
        for (Budgeted i : c)
            totalPriority += i.pri();
        return totalPriority;
    }

    /**
     * randomly selects an item from a collection, weighted by priority
     */
    static <E extends Budgeted> E selectRandomByPriority(@NotNull Memory memory, @NotNull Iterable<E> c) {
        float totalPriority = getPrioritySum(c);

        if (totalPriority == 0) return null;

        float r = memory.random.nextFloat() * totalPriority;

        E s = null;
        for (E i : c) {
            s = i;
            r -= s.pri();
            if (r < 0)
                return s;
        }

        return s;

    }

    @NotNull
    Budget budget();

    /**
     * To summarize a BudgetValue into a single number in [0, 1]
     *
     * @return The summary value
     */
    default float summary() {
        return aveGeo(pri(), dur(), qua());
    }

    float pri();

    float qua();

    float dur();

    long getLastForgetTime();


    //        default long lastForgetTime() { return getLastForgetTime(); }
//
    default boolean	isDeleted() {
        return !Float.isFinite(pri());
    }

    default float priIfFiniteElseZero() {
        return priIfFiniteElse(0);
    }

    default float priIfFiniteElse(float ifNonFinite) {
        float p = pri();
        return Float.isFinite(p) ? p : ifNonFinite;
    }

}
