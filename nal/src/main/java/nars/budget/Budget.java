package nars.budget;

import jcog.bag.Prioritized;
import jcog.bag.Priority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 12/11/15.
 */
public interface Budget extends Priority, Budgeted {
    /**common instance for a 'Deleted budget'.*/
    Priority Deleted = new ROBudget(Float.NaN);
    /** common instance for a 'full budget'.*/
    Priority One = new ROBudget(1f);
    /** common instance for a 'half budget'.*/
    Priority Half = new ROBudget(0.5f);
    /** common instance for a 'zero budget'.*/
    Priority Zero = new ROBudget(0);




    /**
     * copies a budget into this; if source is null, it deletes the budget
     */
    @Override
    @NotNull
    default Priority copyFrom(@Nullable Prioritized srcCopy) {
        if (srcCopy == null) {
            delete();
        } else {
            float p = srcCopy.priSafe(-1);
            if (p < 0) {
                delete();
            } else {
                setPriority(p);
            }
        }

        return this;
    }


    @NotNull
    @Override
    default Priority budget() {
        return this;
    }
}
