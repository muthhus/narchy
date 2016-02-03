package nars.budget;

import nars.data.BudgetedStruct;
import org.jetbrains.annotations.NotNull;

/**
 * Created by jim on 1/6/2016.
 */
public interface Budgeted extends BudgetedStruct {

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
