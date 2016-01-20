package nars.nal.nal8;

import nars.NAR;
import nars.nal.AbstractNALTester;
import nars.util.meter.TestNAR;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Supplier;

@RunWith(Parameterized.class)
public class NAL8TestWorking extends AbstractNALTester {

    final int cycles = 150; //150 worked for most of the initial NAL8 tests converted

    public NAL8TestWorking(Supplier<NAR> b) { super(b); }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable configurations() {
        return AbstractNALTester.nars(8, false);
    }





    @Test
    public void goal_deductionWithVariableElmination()  {
        TestNAR tester = test();

        tester.input("at:(SELF,{t001})!");
        tester.inputAt(10, "(goto($1) ==>+5 at:(SELF,$1)).");

        tester.mustDesire(cycles, "goto({t001})", 1.0f, 0.81f);

    }
    @Test
    public void goal_deduction()  {
        TestNAR tester = test();
        tester.input("x:y!");
        tester.input("(goto(z) ==>+5 x:y).");
        tester.mustDesire(cycles, "goto(z)", 1.0f, 0.81f);
    }
    @Test
    public void goal_deduction_alt()  {
        TestNAR tester = test();
        tester.input("x:y!");
        tester.input("(goto(x) ==>+5 x:y).");
        tester.mustDesire(cycles, "goto(x)", 1.0f, 0.81f);
    }
    @Test
    public void goal_deduction_delayed()  {
        TestNAR tester = test();

        tester.input("x:y!");
        tester.inputAt(10, "(goto(z) ==>+5 x:y).");
        tester.mustDesire(cycles, "goto(z)", 1.0f, 0.81f);
    }

    @Test public void goal_deduction_tensed_conseq()  {
        TestNAR tester = test();

        tester.input("goto(x). :\\:");
        tester.inputAt(10, "(goto($1) ==>+5 at:(SELF,$1)).");

        tester.mustBelieve(cycles, "at:(SELF,x)", 1.0f, 0.81f, 0);
    }
    @Test public void goal_deduction_tensed_conseq_noVar()  {
        TestNAR tester = test();

        tester.input("goto(x). :\\:");
        tester.inputAt(10, "(goto(x) ==>+5 at:(SELF,x)).");

        tester.mustBelieve(cycles, "at:(SELF,x)", 1.0f, 0.81f, 0);
    }

    @Test
    public void belief_deduction_by_condition()  {
        TestNAR tester = test();

        tester.input("(open({t001}) ==>+5 <{t001} --> [opened]>). :|: ");
        tester.inputAt(10, "open({t001}). :|:");

        tester.mustBelieve(cycles, "<{t001} --> [opened]>", 1.0f, 0.81f, 15);

    }
    @Test
    public void condition_goal_deduction2()  {
        TestNAR tester = test();

        tester.input("a:b!");
        tester.inputAt(10, "(( c:d &&+5 e:f ) ==>+0 a:b).");

        tester.mustDesire(cycles, "( c:d &&+5 e:f)", 1.0f, 0.81f);
    }

    @Test
    public void further_detachment()  {
        TestNAR tester = test();


        tester.input("<(SELF,{t002}) --> reachable>. :|:");
        tester.inputAt(10, "(<(SELF,{t002}) --> reachable> &&+5 pick({t002}))!");

        tester.mustDesire(cycles, "pick({t002})", 1.0f, 0.81f);

    }


    @Test public void desiredFeedbackReversedIntoGoalEternal()  {
        TestNAR tester = test();
        tester.input("<y --> (/,^exe,x,_)>!");
        tester.mustDesire(5, "exe(x, y)", 1.0f, 0.9f);
    }


    @Test public void desiredFeedbackReversedIntoGoalNow()  {
        TestNAR tester = test();
        tester.input("<y --> (/,^exe,x,_)>! :|:");
        tester.mustDesire(5, "exe(x, y)", 1.0f, 0.9f, 0);
    }

    @Test public void testExecutionResult()  {
        TestNAR tester = test();

        tester.input("<#y --> (/,^exe,x,_)>! :|:");
        tester.mustDesire(4, "exe(x, #1)", 1.0f, 0.9f, 0);

        //if (!(tester.nar instanceof SingleStepNAR)) {
        //tester.nar.log();
        //tester.mustBelieve(250, "exe(x, a)", 1.0f, 0.99f, 10);
        //        tester.mustBelieve(26, "<a --> (/, ^exe, x, _)>",
        //                exeFunc.getResultFrequency(),
        //                exeFunc.getResultConfidence(),
        //                exeFunc.getResultFrequency(),
        //                exeFunc.getResultConfidence(),
        //                6);
//            tester.nar.onEachFrame(n -> {
//                if (n.time() > 8)
//                    assertEquals(1, exeCount);
//            });
        //}

    }

    @Test
    public void detaching_single_premise()  {
        TestNAR tester = test();
        tester
                .input("(reachable:(SELF,{t002}) &&+5 pick({t002}))!")
                .mustDesire(cycles, "reachable:(SELF,{t002})", 1.0f, 0.81f)
                .mustDesire(cycles, "pick({t002})", 1.0f, 0.81f)
        ;
    }
    @Test
    public void detaching_single_premise_temporal()  {
        TestNAR tester = test();
        tester
                .input("(reachable:(SELF,{t002}) &&+5 pick({t002}))! :|:")
                .mustDesire(cycles, "reachable:(SELF,{t002})", 1.0f, 0.81f, 0)
                .mustDesire(cycles, "pick({t002})", 1.0f, 0.81f, 5)
        ;
    }

}
