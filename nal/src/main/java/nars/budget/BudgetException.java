package nars.budget;

import nars.util.SoftException;


public final class BudgetException extends SoftException {
    public BudgetException() {
        super("NaN");
    }

    public BudgetException(String message) {
        super(message);
    }
}
