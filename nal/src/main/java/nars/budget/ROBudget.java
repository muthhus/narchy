package nars.budget;

import jcog.bag.Priority;
import org.jetbrains.annotations.NotNull;

/**
 * reverse osmosis read-only budget
 */
public final class ROBudget implements Priority {

    private final float pri;

    public ROBudget(float pri) {
        this.pri = pri;
    }

    @Override
    public final boolean isDeleted() {
        return false;
    }

    @Override
    public boolean delete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPriority(float p) {
        throw new UnsupportedOperationException();
    }


    @NotNull
    @Override
    public Priority clone() {
        return new RawBudget(this);
    }


    @Override
    public final float pri() {
        return pri;
    }


    /**
     * Fully display the BudgetValue
     *
     * @return String representation of the value
     */
    @NotNull
    @Override
    public final String toString() {
        return getBudgetString();
    }

}
