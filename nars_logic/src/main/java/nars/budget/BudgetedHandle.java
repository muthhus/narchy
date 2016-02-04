package nars.budget;

/**
 * indicates an implementation has, or is associated with a specific BudgetValue
 */
public abstract class BudgetedHandle implements Budgeted {

	public abstract void setPriority(float p);

	abstract long setLastForgetTime(long currentTime);
}
