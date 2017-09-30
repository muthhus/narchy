package no.birkett.kiwi;

/**
 * Created by alex on 30/01/15.
 */
public class DuplicateConstraintException extends KiwiException {

    private final Constraint constraint;

    public DuplicateConstraintException(Constraint constraint) {
        this.constraint = constraint;
    }
}
