package nars.nal.nal8;

import nars.NAR;
import nars.nal.AbstractNALTester;
import nars.util.meter.TestNAR;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Supplier;

@RunWith(Parameterized.class)
public class NAL8Test extends AbstractNALTester {

    final int cycles = 150; //150 worked for most of the initial NAL8 tests converted

    public NAL8Test(Supplier<NAR> b) { super(b); }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable configurations() {
        return AbstractNALTester.nars(8, false);
    }


    @Test
    public void subsent_1()  {
        TestNAR tester = test();

        //TODO decide correct parentheses ordering

        //tester.nar.log();
        tester.input("[opened]:{t001}. :|:");
        tester.inputAt(10, "(hold:({t002}) &&+5 (at:({t001}) &&+5 (open({t001}) &&+5 [opened]:{t001}))).");

        tester.mustBelieve(cycles, "(hold:({t002}) &&+5 (at:({t001}) &&+5 open({t001})))",
                1.0f, 0.45f,
                -5);
    }

    @Test
    public void subsent_1_simpler()  {
        TestNAR tester = test();

        //TODO decide correct parentheses ordering

        //tester.nar.log();
        tester.input("[opened]:t1. :|:");
        tester.inputAt(10, "(hold:t2 &&+5 (at:t1 &&+5 (open(t1) &&+5 [opened]:t1))).");

        tester.mustBelieve(cycles, "(hold:t2 &&+5 (at:t1 &&+5 open(t1)))",
                1.0f, 0.45f,
                -5);
    }
    @Test
    public void subsent_simultaneous()  {
        TestNAR tester = test();

        //TODO decide correct parentheses ordering

        //tester.nar.log();
        tester.input("[opened]:t1. :|:");
        tester.inputAt(10, "(hold:t2 &&+0 (at:t1 &&+0 (open(t1) &&+0 [opened]:t1))).");

        //TODO Narsese parser for this:
//        tester.mustBelieve(cycles, "( &&+0 ,(t1-->at),(t2-->hold),(t1-->[opened]),open(t1))",
//                1.0f, 0.90f,
//                0);

        tester.mustBelieve(cycles, "(&&, hold:t2, at:t1, open(t1)).",
                1.0f, 0.42f,
                0);

//        tester.mustBelieve(cycles, "(hold:t2 &&+0 (at:t1 &&+0 open(t1))).",
//                1.0f, 0.45f,
//                0);
    }







    @Test
    public void conditional_abduction_test()  { //maybe to nal7 lets see how we will split these in the future
        TestNAR tester = test();

        tester.input("at:(SELF,{t003}). :|:");
        tester.inputAt(10, "(goto($1) ==>+5 at:(SELF,$1)).");

        tester.mustBelieve(cycles, "goto({t003})", 1.0f, 0.45f, -5);

    }

    @Test
    public void ded_with_var_temporal()  {
        TestNAR tester = test();

        tester.input("goto({t003}). :|:");
        tester.inputAt(10, "(goto($1) ==>+5 at:(SELF,$1)).");

        tester.mustBelieve(cycles, "at:(SELF,{t003})", 1.0f, 0.81f, 5);

    }

    @Test
    public void ded_with_var_temporal2()  {
        TestNAR tester = test();

        tester.input("goto({t003}). :|: ");
        tester.inputAt(10, "(goto($1) ==>+5 at:(SELF,$1)). ");

        tester.mustBelieve(cycles, "at:(SELF,{t003})", 1.0f, 0.81f,5);

    }



    @Test public void goal_deduction_tensed_conseq()  {
        TestNAR tester = test();

        tester.input("goto(x). :\\:");
        tester.inputAt(10, "(goto($1) ==>+5 at:(SELF,$1)).");

        tester.mustBelieve(cycles, "at:(SELF,x)", 1.0f, 0.81f, 0);
    }

}
