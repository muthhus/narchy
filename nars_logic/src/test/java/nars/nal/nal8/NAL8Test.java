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

        tester.mustBelieve(cycles*10, "(hold:({t002}) &&+5 (at:({t001}) &&+5 open({t001})))",
                1.0f, 0.45f,
                -5);
    }


    @Test
    public void temporal_deduction_2()  {
        TestNAR tester = test();

        tester.input("<(&/, hold:(SELF,{t002}), at:(SELF,{t001}), open({t001})) =/> [opened]:{t001}>.");
        tester.inputAt(10, "hold:(SELF,{t002}). :|: ");

        //mustBelieve?
        tester.mustBelieve(cycles, "<(&/, at:(SELF,{t001}), open({t001})) =/> [opened]:{t001}>", 1.0f, 0.81f, 10);

    }







    @Test
    public void conditional_abduction_test()  { //maybe to nal7 lets see how we will split these in the future
        TestNAR tester = test();

        tester.input("<(SELF,{t003}) --> at>. :|:");
        tester.inputAt(10, "(goto($1) ==>+5 <(SELF,$1) --> at>).");

        tester.mustBelieve(cycles, "goto({t003})", 1.0f, 0.45f, -5);

    }

    @Test
    public void ded_with_var_temporal()  {
        TestNAR tester = test();

        tester.input("goto({t003}). :|:");
        tester.inputAt(10, "(goto($1) ==>+5 <(SELF,$1) --> at>).");

        tester.mustBelieve(cycles, "<(SELF, {t003}) --> at>", 1.0f, 0.81f, 5);

    }

    @Test
    public void ded_with_var_temporal2()  {
        TestNAR tester = test();

        tester.input("goto({t003}). :|: ");
        tester.inputAt(10, "(goto($1) ==>+5 <(SELF,$1) --> at>). ");

        tester.mustBelieve(cycles, "<(SELF, {t003}) --> at>", 1.0f, 0.81f,5);

    }




}
