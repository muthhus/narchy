package nars.budget;

import org.jetbrains.annotations.NotNull;

/**
 * reverse osmosis read-only budget
 */
public final class ROBudget implements Budget {

    private final float pri, qua;

    public ROBudget(float pri, float qua) {
        this.pri = pri;
        this.qua = qua;
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


    @Override
    public void setQua(float q) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Budget clone() {
        return new RawBudget(this);
    }

    @NotNull
    @Override
    public Budget setBudget(float p, float q) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final float pri() {
        return pri;
    }

    @Override
    public final float qua() {
        return qua;
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
