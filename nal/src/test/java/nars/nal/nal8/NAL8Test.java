package nars.nal.nal8;

import nars.$;
import nars.NAR;
import nars.nal.AbstractNALTest;
import nars.term.Term;
import nars.test.TestNAR;
import nars.time.Tense;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Supplier;

import static nars.time.Tense.ETERNAL;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class NAL8Test extends AbstractNALTest {

    final int cycles = 150;

    public NAL8Test(Supplier<NAR> b) { super(b); }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable configurations() {
        return AbstractNALTest.nars(8);
    }


    @Test
    public void subsent_1()  {
        TestNAR tester = test();

        //TODO decide correct parentheses ordering

        //tester.nar.log();
        tester.input("[opened]:{t001}. :|:");
        tester.inputAt(10,
                "(((hold:({t002}) &&+5 at:({t001})) &&+5 open({t001})) &&+5 [opened]:{t001}).");

        // hold .. at .. open
        tester.mustBelieve(cycles, "((hold:({t002}) &&+5 at:({t001})) &&+5 open({t001}))",
                1.0f, 0.81f,
                -5);


//        //the structurually inverted sequence
//        tester.mustBelieve(cycles,
//                "(hold:({t002}) &&+5 (at:({t001}) &&+5 (open({t001}) &&+5 [opened]:{t001})))",
//                1.0f, 0.90f
//                );


        //tester.inputAt(10, "(hold:({t002}) &&+5 (at:({t001}) &&+5 (open({t001}) &&+5 [opened]:{t001}))).");
////        tester.mustBelieve(cycles, "(hold:({t002}) &&+5 (at:({t001}) &&+5 open({t001})))",
////                1.0f, 0.45f,
////                -5);

//        tester.mustBelieve(cycles, "(hold:({t002}) &&+5 (at:({t001}) &&+5 open({t001})))",
//                1.0f, 0.45f,
//                -5);

    }




    @Test public void subsent_1_even_simpler_simplerBeliefTemporal()  {
        test()
                
                .input("(open(t1) &&+5 [opened]:t1). :|:")
                .mustBelieve(cycles, "open(t1)", 1.0f, 0.81f, 0)
                .mustBelieve(cycles, "[opened]:t1", 1.0f, 0.81f, 5)
                .mustNotOutput(cycles, "open(t1)", '.', ETERNAL)
                .mustNotOutput(cycles, "[opened]:t1", '.', ETERNAL)
        ;
    }
    @Test public void subsent_1_even_simpler_simplerGoalEternal()  {
        test()
                
                .input("(open(t1) && [opened]:t1)!")
                .mustDesire(cycles, "open(t1)", 1.0f, 0.81f, ETERNAL)
                .mustNotOutput(cycles, "open(t1)", '!', 0)
        ;

    }
    @Test public void subsent_1_even_simpler_simplerGoalTemporal()  {
        test()
                
                .input("(open(t1) &&+5 [opened]:t1)! :|:")
                .mustDesire(cycles, "open(t1)", 1.0f, 0.81f, 0) //only temporal
                .mustNotOutput(cycles, "open(t1)", '!',  ETERNAL) //no eternal
        ;
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

        tester.input("goto(x). :|:");
        tester.inputAt(10, "(goto($1) ==>+5 at:(SELF,$1)).");

        tester.mustBelieve(cycles, "at:(SELF,x)", 1.0f, 0.81f, 5);
    }

    @Test
    public void condition_goal_deductionWithVariableElimination()  {

        test()
                
                .input("at:(SELF,{t003}). :|:")
                //.input("goto({t003}). :|:")
                .inputAt(10, "(goto(#1) &&+5 at:(SELF,#1))!")

                .mustDesire(cycles, "goto({t003})", 1.0f, 0.81f, -5)
                //.mustNotOutput(cycles, "goto({t003})", '!', -5) //??
                ;
    }

    @Test
    public void condition_goal_deductionWithVariableEliminationOpposite()  {

        test()
                .log()
                .input("goto({t003}). :|:")
                .inputAt(10, "(goto(#1) &&+5 at:(SELF,#1))!")

                .mustDesire(2 * cycles, "at:(SELF,{t003})", 1.0f, 0.81f, 5)
        ;
    }

    @Test
    public void goal_deduction()  {
        TestNAR tester = test();
        tester.input("x:y! :|:");
        tester.input("(goto(z) ==>+5 x:y).");
        tester.mustDesire(cycles, "goto(z)", 1.0f, 0.81f, -5);
    }

    @Test
    public void goal_deduction_delayed()  {
        TestNAR tester = test();

        tester.input("x:y!");
        tester.inputAt(10, "(goto(z) ==>+5 x:y). :|:");
        tester.mustDesire(cycles, "goto(z)", 1.0f, 0.81f, 10);
    }

    @Test public void goal_deduction_tensed_conseq_noVar()  {
        TestNAR tester = test();

        tester.inputAt(1, "goto(x). :|:");
        tester.inputAt(10, "(goto(x) ==>+5 at:(SELF,x)).");

        tester.mustBelieve(cycles, "at:(SELF,x)", 1.0f, 0.81f, 6);
    }

    @Test
    public void belief_deduction_by_condition()  {
        TestNAR tester = test();

        tester.input("(open({t001}) ==>+5 [opened]:{t001}).");
        tester.inputAt(10, "open({t001}). :|:");

        tester.mustBelieve(cycles, "[opened]:{t001}", 1.0f, 0.81f, 15);

    }

    @Test
    public void condition_goal_deduction2()  {
        test()
            
            .input("a:b! :|:")
            .inputAt(10, "(( c:d &&+5 e:f ) ==>+0 a:b).")
            .mustDesire(cycles, "( c:d &&+5 e:f)", 1.0f, 0.81f, 0)
            .mustNotOutput(cycles, "( c:d &&+5 e:f)", '!', ETERNAL)
        ;
    }
    @Test
    public void condition_goal_deductionEternal()  {
        test()
                
                .input("a:b!")
                .inputAt(10, "(( c:d &&+5 e:f ) ==> a:b).")
                .mustDesire(cycles, "( c:d &&+5 e:f)", 1.0f, 0.81f, ETERNAL)
                .mustNotOutput(cycles, "( c:d &&+5 e:f)", '!', 0)
        ;
    }

    @Test
    public void further_detachment()  {
        test()
            .log()
            .input("reachable:(SELF,{t002}). :|:")
            .inputAt(10, "(reachable:(SELF,{t002}) &&+5 pick({t002}))!")
            .mustDesire(cycles, "pick({t002})", 1.0f, 0.81f, 5);

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


    @Test
    public void condition_goal_deduction()  {
        test()
            
            .input("<(SELF,{t002}) --> reachable>! :|:")
            .inputAt(10, "((<($1,#2) --> on> &&+0 <(SELF,#2) --> at>) ==>+0 <(SELF,$1) --> reachable>).")
            .mustDesire(cycles, "(<(SELF,#1) --> at> &&+0 <({t002},#1) --> on>)", 1.0f, 0.81f, 0)
            .mustNotOutput(cycles, "(<(SELF,#1) --> at> &&+0 <({t002},#1) --> on>)", '!', ETERNAL, 10);

    }

    @Test public void testExecutionResultConstant() {
        test()
            
            .input("<z --> (/,^exe,x,_)>! :|:")
            .mustDesire(64, "exe(x, z)", 1.0f, 0.9f, 0);
    }

    @Test public void testExecutionResult()  {
        test()
                
            .input("<#1 --> (/,^exe,x,_)>! :|:")
            .mustDesire(64, "exe(x, #1)", 1.0f, 0.9f, 0);

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



//    @Test
//    public void detaching_condition_2present()  {
//        detachingCondition(true);
//    }
//    @Test
//    public void detaching_condition_2eternal()  {
//        detachingCondition(false);
//    }
//
//    public void detachingCondition(boolean presentOrEternal) {
//        String suffix = "(open({t001}) ==>+5 [opened]:{t001})";
//        test()
//                
//            .input("at:(SELF,{t001}). :|: ")
//            .inputAt(10, "(at:(SELF,{t001}) &&+5 " + suffix + "). " + (presentOrEternal ? ":|:" : "")) //the occurrence time of this event is ignored; what matter is the task
//            .mustBelieve(cycles, suffix, 1.0f, 0.81f, 5)
//            .mustNotOutput(cycles,suffix,'.',ETERNAL)
//            .mustNotOutput(cycles,suffix,'.',0)
//        ;
//    }


    @Test
    public void goal_ded_2()  {
        TestNAR tester = test();

        tester.input("at:(SELF,{t001}). :|:");
        tester.inputAt(10, "(at:(SELF,{t001}) &&+5 open({t001}))!");

        tester.mustDesire(cycles, "open({t001})", 1.0f, 0.81f, 5);

    }

    @Test
    public void condition_goal_deduction_3simplerReverse()  {
        test()
//                .log()
                .inputAt(1, "at:t003! :|:")
                .inputAt(1, "(at:$1 ==>+5 goto:$1).")

                .mustDesire(cycles*2, "goto:t003", 1.0f, 0.45f, 6);

    }






    @Test
    public void temporal_deduction_1()  {
        TestNAR tester = test();

        //tester.nar.log();
//        tester.input("pick({t002}). :\\:");
//        tester.inputAt(10, "(pick({t002}) ==>+5 hold:({t002})). :\\:");
//
//        tester.mustBelieve(cycles, "hold:({t002})", 1.0f, 0.81f, 0);

        tester.input("pick:t2. :|:");
        tester.inputAt(10, "(pick:t2 ==>+5 hold:t2).");
        tester.mustBelieve(cycles, "hold:t2", 1.0f, 0.81f, 5);

    }



    @Test
    public void subgoal_2()  {
        test()
                .input("(hold:(SELF,{t002}) &&+5 (at:(SELF,{t001}) && open({t001})))! :|:")
                .mustDesire(cycles, "hold:(SELF,{t002})", 1.0f, 0.81f, 0)
                .mustNotOutput(cycles, "hold:(SELF,{t002})", '!', ETERNAL);
    }

    @Test
    public void subgoal_2_inner_dt()  {
        test()
            .input("(hold:(SELF,{t002}) &&+5 (at:(SELF,{t001}) &&+5 open({t001})))! :|:")
            .mustDesire(cycles, "hold:(SELF,{t002})", 1.0f, 0.81f, 0)
            .mustNotOutput(cycles, "hold:(SELF,{t002})", '!', ETERNAL);
    }

    @Test
    public void subbelief_2()  {
        Term t = $.$("(hold:(SELF,{t002}) &&+5 (at:(SELF,{t001}) &&+5 open({t001})))");
        assertEquals(2, t.size());

        test()
                
                .input("(hold:(SELF,{t002}) &&+5 (at:(SELF,{t001}) &&+5 open({t001}))). :|:")
                .mustBelieve(cycles*4, "hold:(SELF,{t002})", 1.0f, 0.81f, 0)
                .mustBelieve(cycles*4, "(at:(SELF,{t001}) &&+5 open({t001}))", 1.0f, 0.81f, 5)
        ;
    }
    @Test
    public void subbelief_2easy()  {
        //requires StructuralDeduction to AllowOverlap
        test()
                
                .input("(a:b &&+5 x:y). :|:")
                .mustBelieve(cycles, "a:b", 1.0f, 0.81f, 0)
                .mustBelieve(cycles, "x:y", 1.0f, 0.81f, 5)
        ;
    }
    @Test
    public void subbelief_2medium()  {
        //requires StructuralDeduction to AllowOverlap
        test()
                .input("(a:b &&+5 (c:d &&+5 x:y)). :|:")
                .mustBelieve(cycles, "a:b", 1.0f, 0.81f, 0)
                .mustBelieve(cycles, "c:d", 1.0f, 0.73f, 5)
                .mustBelieve(cycles, "x:y", 1.0f, 0.73f, 10)
        ;
    }

    @Test
    public void further_detachment_2()  {
        test()
            .input("reachable:(SELF,{t002}). :|:")
            .inputAt(3, "((reachable:(SELF,{t002}) &&+5 pick({t002})) ==>+7 hold:(SELF,{t002})).")
            .mustBelieve(cycles, "(pick({t002}) ==>+7 hold:(SELF, {t002}))", 1.0f, 0.81f, 5)
            .mustNotOutput(cycles, "(pick({t002}) ==>+7 hold:(SELF, {t002}))", '.', 0)
        ;

    }


    @Test
    public void goal_deduction_2()  {
        TestNAR tester = test();

        tester.input("goto({t001}). :|: ");
        tester.inputAt(7, "(goto($1) ==>+2 at:(SELF,$1)). ");

        tester.mustBelieve(cycles, "at:(SELF,{t001})", 1.0f, 0.81f, 2);

    }

    @Test
    public void condition_goal_deduction_2()  {
        test()
            
            .input("<({t002},{t003}) --> on>. :|:")
            .inputAt(10, "(<({t002},#1) --> on> &&+0 <(SELF,#1) --> at>)!")
            .mustDesire(cycles, "<(SELF,{t003}) --> at>", 1.0f, 0.81f, 0);

        //tester.mustNotOutput(time, selfAtT3, '!', 0, 1f, 0, 1f, ETERNAL);
    }
    @Test
    public void condition_belief_deduction_2()  {

        test()
            
            .input(              "on:({t002},{t003}). :|:")
            .inputAt(10,         "(on:({t002},#1) &&+0 at:(SELF,#1)).")
            .mustBelieve(cycles,   "at:(SELF,{t003})", 1.0f, 0.43f, 0)
            .mustNotOutput(cycles, "at:(SELF,{t003})", '.', 0, 1f, 0, 1f, ETERNAL);

    }

    @Test
    public void condition_belief_deduction_2_easier()  {

        test()
                .input(      "on:(t002,t003). :|:")
                .inputAt(2,  "(on:(t002,#1) &&+0 at:(SELF,#1)).")
                .mustBelieve(cycles,   "at:(SELF,t003)", 1.0f, 0.43f, 0)
                .mustNotOutput(cycles, "at:(SELF,t003)", '.', 0, 1f, 0, 1f, ETERNAL);
    }

    @Ignore
    @Test
    public void condition_belief_deduction_2_dternal()  {


        test()
                
                .input(              "on:(t002,t003). :|:")
                .inputAt(10,         "(on:(t002,#1) && at:(SELF,#1)).") //<-- DTERNAL
                .mustBelieve(cycles*4,   "at:(SELF,t003)", 1.0f, 0.43f, 0)
                //TODO mustNotBelieve? ^ the DTERNAL conjunctoin maybe should not be decomposed
                ;
    }

    @Test
    public void temporal_goal_detachment_1()  {
        test()
                
                .input("(hold). :|:")
                .inputAt(2, "( (hold) &&+5 ((at) &&+5 (open)) )!")
                .mustDesire(cycles, "((at) &&+5 (open))", 1.0f, 0.81f, 5)
                .mustNotOutput(cycles, "((at) &&+5 (open))", '!', ETERNAL, 7)
        ;
    }

    @Test
    public void temporal_goal_detachment_2_invalid()  {
        //this is the reverse case which should not be derived by decomposing the belief
        test()
                .input("(hold)! :|:")
                .inputAt(2, "( (hold) &&+5 (eat) ).") //should not decomposed by the goal task
                .mustNotOutput(cycles, "(eat)", '!', ETERNAL, 15, 5)
        ;
    }
    @Test
    public void temporal_goal_detachment_3_valid()  {
        test()
                .input("(use)! :|:")
                .inputAt(2, "( (hold) &&+5 (use) ).") //should be decomposed by the goal task
                .mustDesire(cycles, "(hold)", 1f, 0.81f, -5)
                .mustNotOutput(cycles, "(use)", '!', ETERNAL) //not eternal, we have a temporal basis here
                .mustNotOutput(cycles, "(hold)", '!', ETERNAL)
        ;
    }

    @Test
    public void temporal_goal_detachment_3_valid_negate()  {
        test()
                .input("(--,(use))! :|:")
                .inputAt(2, "( (hold) &&+5 (--,(use)) ).") //should be decomposed by the goal task
                .mustDesire(cycles, "(hold)", 1f, 0.81f, -5)
                .mustNotOutput(cycles, "(use)", '!', ETERNAL) //not eternal, we have a temporal basis here
        ;
    }

    @Test
    public void detaching_condition0()  {
        TestNAR tester = test();

        tester.input("( ( hold:t2 &&+5 (at:t1 &&+5 open:t1)) ==>+5 opened:t1).");
        tester.inputAt(10, "hold:t2. :|:");

        tester.mustBelieve(cycles, "((at:t1 &&+5 open:t1) ==>+5 opened:t1)", 1.0f, 0.81f, 15);

    }
    @Test
    public void detaching_condition()  {
        TestNAR tester = test();

        tester.input("( ( hold:(SELF,{t002}) &&+5 (at:(SELF,{t001}) &&+5 open({t001}))) ==>+5 [opened]:{t001}).");
        tester.inputAt(10, "hold:(SELF,{t002}). :|:");

        tester.mustBelieve(cycles, "((at:(SELF,{t001}) &&+5 open({t001})) ==>+5 [opened]:{t001})", 1.0f, 0.81f, 15);

    }

    @Test
    public void subgoal_1_abd()  {
        TestNAR tester = test();

        tester.input("[opened]:{t001}. :|:");
        tester.inputAt(10, "((hold:(SELF,{t002}) &&+5 ( at:(SELF,{t001}) &&+5 open({t001}))) ==>+5 [opened]:{t001}).");

        tester.mustBelieve(cycles, "( hold:(SELF,{t002}) &&+5 ( at:(SELF,{t001}) &&+5 open({t001})))",
                1.0f, 0.45f,
                -5);

    }

    @Test
    public void temporal_deduction_2()  {
        TestNAR tester = test();

        tester.input("((hold:(SELF,{t002}) &&+5 (at:(SELF,{t001}) &&+5 open({t001}))) ==>+5 [opened]:{t001}).");
        tester.inputAt(10, "hold:(SELF,{t002}). :|: ");

        tester.mustBelieve(cycles, "((at:(SELF,{t001}) &&+5 open({t001})) ==>+5 [opened]:{t001})", 1.0f, 0.81f, 15);

    }


    @Test
    public void goalInferredFromSimilarity()  {
        TestNAR tester = test();

        tester.input("(a:b<->c:d).");
        tester.input("c:d!");
        tester.mustDesire(cycles, "a:b", 1.0f, 0.81f);
    }
    @Test
    public void goalInferredFromSimilarityEternalAndPresent()  {
        TestNAR tester = test();

        tester.input("(a:b<->c:d)."); //ETERNAL
        tester.input("c:d! :|:"); //PRESENT
        tester.mustDesire(cycles, "a:b", 1.0f, 0.81f, 0);
        tester.mustNotOutput(cycles*3, "a:b", '!', ETERNAL);

    }
    @Test
    public void goalInferredFromSimilarityAndImplEternalAndPresent()  {
        TestNAR tester = test();

        tester.input("(a:b<->c:d)."); //ETERNAL
        tester.input("(c:d &&+0 e:f). :|:"); //PRESENT
        tester.input("e:f! :|:"); //PRESENT
        tester.mustDesire(cycles, "a:b", 1.0f, 0.73f, 0);
        tester.mustNotOutput(cycles*3, "a:b", '!', ETERNAL);

    }

    @Test
    public void conjunctionSubstitutionViaSimilarity()  {
        TestNAR tester = test();

        tester.input("(a:b<->c:d)."); //ETERNAL
        tester.input("(c:d &&+0 e:f). :|:"); //PRESENT
        tester.mustBelieve(cycles, "(a:b &&+0 e:f)", 1.0f, 0.81f, 0);
        tester.mustNotOutput(cycles*3, "(a:b &&+0 e:f)", '.', ETERNAL);

    }
    @Test
    public void implSubstitutionViaSimilarity()  {
        test()
            .input("(a:b<->c:d).") //ETERNAL
            .input("(c:d ==>+1 e:f). :|:") //PRESENT
            .mustBelieve(cycles, "(a:b ==>+1 e:f)", 1.0f, 0.81f, 0)
            .mustNotOutput(cycles, "(a:b ==>+1 e:f)", '.', ETERNAL);
    }
    @Test
    public void implSubstitutionViaSimilarityReverse()  {
        test()
                .input("(a:b<->c:d).") //ETERNAL
                .input("(e:f ==>+1 c:d). :|:") //PRESENT
                .mustBelieve(cycles, "(e:f ==>+1 a:b)", 1.0f, 0.81f, 0)
                .mustNotOutput(cycles, "(e:f ==>+1 a:b)", '.', ETERNAL);
    }
    @Test
    public void equiSubstitutionViaSimilarity()  {
        test()
                .input("(a:b<->c:d).") //ETERNAL
                .input("(e:f <=>+1 c:d). :|:") //PRESENT
                .mustBelieve(cycles, "(e:f <=>+1 a:b)", 1.0f, 0.81f, 0)
                .mustNotOutput(cycles, "(e:f <=>+1 a:b)", '.', ETERNAL);
    }

    @Test public void testDeiredImpl() {
        /*
        $0.61;0.39;0.50$ (y)! :172: %1.00;0.45%
              PARENT   $0.75;0.80;0.95$ <(x) ==> (y)>! :3: %1.00;0.90%
              BELIEF   $0.50;0.80;0.95$ (x). :4: %1.00;0.90%
         */

        TestNAR t = test();
        t
            
            .believe("(x)")
            .goal("((x)&&(y))")
            .mustDesire(cycles, "(y)", 1f, 0.81f);
    }

    @Test public void testBelievedImplOfDesire() {

        TestNAR t = test();
        t
                
                .goal("(x)")
                .believe("((x)==>(y))")
                .mustDesire(cycles, "(y)", 1f, 0.45f);
    }

    @Test public void testDeiredImplDelayed() {


        test()
                
                .believe("(x)", Tense.Present, 1f, 0.9f)
                .goal("((x)&&+3(y))")
                .mustDesire(cycles, "(y)", 1f, 0.81f, 3)
                .mustNotOutput(cycles*3, "(y)", '!', ETERNAL);
    }
    @Test public void testBelievedImplOfDesireDelayed() {

        test()
                //t.log()
                .goal("(x)", Tense.Present, 1f, 0.9f)
                .believe("((x)==>+3(y))")
                .mustDesire(cycles, "(y)", 1f, 0.45f, 3)
                .mustNotOutput(cycles*3, "(y)", '!', ETERNAL);
    }

    @Test public void testGoalConjunctionDecompose() {
        test()
                
                .goal("((x) &&+3 (y))", Tense.Present, 1f, 0.9f)
                .mustDesire(cycles, "(x)", 1f, 0.81f, 0)
                //.mustNotOutput(cycles, "(y)", '!', 3)
                .mustNotOutput(cycles, "(y)", '!', ETERNAL);
    }

    @Test public void testGoalConjunctionPostDecompose() {
        //after a belief has been fedback, continue decomposing the conjunction goal to expose the (y) desire:

        test()
                
                .goal("((x) &&+3 (y))", Tense.Present, 1f, 0.9f)
                .believe("(x)", Tense.Present, 1f, 0.9f)
                .mustDesire(cycles, "(y)", 1f, 0.81f, 3)
                .mustNotOutput(cycles, "(y)", '!', ETERNAL);
    }



        //        @Test
        //        public void subgoal_2_small()  {
        //            TestNAR tester = test();
        //
        //            tester.input("(hold:(SELF,y) &&+5 at:(SELF,x))!");
        //
        //            tester.mustDesire(cycles, "hold:(SELF,y)", 1.0f, 0.81f);
        //
        //        }


        //        @Test public void subsent_1_even_simpler_simplerGoalTemporal()  {
        //        test()
        //                .log()
        //                .input("(open(t1) &&+5 [opened]:t1)! :|:")
        //                .mustDesire(cycles, "open(t1)", 1.0f, 0.81f, 0) //temporal
        //                .mustNotDesire(cycles, "[opened]:t1", 1.0f, 0.81f, 5) //temporal
        //        ;
        //    }

        //        @Test
        //        public void detaching_single_premise2()  {
        //            TestNAR tester = test();
        //
        //            tester.input("(at:(SELF,{t001}) &&+5 open({t001}) )!");
        //            tester.mustDesire(cycles, "at:(SELF,{t001})", 1.0f, 0.81f);
        //            tester.mustNotDesire(cycles, "open({t001})", 1.0f, 0.81f);
        //
        //        }

        //        @Test
        //        public void detaching_single_premise_temporal()  {
        //            TestNAR tester = test();
        //            tester
        //                    
        //                    .input("(reachable:(SELF,{t002}) &&+5 pick({t002}))! :|:")
        //                    .mustDesire(6, "reachable:(SELF,{t002})", 1.0f, 0.81f, 0)
        //                    .mustNotDesire(6, "pick({t002})", 1.0f, 0.81f, 5)
        //            ;
        //        }

        //        @Test
        //        public void detaching_single_premise()  {
        //            TestNAR tester = test();
        //            tester
        //                    
        //                    .input("(reachable:(SELF,{t002}) &&+5 pick({t002}))!")
        //                    .mustDesire(cycles, "reachable:(SELF,{t002})", 1.0f, 0.81f)
        //                    .mustDesire(cycles, "pick({t002})", 1.0f, 0.81f);
        //        }
    //}


    @Test public void testGoalConjunctionDecomposeSuffix() {
        test()
                .goal("((x) &&+3 (y))", Tense.Eternal, 1f, 0.9f)
                .inputAt(4, "(x). :|:")
                .mustDesire(cycles, "(y)", 1f, 0.81f, (4+3))
                .mustNotOutput(cycles, "(y)", '!', 3)
                .mustNotOutput(cycles, "(y)", '!', ETERNAL);
    }


    @Test
    public void testInhibition()  {
        //by deduction
        test()
            .goal("(reward)")
            .believe("((good) ==> (reward))", 1, 0.9f)
            .believe("((--,(bad)) ==> (reward))", 1, 0.9f)
            .mustDesire(cycles, "(bad)", 0.0f, 0.81f);

    }

    @Test
    public void testInhibition0()  {
        test()
                
                .goal("(reward)")
                .believe("((bad) ==> (--,(reward)))", 1, 0.9f)
                .mustDesire(cycles, "(bad)", 0.0f, 0.81f)
                .mustNotOutput(cycles, "(bad)", '!', 0.5f, 1f, 0f, 1f, ETERNAL);
    }

    @Test
    public void testInhibition1()  {
        test()
                
                .goal("(reward)")
                .believe("((good) ==> (reward))", 1, 0.9f)
                .believe("((bad) ==> (--,(reward)))", 1, 0.9f)
                .mustDesire(cycles, "(good)", 1.0f, 0.81f)
                .mustDesire(cycles, "(bad)", 0.0f, 0.81f);
    }


    @Test public void testNegatedImplicationTerm1() {

        test()
                .log()
                .goal("(R)")
                .input("((--,a:b) ==>+0 (R)). :|:")
                .mustDesire(cycles, "a:b", 0.0f, 0.81f, 0);
    }

    @Test public void testNegatedImplicationTerm2() {

        test()
                .input("(R)! :|:")
                .input("((--,a:b) ==>+0 (R)).")
                .mustDesire(cycles, "a:b", 0.0f, 0.81f, 0);

    }
    @Test public void testNegatedImplicationTerm3() {
        test()
                .input("(R). :|:")
                .input("((--,a:b) &&+0 (R))!")
                .mustDesire(cycles, "a:b", 0.0f, 0.81f, 0);
    }


    @Test public void testGoalSimilaritySpreading() {
        test()
                
                .input("(R)!")
                .input("((G) <-> (R)).")
                .mustDesire(cycles, "(G)", 1.0f, 0.81f);
    }
    @Test public void testNegatedGoalSimilaritySpreading() {
        test()
                
                .input("(--,(R))!")
                .input("((G) <-> (R)).")
                .mustDesire(cycles, "(G)", 0.0f, 0.81f);
    }

//    @Test public void testInheritanceCompositionTemporal() {
//        /*
//        WRONG OCCURRENCE TIME:
//        $.38;.39;.78$ (((in)|(left))-->^cam)! 474-424 %.12;.99% {474-424: 67;7k;ab;kv;lx} PremiseRule{	 prePreconditions=[TermNotEquals(1:(0),0:(0)), task:".!"]	 match=MatchTaskBelief[((%1-->%2),(%3-->%2))]	 postconditions=[PostCondition{term=((%1|%3)-->%2), beliefTruth=Intersection, goalTruth=Intersection, puncOverride= }]	 temporalize=nars.nal.TimeFunctions$$Lambda$122/684230144@3fbfa96	 eternalize=false	 anticipate=false	 minNAL=1	 source='<(P --> M), (S --> M), notSet(S), notSet(P), neq(S,P), no_common_subterm(S,P) |- ((S | P) --> M), (Belief:Intersection, Desire:Intersection, Derive:NoSwap)>'}
//              $1.0;.50;.51$ cam(left)! 464-414 %.78;1.0% {464-414: 67;7k;ab} Revection Merge
//              $.96;.46;.43$ (((in)|(left))-->^cam). 442-399 %.16;.99% {442-399: kv;lx} Revection Merge
//        */
//
//        //uses AUTO TimeFunction
//
//        test()
//                
//                .inputAt(0, "cam(left)! :|:")
//                .inputAt(4, "(((in)|(left))-->^cam). :|:")
//
//                //must interpolate
//                .mustDesire(cycles, "(((in)|(left))-->^cam)", 1f,0.73f, 4)
//                .mustNotOutput(cycles, "(((in)|(left))-->^cam)", '!', 0, ETERNAL);
//    }

    @Ignore @Test public void testInheritanceDecompositionTemporalGoal() {
        //(((in)|(left))-->^cam)!
        //   cam(in)!
        //   cam(out)!

        test()
                
                .inputAt(0, "(((in)|(left))-->^cam)! :|:")
                .mustDesire(cycles, "cam(in)", 1f,0.81f, 0)
                .mustDesire(cycles, "cam(left)", 1f,0.81f, 0);

    }
    @Test public void testInheritanceDecompositionTemporalBelief() {
        //(((in)|(left))-->^cam)!
        //   cam(in)!
        //   cam(out)!

        test()
                .inputAt(0, "(((in)|(left))-->^cam). :|:")
                .mustBelieve(cycles, "cam(in)", 1f,0.81f, 0)
                .mustBelieve(cycles, "cam(left)", 1f,0.81f, 0);

    }

//    @Test public void testImplBackward1() {
//        test()
//            .log()
//                //.inputAt(2, "(?x ==>+2 (b))? :|:")
//                //.inputAt(2, "(?x &&+2 (b))? :|:")
//                .inputAt(2, "(a). :|:")
//                .inputAt(4, "(b). :|:")
//                .mustBelieve(cycles, "(UNWRITTEN)", 1f,0.81f, 0);
//
//    }
//    @Test public void testMixedTemporalInductionGoalConj() {
//        test()
//                .log()
//                .inputAt(0, "(x). :|:")
//                .inputAt(1, "(y)! :|:")
//                .mustDesire(cycles, "((x) &&+1 (y))", 1f,0.81f, 0)
//        ;
//    }

}
