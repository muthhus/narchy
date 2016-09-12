package nars.nal.nal4;

import nars.NAR;
import nars.Narsese;
import nars.nal.AbstractNALTest;
import nars.util.signal.TestNAR;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Supplier;

@RunWith(Parameterized.class)
public class NAL4MultistepTest extends AbstractNALTest {


    public NAL4MultistepTest(Supplier<NAR> b) {
        super(b);
    }

    @Parameterized.Parameters(name = "{index}:{0}")
    public static Iterable<Supplier<NAR>> configurations() {
        return AbstractNALTest.nars(6);
    }

    //this test only works because the confidence matches, but the related task has insufficient budget
    @Test
    public void nal4_everyday_reasoning() throws Narsese.NarseseException {
        int time = 450;

        //Global.DEBUG = true;

        TestNAR tester = test();

        //tester.log();
        //tester.nar.logSummaryGT(System.out, 0.5f);

        //(({tom},{sky})-->likes).  <{tom} --> cat>. <({tom},{sky}) --> likes>. <(cat,[blue]) --> likes>?

        tester.believe("<{sky} --> [blue]>",1.0f,0.9f); //en("the sky is blue");
        tester.believe("<{tom} --> cat>",1.0f,0.9f); //en("tom is a cat");
        tester.believe("<({tom},{sky}) --> likes>",1.0f,0.9f); //en("tom likes the sky");

        tester.askAt(0, "<(cat,[blue]) --> likes>"); //cats like blue?

        tester.mustBelieve(time, "<(cat,[blue]) --> likes>", 1.0f,
                0.27f);
                //0.42f); //en("A base is something that has a reaction with an acid.");

    }

    @Test
    public void nal4_everyday_reasoning_easiest() throws Narsese.NarseseException {
        int time = 150;

        //Global.DEBUG = true;

        TestNAR tester = test();
        tester.believe("<sky --> blue>",1.0f,0.9f); //en("the sky is blue");
        //tester.believe("<tom --> cat>",1.0f,0.9f); //en("tom is a cat");
        //tester.believe("<(tom,sky) --> likes>",1.0f,0.9f); //en("tom likes the sky");
        tester.believe("<sky --> likes>",1.0f,0.9f); //en("tom likes the sky");

        //tester.ask("<(cat,blue) --> likes>"); //cats like blue?
        //tester.askAt(time/3, "<(cat,blue) --> likes>"); //cats like blue?
        //tester.askAt(time/2, "<(cat,blue) --> likes>"); //cats like blue?
        tester.ask("<blue --> likes>"); //cats like blue?

        //tester.mustBelieve(time, "<(cat,blue) --> likes>", 1.0f, 0.42f); //en("A base is something that has a reaction with an acid.");
        tester.mustBelieve(time, "<blue --> likes>", 1.0f, 0.45f); //en("A base is something that has a reaction with an acid.");

    }

    @Test @Ignore
    public void nal4_everyday_reasoning_easier() throws Narsese.NarseseException {
        int time = 2550;

        //Global.DEBUG = true;

        TestNAR tester = test();
        //tester.nar.log();
        tester.believe("<sky --> blue>",1.0f,0.9f); //en("the sky is blue");
        tester.believe("<tom --> cat>",1.0f,0.9f); //en("tom is a cat");
        tester.believe("<(tom,sky) --> likes>",1.0f,0.9f); //en("tom likes the sky");



        tester.ask("<(cat,blue) --> likes>"); //cats like blue?
        //tester.askAt(time/3, "<(cat,blue) --> likes>"); //cats like blue?
        //tester.askAt(time/2, "<(cat,blue) --> likes>"); //cats like blue?


        tester.mustBelieve(time, "<(cat,blue) --> likes>", 1.0f, 0.42f); //en("A base is something that has a reaction with an acid.");

    }

//    //like seen when changing the expected confidence in mustBelief, or also in the similar list here we have such a ghost task where I expect better budget:
//
//    @Test
//    public void multistep_budget_ok() throws Narsese.NarseseException {
//        TestNAR tester = test();
//        //tester.nar.log();
//        tester.believe("<{sky} --> [blue]>",1.0f,0.9f); //en("the sky is blue");
//        tester.believe("<{tom} --> cat>",1.0f,0.9f); //en("tom is a cat");
//        tester.believe("<({tom},{sky}) --> likes>",1.0f,0.9f); //en("tom likes the sky");
//        tester.askAt(500,"<(cat,[blue]) --> likes>"); //cats like blue?
//        //tester.mustAnswer(1000, "<(cat,[blue]) --> likes>", 1.0f, 0.42f, Tense.Eternal); //en("A base is something that has a reaction with an acid.");
//        tester.mustBelieve(1000, "<(cat,[blue]) --> likes>", 1.0f, 0.42f); //en("A base is something that has a reaction with an acid.");
//
//    }

}
