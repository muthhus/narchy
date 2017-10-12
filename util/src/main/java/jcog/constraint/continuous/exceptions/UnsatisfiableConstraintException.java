package jcog.constraint.continuous.exceptions;

import jcog.constraint.continuous.ContinuousConstraint;

/**
 * Created by alex on 30/01/15.
 */
public class UnsatisfiableConstraintException extends KiwiException {

    private final ContinuousConstraint constraint;
    public UnsatisfiableConstraintException(ContinuousConstraint constraint) {
        super(constraint.toString());
        this.constraint = constraint;
    }
}
