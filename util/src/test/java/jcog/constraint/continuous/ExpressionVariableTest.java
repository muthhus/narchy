package jcog.constraint.continuous;

import jcog.constraint.continuous.exceptions.DuplicateConstraintException;
import jcog.constraint.continuous.exceptions.UnsatisfiableConstraintException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by alex on 31/01/16.
 */
public class ExpressionVariableTest {
    private static double EPSILON = 1.0e-8;

    @Test
    public void lessThanEqualTo() throws DuplicateConstraintException, UnsatisfiableConstraintException {
        DoubleVar x = new DoubleVar("x");
        ContinuousConstraintSolver solver = new ContinuousConstraintSolver();
        solver.add(C.lessThanOrEqualTo(new Expression(100), x));
        solver.update();
        assertTrue(100 <= x.value());
        solver.add(C.equals(x, 110));
        solver.update();
        assertEquals(x.value(), 110, EPSILON);
    }

    @Test(expected = UnsatisfiableConstraintException.class)
    public void lessThanEqualToUnsatisfiable() throws DuplicateConstraintException, UnsatisfiableConstraintException {
        DoubleVar x = new DoubleVar("x");
        ContinuousConstraintSolver solver = new ContinuousConstraintSolver();
        solver.add(C.lessThanOrEqualTo(new Expression(100), x));
        solver.update();
        assertTrue(x.value() <= 100);
        solver.add(C.equals(x, 10));
        solver.update();
    }

    @Test
    public void greaterThanEqualTo() throws DuplicateConstraintException, UnsatisfiableConstraintException {
        DoubleVar x = new DoubleVar("x");
        ContinuousConstraintSolver solver = new ContinuousConstraintSolver();
        solver.add(C.greaterThanOrEqualTo(new Expression(100), x));
        solver.update();
        assertTrue(100 >= x.value());
        solver.add(C.equals(x, 90));
        solver.update();
        assertEquals(x.value(), 90, EPSILON);
    }

    @Test(expected = UnsatisfiableConstraintException.class)
    public void greaterThanEqualToUnsatisfiable() throws DuplicateConstraintException, UnsatisfiableConstraintException {
        DoubleVar x = new DoubleVar("x");
        ContinuousConstraintSolver solver = new ContinuousConstraintSolver();
        solver.add(C.greaterThanOrEqualTo(new Expression(100), x));
        solver.update();
        assertTrue(100 >= x.value());
        solver.add(C.equals(x, 110));
        solver.update();
    }
}
