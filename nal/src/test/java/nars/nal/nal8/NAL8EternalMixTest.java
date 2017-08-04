package nars.nal.nal8;

import nars.nal.AbstractNALTest;
import nars.test.TestNAR;
import org.junit.Ignore;
import org.junit.Test;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static nars.time.Tense.ETERNAL;

/**
 * NAL8 tests specifically involving one or more eternal input tasks
 */
public class NAL8EternalMixTest extends AbstractNALTest {

    final int cycles = 30;


    @Test
    public void subsent_1_even_simpler_simplerGoalEternal() {

        test

                .input("(open(t1) && [opened]:t1)!")
                .mustDesire(cycles, "open(t1)", 1.0f, 0.81f, ETERNAL)
                .mustNotOutput(cycles, "open(t1)", GOAL, 0)
        ;

    }

    @Test
    public void subsent_1() {

        TestNAR tester = test;


        //TODO decide correct parentheses ordering

        tester.input("[opened]:{t001}. :|:");
        tester.input("(((hold({t002}) &&+5 at({t001})) &&+5 open({t001})) &&+5 [opened]:{t001}).");

        // hold .. at .. open
        tester.mustBelieve(cycles, "((hold({t002}) &&+5 at({t001})) &&+5 open({t001}))",
                1.0f, 0.81f,
                -15, -5);


//        //the structurually inverted sequence
//        tester.mustBelieve(cycles,
//                "(hold({t002}) &&+5 (at({t001}) &&+5 (open({t001}) &&+5 [opened]:{t001})))",
//                1.0f, 0.90f
//                );


        //tester.inputAt(10, "(hold({t002}) &&+5 (at({t001}) &&+5 (open({t001}) &&+5 [opened]:{t001}))).");
////        tester.mustBelieve(cycles, "(hold({t002}) &&+5 (at({t001}) &&+5 open({t001})))",
////                1.0f, 0.45f,
////                -5);

//        tester.mustBelieve(cycles, "(hold({t002}) &&+5 (at({t001}) &&+5 open({t001})))",
//                1.0f, 0.45f,
//                -5);

    }

    @Test
    public void conditional_abduction_temporal_vs_eternal() { //maybe to nal7 lets see how we will split these in the future

        TestNAR tester = test;

        tester.input("at(SELF,{t003}). :|:");
        tester.inputAt(10, "(goto($1) ==>+5 at(SELF,$1)).");

        tester.mustBelieve(cycles, "goto({t003})", 1.0f, 0.45f, -5);

    }

    @Test
    public void ded_with_indep_var_temporal() {

        TestNAR tester = test;

        tester.input("goto({t003}). :|:");
        tester.inputAt(10, "(goto($1) ==>+5 at(SELF,$1)).");

        tester.mustBelieve(cycles, "at(SELF,{t003})", 1.0f, 0.81f, 5);

    }

    @Test
    public void ded_with_var_temporal() {

        test

                .input("goto({t003}). :|:")
                .inputAt(10, "(goto(#1) ==>+5 at(SELF,#1)).")
                .mustBelieve(cycles, "at(SELF,{t003})", 1.0f, 0.81f, 5);
    }

    @Test
    public void ded_with_var_temporal2() {

        TestNAR tester = test;

        tester.input("goto({t003}). :|: ");
        tester.inputAt(10, "(goto($1) ==>+5 at(SELF,$1)). ");

        tester.mustBelieve(cycles, "at(SELF,{t003})", 1.0f, 0.81f, 5);

    }


    @Test
    public void goal_deduction_tensed_conseq() {

        TestNAR tester = test;

        tester.input("goto(x). :|:");
        tester.inputAt(10, "(goto($1) ==>+5 at(SELF,$1)).");

        tester.mustBelieve(cycles, "at(SELF,x)", 1.0f, 0.81f, 5);
    }


    @Test
    public void condition_goal_deductionWithVariableEliminationOpposite() {

        test
                //.log()
                .input("goto({t003}). :|:")
                .input("(goto(#1) &&+5 at(SELF,#1))!")
                .mustDesire(2 * cycles, "at(SELF,{t003})", 1.0f, 0.81f, 5)
        ;
    }

    @Test
    public void goal_deduction_impl() {

        TestNAR tester = test;
        tester.input("x:y! :|:");
        tester.input("(goto(z) ==>+5 x:y).");
        tester.mustDesire(cycles, "goto(z)", 1.0f, 0.81f, 0);
    }

    @Test
    public void goal_deduction_impl_after() {

        TestNAR tester = test;
        tester.input("x:y! :|:");
        tester.input("(goto(z) ==>-5 x:y).");
        tester.mustDesire(cycles, "goto(z)", 1.0f, 0.81f, 0);
    }

    @Test
    public void goal_deduction_delayed_impl() {

        TestNAR tester = test;

        tester.input("x:y!");
        tester.inputAt(10, "(goto(z) ==>+5 x:y). :|:");
        tester.mustDesire(cycles, "goto(z)", 1.0f, 0.81f, 10);
    }


    @Test
    public void goal_deduction_equi_pos_pospos() {

        TestNAR tester = test;
        tester.input("x:y! :|:");
        tester.input("(goto(z) <=>+5 x:y).");
        tester.mustDesire(cycles, "goto(z)", 1.0f, 0.81f, 0);
        tester.mustNotOutput(cycles, "goto(z)", GOAL, ETERNAL);
    }


    @Test
    public void goal_deduction_equi_neg_pospos() {

        TestNAR tester = test;
        tester.input("--x:y! :|:");
        tester.input("(goto(z) <=>+5 x:y).");
        tester.mustDesire(cycles, "goto(z)", 0.0f, 0.81f, 0);
        tester.mustNotOutput(cycles, "goto(z)", GOAL, 0.9f, 1f, 0f, 0.81f, -5);
    }

    @Test
    public void goal_deduction_equi_pos_posneg() {

        test
                .input("(R)! :|:")
                .input("((S) <=>+5 --(R)).") //internally, this reduces to --(S ==> R)
                .mustDesire(cycles, "(S)", 0.0f, 0.81f, 0);

    }

    @Test
    public void goal_deduction_equi_pos_posneg_var() {

        test

                .input("g(x)! :|:")
                .input("(f($1) <=>+5 --g($1)).") //internally, this reduces to --(S ==> R)
                .mustDesire(cycles, "f(x)", 0.0f, 0.81f, 0)
                .mustNotOutput(cycles, "goto({t003})", GOAL, 0);

    }

    @Test
    public void goal_deduction_equi_neg_posneg() {

        test
                .log()
                .input("--(R)! :|:")
                .input("((S) <=>+5 --(R)).") //internally, this reduces to --(S <=> R)
                .mustDesire(cycles, "(S)", 1.0f, 0.81f, 0 /* shifted to present */)
                .mustNotOutput(cycles, "(S)", GOAL, 0f, 0.5f, 0f, 1f, 0)
                .mustNotOutput(cycles, "(S)", GOAL, 0, 0.5f, 0f, 1f, -5)
        ;
    }

    @Test
    public void goal_deduction_equi_subst() {

        TestNAR tester = test;
        tester.input("x:y! :|:");
        tester.input("(goto($1) <=>+5 $1:y).");
        tester.mustDesire(cycles, "goto(x)", 1.0f, 0.81f, 0);
    }

    @Test
    public void goal_deduction_tensed_conseq_noVar() {

        TestNAR tester = test;

        tester.inputAt(1, "goto(x). :|:");
        tester.inputAt(10, "(goto(x) ==>+5 at(SELF,x)).");

        tester.mustBelieve(cycles, "at(SELF,x)", 1.0f, 0.81f, 6);
    }

    @Test
    public void belief_deduction_by_condition() {

        TestNAR tester = test;

        tester.input("(open({t001}) ==>+5 [opened]:{t001}).");
        tester.inputAt(10, "open({t001}). :|:");

        tester.mustBelieve(cycles, "[opened]:{t001}", 1.0f, 0.81f, 15);

    }

    @Test
    public void condition_goal_deduction2() {

        test

                .input("a:b! :|:")
                .inputAt(10, "(( c:d &&+5 e:f ) ==>+0 a:b).")
                .mustDesire(cycles, "( c:d &&+5 e:f)", 1.0f, 0.81f, 0, 5)
                .mustNotOutput(cycles, "( c:d &&+5 e:f)", GOAL, ETERNAL)
        ;
    }

    @Test
    public void condition_goal_deduction_interval() {

        test

                .input("a:b! :|:")
                .input("(( c:d &&+5 e:f ) ==>+5 a:b).")
                .mustDesire(cycles, "( c:d &&+5 e:f)", 1.0f, 0.81f, 0, 5)
        ;
    }

    @Test
    public void condition_goal_deductionEternal() {

        test

                .input("a:b!")
                .inputAt(10, "(( c:d &&+5 e:f ) ==> a:b).")
                .mustDesire(cycles, "( c:d &&+5 e:f)", 1.0f, 0.81f, ETERNAL)
                .mustNotOutput(cycles, "( c:d &&+5 e:f)", GOAL, 0)
        ;
    }

    @Test
    public void further_detachment() {

        test
                .input("reachable(SELF,{t002}). :|:")
                .inputAt(10, "(reachable(SELF,{t002}) &&+5 pick({t002}))!")
                .mustDesire(cycles, "pick({t002})", 1.0f, 0.81f, 5);

    }

    @Test
    public void condition_goal_deduction_eternal_belief() {

        test
                .log()
                .input("reachable(SELF,{t002})! :|:")
                .inputAt(5, "((on($1,#2) &&+0 at(SELF,#2)) ==>+0 reachable(SELF,$1)).")
                .mustDesire(cycles, "(on({t002},#1) &&+0 at(SELF,#1))", 1.0f, 0.81f, 0)
                .mustNotOutput(cycles, "(at(SELF,#1) &&+0 on({t002},#1))", GOAL, ETERNAL, 5);

    }

    @Test
    public void goal_ded_2() {

        TestNAR tester = test;

        tester.log();
        tester.inputAt(0, "at(SELF,{t001}). :|:");
        tester.inputAt(0, "(at(SELF,{t001}) &&+5 open({t001}))!");

        tester.mustDesire(cycles, "open({t001})", 1.0f, 0.81f, 5);

    }

    @Test
    public void condition_goal_deduction_3simplerReverse() {

        test
                //.log()
                .inputAt(1, "at:t003! :|:")
                .input("(at:$1 ==>+5 goto:$1).")

                .mustDesire(cycles, "goto:t003", 1.0f, 0.45f, 6)
                .mustNotOutput(cycles, "goto:t003", GOAL, 0f, 1f, 0.1f, 1f, 1L);

    }


    @Test
    public void temporal_deduction_1() {

        TestNAR tester = test;

        //tester.nar;
//        tester.input("pick({t002}). :\\:");
//        tester.inputAt(10, "(pick({t002}) ==>+5 hold({t002})). :\\:");
//
//        tester.mustBelieve(cycles, "hold({t002})", 1.0f, 0.81f, 0);

        tester.input("pick:t2. :|:");
        tester.inputAt(10, "(pick:t2 ==>+5 hold:t2).");
        tester.mustBelieve(cycles, "hold:t2", 1.0f, 0.81f, 5);

    }

    @Test
    public void further_detachment_2() {

        test
                .input("reachable(SELF,{t002}). :|:")
                .inputAt(3, "((reachable(SELF,{t002}) &&+5 pick({t002})) ==>+7 hold(SELF,{t002})).")
                .mustBelieve(cycles, "(pick({t002}) ==>+7 hold(SELF, {t002}))", 1.0f, 0.81f, 5)
        //.mustNotOutput(cycles, "(pick({t002}) ==>+7 hold(SELF, {t002}))", BELIEF, 0)
        ;

    }

    @Test
    public void goal_deduction_2() {

        TestNAR tester = test;

        tester.input("goto({t001}). :|: ");
        tester.inputAt(7, "(goto($1) ==>+2 at(SELF,$1)). ");

        tester.mustBelieve(cycles, "at(SELF,{t001})", 1.0f, 0.81f, 2);

    }
      @Test
    public void condition_goal_deduction_2() {

        test

                .input("on({t002},{t003}). :|:")
                .input("(on({t002},#1) &&+0 at(SELF,#1))!")
                .mustDesire(cycles, "at(SELF,{t003})", 1.0f, 0.81f, 0);

        //tester.mustNotOutput(time, selfAtT3, GOAL, 0, 1f, 0, 1f, ETERNAL);
    }
        @Test
    public void condition_belief_deduction_2() {

        test
                .input("on({t002},{t003}). :|:")
                .inputAt(2, "(on({t002},#1) &&+0 at(SELF,#1)).")
                .mustBelieve(cycles, "at(SELF,{t003})", 1.0f, 0.43f, 0)
                .mustNotOutput(cycles, "at(SELF,{t003})", BELIEF, 0, 1f, 0, 1f, ETERNAL);
    }



    @Test
    public void condition_belief_deduction_2_neg() {

        test
                .input("(--,on({t002},{t003})). :|:")
                .inputAt(2, "((--,on({t002},#1)) &&+0 at(SELF,#1)).")
                .mustBelieve(cycles, "at(SELF,{t003})", 1.0f, 0.43f, 0)
                .mustNotOutput(cycles, "at(SELF,{t003})", BELIEF, 0, 1f, 0, 1f, ETERNAL);
    }

    @Test
    public void condition_belief_deduction_2_easier() {

        test
                .input("on(t002,t003). :|:")
                .inputAt(2, "(on(t002,#1) &&+0 at(SELF,#1)).")
                .mustBelieve(cycles, "at(SELF,t003)", 1.0f, 0.43f, 0)
                .mustNotOutput(cycles, "at(SELF,t003)", BELIEF, 0, 1f, 0, 1f, ETERNAL);
    }

    @Ignore
    @Test
    public void condition_belief_deduction_2_dternal() {


        test

                .input("on:(t002,t003). :|:")
                .inputAt(10, "(on:(t002,#1) && at(SELF,#1)).") //<-- DTERNAL
                .mustBelieve(cycles * 4, "at(SELF,t003)", 1.0f, 0.43f, 0)
        //TODO mustNotBelieve? ^ the DTERNAL conjunctoin maybe should not be decomposed
        ;
    }

        @Test
    public void temporal_goal_detachment_1() {

        test

                .input("(hold). :|:")
                .input("( (hold) &&+5 ((at) &&+5 (open)) )!")
                .mustDesire(cycles, "((at) &&+5 (open))", 1.0f, 0.81f, 5, 10)
                .mustNotOutput(cycles, "((at) &&+5 (open))", GOAL, ETERNAL)
        ;
    }

    @Test
    public void temporal_goal_detachment_2() {
        //this is the reverse case which should not be derived by decomposing the belief

        test
                .input("(hold)! :|:")
                .inputAt(2, "( (hold) &&+5 (eat) ).") //should not decomposed by the goal task
                .mustDesire(cycles, "(eat)", 1f, 0.81f, 5)
        ;
    }

    @Test
    public void temporal_goal_detachment_3_valid() {

        test
                .log()
                .input("(use)! :|:")
                .inputAt(2, "( (hold) &&+5 (use) ).") //should be decomposed by the goal task
                .mustDesire(cycles, "(hold)", 1f, 0.81f, 0)
                .mustNotOutput(cycles, "(use)", GOAL, ETERNAL) //not eternal, we have a temporal basis here
                .mustNotOutput(cycles, "(hold)", GOAL, ETERNAL)
        ;
    }

    @Test
    public void temporal_goal_detachment_3_valid_negate() {

        test
                .input("--(use)! :|:")
                .inputAt(1, "( (hold) &&+5 --(use) ).")
                .mustDesire(cycles, "(hold)", 1f, 0.81f, 0)
                .mustNotOutput(cycles, "(use)", GOAL, ETERNAL) //not eternal, we have a temporal basis here
        ;
    }

    @Test
    public void detaching_condition0() {

        TestNAR tester = test;

        tester.input("( ( hold:t2 &&+5 (att1 &&+5 open:t1)) ==>+5 opened:t1).");
        tester.inputAt(10, "hold:t2. :|:");

        tester.mustBelieve(cycles, "((att1 &&+5 open:t1) ==>+5 opened:t1)", 1.0f, 0.81f, 15);

    }

    @Test
    public void detaching_condition() {

        test
                .input("( ( hold(SELF,{t002}) &&+5 (at(SELF,{t001}) &&+5 open({t001}))) ==>+5 [opened]:{t001}).")
                .inputAt(10, "hold(SELF,{t002}). :|:")
                .mustBelieve(cycles, "((at(SELF,{t001}) &&+5 open({t001})) ==>+5 [opened]:{t001})", 1.0f, 0.81f, 15);

    }

}
