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
public class NAL8NewTest extends AbstractNALTester {

    final int cycles = 250;

    public NAL8NewTest(Supplier<NAR> b) { super(b); }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable configurations() {
        return AbstractNALTester.nars(8, false);
    }



    @Test
    public void subgoal_2() throws Narsese.NarseseException {
        TestNAR tester = test();

        tester.input("((<(SELF,{t002}) --> hold> &&+5 <(SELF,{t001}) --> at>) &&+5 open({t001})))!");

        tester.mustDesire(cycles, "<(SELF,{t002}) --> hold>",
                1.0f, 0.81f); // :|:

    }

    @Test
    public void goal_deductionWithVariableElmination() throws Narsese.NarseseException {
        TestNAR tester = test();

        tester.nar.log();
        tester.input("<(SELF,{t001}) --> at>!");
        tester.inputAt(10, "(goto($1) ==>+5 <(SELF,$1) --> at>).");

        tester.mustDesire(cycles, "goto({t001})", 1.0f, 0.81f); // :|:

    }
    @Test
    public void goal_deduction() throws Narsese.NarseseException {
        TestNAR tester = test();

        tester.nar.log();
        tester.input("x:y!");
        tester.input("(goto(z) ==>+5 x:y).");

        tester.mustDesire(cycles, "goto(z)", 1.0f, 0.81f); // :|:

    }


}
