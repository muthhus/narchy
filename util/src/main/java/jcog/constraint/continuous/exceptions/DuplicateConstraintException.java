package jcog.constraint.continuous.exceptions;

import jcog.constraint.continuous.ContinuousConstraint;

/**
 * Created by alex on 30/01/15.
 */
public class DuplicateConstraintException extends KiwiException {

    private final ContinuousConstraint constraint;

    public DuplicateConstraintException(ContinuousConstraint constraint) {
        this.constraint = constraint;
    }
}
