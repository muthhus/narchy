package nars.budget;

import nars.data.BudgetedStruct;
import org.jetbrains.annotations.NotNull;

/**
 * Created by jim on 1/6/2016.
 */
public interface Budgeted extends BudgetedStruct {

	@NotNull
	Budget budget();

}
