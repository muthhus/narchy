package jcog.constraint.continuous;

import jcog.constraint.continuous.exceptions.DuplicateConstraintException;
import jcog.constraint.continuous.exceptions.UnsatisfiableConstraintException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Created by alex on 31/01/16.
 */
public class VariableVariableTest {

    private static double EPSILON = 1.0e-8;

    @Test
    public void lessThanEqualTo() throws DuplicateConstraintException, UnsatisfiableConstraintException {
        ContinuousConstraintSolver solver = new ContinuousConstraintSolver();

        DoubleVar x = new DoubleVar("x");
        DoubleVar y = new DoubleVar("y");

        solver.add(C.equals(y, 100));
        solver.add(C.lessThanOrEqualTo(x, y));

        solver.update();
        assertTrue(x.value() <= 100);
        solver.add(C.equals(x, 90));
        solver.update();
        assertEquals(x.value(), 90, EPSILON);
    }

    @Test
    public void lessThanEqualToUnsatisfiable() throws DuplicateConstraintException, UnsatisfiableConstraintException {
        assertThrows(UnsatisfiableConstraintException.class, () -> {
            ContinuousConstraintSolver solver = new ContinuousConstraintSolver();

            DoubleVar x = new DoubleVar("x");
            DoubleVar y = new DoubleVar("y");

            solver.add(C.equals(y, 100));
            solver.add(C.lessThanOrEqualTo(x, y));

            solver.update();
            assertTrue(x.value() <= 100);
            solver.add(C.equals(x, 110));
            solver.update();

        });
    }

    @Test
    public void greaterThanEqualTo() throws DuplicateConstraintException, UnsatisfiableConstraintException {
        ContinuousConstraintSolver solver = new ContinuousConstraintSolver();

        DoubleVar x = new DoubleVar("x");
        DoubleVar y = new DoubleVar("y");

        solver.add(C.equals(y, 100));
        solver.add(C.greaterThanOrEqualTo(x, y));

        solver.update();
        assertTrue(x.value() >= 100);
        solver.add(C.equals(x, 110));
        solver.update();
        assertEquals(x.value(), 110, EPSILON);
    }

    @Test
    public void greaterThanEqualToUnsatisfiable() throws DuplicateConstraintException, UnsatisfiableConstraintException {
        assertThrows(UnsatisfiableConstraintException.class, () -> {
            ContinuousConstraintSolver solver = new ContinuousConstraintSolver();

            DoubleVar x = new DoubleVar("x");
            DoubleVar y = new DoubleVar("y");

            solver.add(C.equals(y, 100));

            solver.add(C.greaterThanOrEqualTo(x, y));
            solver.update();
            assertTrue(x.value() >= 100);
            solver.add(C.equals(x, 90));
            solver.update();
        });
    }
}
