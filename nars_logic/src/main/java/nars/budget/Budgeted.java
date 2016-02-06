package nars.budget;

import nars.Memory;
import nars.data.BudgetedStruct;
import org.jetbrains.annotations.NotNull;

/**
 * Created by jim on 1/6/2016.
 */
public interface Budgeted extends BudgetedStruct {

    static float getPrioritySum(@NotNull Iterable<? extends Budgeted> c) {
        float totalPriority = 0;
        for (Budgeted i : c)
            totalPriority+=i.pri();
        return totalPriority;
    }

    /** randomly selects an item from a collection, weighted by priority */
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

	default float summary() {
		return budget().summary();
	}
        
        default float pri() {
            return getPriority();
        }

        default float qua() {
            return getQuality();
        }

        default float dur() {
            return getDurability();
        }
        
        default long lastForgetTime() { return getLastForgetTime(); }
        
        default boolean isDeleted() { return getDeleted(); }
}
