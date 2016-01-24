package nars.budget;

/**
 * indicates an implementation has, or is associated with a specific BudgetValue
 */
public abstract class BudgetedHandle implements Budgeted {


    @Override
    public boolean isDeleted() {
        return Budget.isDeleted(pri());
    }

    @Override
    public float pri() {
        return budget().pri();
    }

    @Override
    public float dur() {
        return budget().dur();
    }

    @Override
    public float qua() {
        return budget().qua();
    }

    @Override
    public long lastForgetTime() {
        return budget().lastForgetTime();
    }

    public abstract void setPriority(float p);

    abstract long setLastForgetTime(long currentTime);
}
