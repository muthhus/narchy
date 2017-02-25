package nars.budget;

import jcog.Util;
import jcog.bag.Prioritized;
import nars.Param;
import org.jetbrains.annotations.NotNull;

import static nars.budget.Budget.aveGeo;

//import nars.data.BudgetedStruct;

/**
 * budget = (pri, qua)
 *      pri = priority,
 *      qua = quality, or 2nd-order/long-term prioritization
 */
public interface Budgeted extends Prioritized {

    @NotNull
    Budget budget();

    /**
     * To summarize a BudgetValue into a single number in [0, 1]
     *
     * @return The summary value
     */
    default float summary() {
        return aveGeo(pri(), 0, qua());
    }


    float qua();



    default boolean equalsBudget(@NotNull Budgeted t, float epsilon) {
        return Util.equals(pri(), t.pri(), epsilon) &&
                Util.equals(qua(), t.qua(), epsilon);
    }

    default boolean equalsBudget(@NotNull Budgeted b) {
        return equalsBudget(b, Param.BUDGET_EPSILON);
    }


    default void setBudget(Budget b) {
        budget().setBudget(b);
    }
}
