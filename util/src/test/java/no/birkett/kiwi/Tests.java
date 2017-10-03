package no.birkett.kiwi;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class Tests {

    private static double EPSILON = 1.0e-8;

    @Test
    public void simpleNew() throws UnsatisfiableConstraintException, DuplicateConstraintException {
        Solver solver = new Solver();
        Variable x = new Variable("x");


        solver.add(C.equals(C.add(x, 2), 20));

        solver.update();

        assertEquals(x.value(), 18, EPSILON);
    }

    @Test
    public void simple0() throws UnsatisfiableConstraintException, DuplicateConstraintException {
        Solver solver = new Solver();
        Variable x = new Variable("x");
        Variable y = new Variable("y");

        solver.add(C.equals(x, 20));

        solver.add(C.equals(C.add(x, 2), C.add(y, 10)));

        solver.update();

        System.out.println("x " + x.value() + " y " + y.value());

        assertEquals(y.value(), 12, EPSILON);
        assertEquals(x.value(), 20, EPSILON);
    }

    @Test
    public void simple1() throws DuplicateConstraintException, UnsatisfiableConstraintException {
        Variable x = new Variable("x");
        Variable y = new Variable("y");
        Solver solver = new Solver();
        solver.add(C.equals(x, y));
        solver.update();
        assertEquals(x.value(), y.value(), EPSILON);
    }

    @Test
    public void casso1() throws DuplicateConstraintException, UnsatisfiableConstraintException {
        Variable x = new Variable("x");
        Variable y = new Variable("y");
        Solver solver = new Solver();

        solver.add(C.lessThanOrEqualTo(x, y));
        solver.add(C.equals(y, C.add(x, 3.0)));
        solver.add(C.equals(x, 10.0).setStrength(Strength.WEAK));
        solver.add(C.equals(y, 10.0).setStrength(Strength.WEAK));

        solver.update();

        if (Math.abs(x.value() - 10.0) < EPSILON) {
            assertEquals(10, x.value(), EPSILON);
            assertEquals(13, y.value(), EPSILON);
        } else {
            assertEquals(7, x.value(), EPSILON);
            assertEquals(10, y.value(), EPSILON);
        }
    }

    @Test
    public void addDelete1() throws DuplicateConstraintException, UnsatisfiableConstraintException, UnknownConstraintException {
        Variable x = new Variable("x");
        Solver solver = new Solver();

        solver.add(C.lessThanOrEqualTo(x, 100).setStrength(Strength.WEAK));

        solver.update();
        assertEquals(100, x.value(), EPSILON);

        Constraint c10 = C.lessThanOrEqualTo(x, 10.0);
        Constraint c20 = C.lessThanOrEqualTo(x, 20.0);

        solver.add(c10);
        solver.add(c20);

        solver.update();

        assertEquals(10, x.value(), EPSILON);

        solver.remove(c10);

        solver.update();

        assertEquals(20, x.value(), EPSILON);

        solver.remove(c20);
        solver.update();

        assertEquals(100, x.value(), EPSILON);

        Constraint c10again = C.lessThanOrEqualTo(x, 10.0);

        solver.add(c10again);
        solver.add(c10);
        solver.update();

        assertEquals(10, x.value(), EPSILON);

        solver.remove(c10);
        solver.update();
        assertEquals(10, x.value(), EPSILON);

        solver.remove(c10again);
        solver.update();
        assertEquals(100, x.value(), EPSILON);
    }

    @Test
    public void addDelete2() throws DuplicateConstraintException, UnsatisfiableConstraintException, UnknownConstraintException {
        Variable x = new Variable("x");
        Variable y = new Variable("y");
        Solver solver = new Solver();

        solver.add(C.equals(x, 100).setStrength(Strength.WEAK));
        solver.add(C.equals(y, 120).setStrength(Strength.STRONG));

        Constraint c10 = C.lessThanOrEqualTo(x, 10.0);
        Constraint c20 = C.lessThanOrEqualTo(x, 20.0);

        solver.add(c10);
        solver.add(c20);
        solver.update();

        assertEquals(10, x.value(), EPSILON);
        assertEquals(120, y.value(), EPSILON);

        solver.remove(c10);
        solver.update();

        assertEquals(20, x.value(), EPSILON);
        assertEquals(120, y.value(), EPSILON);

        Constraint cxy = C.equals(C.multiply(x, 2.0), y);
        solver.add(cxy);
        solver.update();

        assertEquals(20, x.value(), EPSILON);
        assertEquals(40, y.value(), EPSILON);

        solver.remove(c20);
        solver.update();

        assertEquals(60, x.value(), EPSILON);
        assertEquals(120, y.value(), EPSILON);

        solver.remove(cxy);
        solver.update();

        assertEquals(100, x.value(), EPSILON);
        assertEquals(120, y.value(), EPSILON);
    }

    @Test(expected = UnsatisfiableConstraintException.class)
    public void inconsistent1() throws InternalError, DuplicateConstraintException, UnsatisfiableConstraintException {
        Variable x = new Variable("x");
        Solver solver = new Solver();

        solver.add(C.equals(x, 10.0));
        solver.add(C.equals(x, 5.0));

        solver.update();
    }

    @Test(expected = UnsatisfiableConstraintException.class)
    public void inconsistent2() throws DuplicateConstraintException, UnsatisfiableConstraintException {
        Variable x = new Variable("x");
        Solver solver = new Solver();

        solver.add(C.greaterThanOrEqualTo(x, 10.0));
        solver.add(C.lessThanOrEqualTo(x, 5.0));
        solver.update();
    }

    @Test(expected = UnsatisfiableConstraintException.class)
    public void inconsistent3() throws DuplicateConstraintException, UnsatisfiableConstraintException {

        Variable w = new Variable("w");
        Variable x = new Variable("x");
        Variable y = new Variable("y");
        Variable z = new Variable("z");
        Solver solver = new Solver();

        solver.add(C.greaterThanOrEqualTo(w, 10.0));
        solver.add(C.greaterThanOrEqualTo(x, w));
        solver.add(C.greaterThanOrEqualTo(y, x));
        solver.add(C.greaterThanOrEqualTo(z, y));
        solver.add(C.greaterThanOrEqualTo(z, 8.0));
        solver.add(C.lessThanOrEqualTo(z, 4.0));
        solver.update();
    }

}
