package nars.nal.nal8;

import nars.NAR;
import nars.Narsese;
import nars.nal.AbstractNALTester;
import nars.util.meter.TestNAR;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Supplier;

@RunWith(Parameterized.class)
public class NAL8Test extends AbstractNALTester {

    final int cycles = 250;

    public NAL8Test(Supplier<NAR> b) { super(b); }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable configurations() {
        return AbstractNALTester.nars(8, false);
    }


    @Test
    public void subgoal_2_small() throws Narsese.NarseseException {
        TestNAR tester = test();

        tester.input("(hold:(SELF,y) &&+5 at:(SELF,x))!");

        tester.mustDesire(cycles, "hold:(SELF,y)", 1.0f, 0.81f); // :|:

    }

    @Test
    public void subgoal_2() throws Narsese.NarseseException {
        TestNAR tester = test();

        tester.input("(<(SELF,{t002}) --> hold> &&+5 (<(SELF,{t001}) --> at> &&+5 open({t001})))!");

        tester.mustDesire(cycles, "<(SELF,{t002}) --> hold>",
                1.0f, 0.81f); // :|:

    }

    @Test
    public void goal_deductionWithVariableElmination() throws Narsese.NarseseException {
        TestNAR tester = test();

        tester.input("at:(SELF,{t001})!");
        tester.inputAt(10, "(goto($1) ==>+5 at:(SELF,$1)).");

        tester.mustDesire(cycles, "goto({t001})", 1.0f, 0.81f); // :|:

    }
    @Test
    public void goal_deduction() throws Narsese.NarseseException {
        TestNAR tester = test();
        tester.input("x:y!");
        tester.input("(goto(z) ==>+5 x:y).");
        tester.mustDesire(cycles, "goto(z)", 1.0f, 0.81f); // :|:
    }
    @Test
    public void goal_deduction_alt() throws Narsese.NarseseException {
        TestNAR tester = test();
        tester.input("x:y!");
        tester.input("(goto(x) ==>+5 x:y).");
        tester.mustDesire(cycles, "goto(x)", 1.0f, 0.81f); // :|:
    }
    @Test
    public void goal_deduction_delayed() throws Narsese.NarseseException {
        TestNAR tester = test();

        tester.input("x:y!");
        tester.inputAt(10, "(goto(z) ==>+5 x:y).");
        tester.mustDesire(cycles, "goto(z)", 1.0f, 0.81f); // :|:
    }

    @Test
    public void goal_deduction_tensed() throws Narsese.NarseseException {
        TestNAR tester = test();

        tester.input("goto(x). :\\:");
        tester.inputAt(10, "(goto($1) ==>+5 at:(SELF,$1)).");

        tester.mustBelieve(cycles, "at:(SELF,x)", 1.0f, 0.81f, 0);

    }
}
