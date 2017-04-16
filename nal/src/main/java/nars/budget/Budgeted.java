package nars.budget;

import jcog.bag.Prioritized;
import jcog.bag.Priority;
import org.jetbrains.annotations.NotNull;


//import nars.data.BudgetedStruct;

/**
 * budget = (pri, qua)
 *      pri = priority,
 *      qua = quality, or 2nd-order/long-term prioritization
 */
public interface Budgeted extends Prioritized {

    @NotNull
    Priority budget();

}
