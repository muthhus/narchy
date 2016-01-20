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
    public void subgoal_2_small()  {
        TestNAR tester = test();

        tester.input("(hold:(SELF,y) &&+5 at:(SELF,x))!");

        tester.mustDesire(cycles, "hold:(SELF,y)", 1.0f, 0.81f);

    }

    @Test
    public void subgoal_2()  {
        TestNAR tester = test();

        tester.input("(<(SELF,{t002}) --> hold> &&+5 (<(SELF,{t001}) --> at> &&+5 open({t001})))!");

        tester.mustDesire(cycles, "<(SELF,{t002}) --> hold>",
                1.0f, 0.81f);

    }


    @Test
    public void temporal_deduction_1()  {
        TestNAR tester = test();

        //tester.nar.log();
//        tester.input("pick({t002}). :\\:");
//        tester.inputAt(10, "(pick({t002}) ==>+5 hold:({t002})). :\\:");
//
//        tester.mustBelieve(cycles, "hold:({t002})", 1.0f, 0.81f, 0);

        tester.input("pick:t2. :\\:");
        tester.inputAt(10, "(pick:t2 ==>+5 hold:t2). :\\:");

        tester.mustBelieve(cycles, "hold:t2", 1.0f, 0.81f, 0);

    }


    @Test
    public void further_detachment_2()  {
        TestNAR tester = test();

        tester.input("reachable:(SELF,{t002}). :|:");
        tester.inputAt(10, "((reachable:(SELF,{t002}) &&+5 pick({t002})) ==>+5 hold:(SELF,{t002})).");

        tester.mustBelieve(cycles, "(pick({t002}) ==>+5 hold:(SELF, {t002}))", 1.0f, 0.81f, 0);

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
    public void subgoal_1_abd()  {
        TestNAR tester = test();

        tester.input("<{t001} --> [opened]>. :|:");
        tester.inputAt(10, "<(&/, <(SELF,{t002}) --> hold>, <(SELF,{t001}) --> at>, <({t001}) --> ^open>) =/> <{t001} --> [opened]>>.");

        tester.mustBelieve(cycles, "(&/, <(SELF, {t002}) --> hold>, <(SELF, {t001}) --> at>, open({t001}))",
                1.0f, 0.45f,
                -5);

    }







    @Test
    public void temporal_goal_detachment_1()  {
        TestNAR tester = test();


        tester.input("<(SELF,{t002}) --> hold>.");
        tester.inputAt(10, "(&/, <(SELF,{t002}) --> hold>, <(SELF,{t001}) --> at>, open({t001}) )!");

        tester.mustDesire(cycles, "(&/,<(SELF,{t001}) --> at>,open({t001}))", 1.0f, 0.81f);

    }

    @Test
    public void temporal_deduction_2()  {
        TestNAR tester = test();

        tester.input("<(&/, <(SELF,{t002}) --> hold>, <(SELF,{t001}) --> at>, open({t001})) =/> <{t001} --> [opened]>>.");
        tester.inputAt(10, "<(SELF,{t002}) --> hold>. :|: ");

        //mustBelieve?
        tester.mustBelieve(cycles, "<(&/, <(SELF,{t001}) --> at>, open({t001})) =/> <{t001} --> [opened]>>", 1.0f, 0.81f, 10);

    }

    @Test
    public void detaching_condition()  {
        TestNAR tester = test();

        tester.input("<(&/, <(SELF,{t002}) --> hold>, <(SELF, {t001}) --> at>, open({t001})) =/> <{t001} --> [opened]>>.");
        tester.inputAt(10, "<(SELF,{t002}) --> hold>. :|:");

        tester.mustBelieve(cycles, "<(&/, <(SELF,{t001}) --> at>, open({t001})) =/> <{t001} --> [opened]>>", 1.0f, 0.81f, 10);

    }



    @Test
    public void detaching_single_premise2()  {
        TestNAR tester = test();

        tester.input("(&/, <(SELF,{t001}) --> at>, open({t001}) )!");


        tester.mustDesire(cycles, "<(SELF,{t001}) --> at>", 1.0f, 0.81f);

    }


    @Test
    public void goal_deduction_2()  {
        TestNAR tester = test();

        tester.input("(^goto,{t001}). :\\: ");
        tester.inputAt(10, "<(^goto,$1)=/><(SELF,$1) --> at>>. ");

        tester.mustBelieve(cycles, "<(SELF,{t001}) --> at>", 1.0f, 0.81f, 0);

    }

    @Test
    public void detaching_condition_2()  {
        TestNAR tester = test();

        tester.input("<(SELF,{t001}) --> at>. :|: ");
        tester.inputAt(10, "<(&/,<(SELF,{t001}) --> at>,(^open,{t001}))=/><{t001} --> [opened]>>. :|:");

        tester.mustBelieve(cycles, "<(^open, {t001}) =/> <{t001} --> [opened]>>", 1.0f, 0.81f, 0);

    }

    @Test
    public void goal_ded_2()  {
        TestNAR tester = test();

        tester.input("<(SELF,{t001}) --> at>. :|:");
        tester.inputAt(10, "(&/,<(SELF,{t001}) --> at>,(^open,{t001}))!");

        tester.mustDesire(cycles, "(^open,{t001})", 1.0f, 0.81f);

    }


    @Test
    public void condition_goal_deduction()  {
        TestNAR tester = test();

        tester.input("<(SELF,{t002}) --> reachable>! ");
        tester.inputAt(10, "<(&|,<($1,#2) --> on>,<(SELF,#2) --> at>)=|><(SELF,$1) --> reachable>>.");

        tester.mustDesire(cycles, "(&|,<(SELF,#1) --> at>,<({t002},#1) --> on>)", 1.0f, 0.81f);

    }

    @Test
    public void condition_goal_deduction_2()  {
        TestNAR tester = test();

        tester.input("<({t002},{t003}) --> on>. :|:");
        tester.inputAt(10, "(&|,<({t002},#1) --> on>,<(SELF,#1) --> at>)!");

        tester.mustDesire(cycles, "<(SELF,{t003}) --> at>", 1.0f, 0.81f);

    }

    @Test
    public void condition_goal_deduction_3()  {
        TestNAR tester = test();

        tester.input("<(SELF,{t003}) --> at>!");
        tester.inputAt(10, "<goto($1)=/><(SELF,$1) --> at>>.");

        tester.mustDesire(cycles, "goto({t003})", 1.0f, 0.81f);

    }

    @Test
    public void condition_goal_deduction_3_()  {
        TestNAR tester = test();

        tester.input("<(SELF,{t003}) --> at>! :|:");
        tester.inputAt(10, "<(&/,goto($1),/1) =/> <(SELF,$1) --> at>>.");

        tester.mustDesire(cycles, "goto({t003})", 1.0f, 0.81f, -5);

    }

    @Test
    public void condition_goal_deduction_3__()  {
        TestNAR tester = test();

        tester.input("<(SELF,{t003}) --> at>! :|:");
        tester.inputAt(10, "<goto($1) =/> <(SELF,$1) --> at>>.");

        tester.mustDesire(cycles, "goto({t003})", 1.0f, 0.81f, -5);

    }

    @Test
    public void conditional_abduction_test()  { //maybe to nal7 lets see how we will split these in the future
        TestNAR tester = test();

        tester.input("<(SELF,{t003}) --> at>. :|:");
        tester.inputAt(10, "<(&/,goto($1),/1) =/> <(SELF,$1) --> at>>.");

        tester.mustBelieve(cycles, "goto({t003})", 1.0f, 0.45f, -5);

    }

    @Test
    public void ded_with_var_temporal()  {
        TestNAR tester = test();

        tester.input("<({t003}) --> ^goto>. :|: ");
        tester.inputAt(10, "<<($1) --> ^goto> =/> <(SELF,$1) --> at>>. ");

        tester.mustBelieve(cycles, "<(SELF, {t003}) --> at>", 1.0f, 0.81f, 5);

    }

    @Test
    public void ded_with_var_temporal2()  {
        TestNAR tester = test();

        tester.input("<({t003}) --> ^goto>. :|: ");
        tester.inputAt(10, "<<($1) --> ^goto> =/> <(SELF,$1) --> at>>. ");

        tester.mustBelieve(cycles, "<(SELF, {t003}) --> at>", 1.0f, 0.81f,5);

    }




}
