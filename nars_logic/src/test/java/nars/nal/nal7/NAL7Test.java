package nars.nal.nal7;

import nars.NAR;
import nars.Narsese;
import nars.nal.AbstractNALTester;
import nars.util.meter.TestNAR;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Supplier;

/**
 * Created by me on 1/9/16.
 */
@RunWith(Parameterized.class)
public class NAL7Test extends AbstractNALTester {

    final int cycles = 25;

    public NAL7Test(Supplier<NAR> b) {
        super(b);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable configurations() {
        return AbstractNALTester.nars(7, true, true);
    }



    @Test
    public void induction_on_events() throws Narsese.NarseseException {
        /*

        //    P, S, after(Task,Belief), measure_time(I), notImplicationOrEquivalence(P), notImplicationOrEquivalence(S) |- ((&/,S,I) =/> P), (Belief:Induction, Eternalize:Immediate),
        //                                                                                                                       (P =\> (&/,S,I)), (Belief:Abduction),
        //                                                                                                                       ((&/,S,I) </> P), (Belief:Comparison)

        //    P, S, after(Task,Belief), notConjunction(P), notConjunction(S),  measure_time(I), notImplicationOrEquivalence(P), notImplicationOrEquivalence(S) |- (&/,S,I,P), (Belief:Intersection)

         */


        float inductionConf = 0.45f;
        float abductionConf = 0.45f;
        float comparisonConf = 0.45f;
        float intersectionConf = 0.81f;

        TestNAR t = test();
        //t.nar.log();

        t
        .input("x:before. :|:")
        .inputAt(10, "x:after. :|:")
        .mustBelieve(11, "(x:before ==>+10 x:after)", 1.00f, inductionConf, 0)
        .mustBelieve(11, "(x:after ==>-10 x:before)", 1.00f, abductionConf, 0)
        .mustBelieve(11, "(x:after <=>-10 x:before)", 1.00f, comparisonConf, 0)
        .mustBelieve(11, "(x:after &&-10 x:before)", 1.00f, intersectionConf, 0)
        ;

//        tester.mustBelieve(cycles, "<<(John, room) --> enter> =\\> (&/, <(John, door) --> open>, /6)>",
//                1.00f, 0.45f,
//                11);
    }



    @Test
    public void temporal_explification() throws Narsese.NarseseException {
        TestNAR tester = test();
        tester.believe("(<($x, room) --> enter> ==>-5 <($x, door) --> open>)", 0.9f, 0.9f);
        tester.believe("(<($y, door) --> open> ==>-5 <($y, key) --> hold>)", 0.8f, 0.9f);

        tester.mustBelieve(cycles, "(<($1,key) --> hold> ==>+5 <($1,room) --> enter>)", 1.00f, 0.37f);

    }


    @Test
    public void temporal_analogy() throws Narsese.NarseseException {
        TestNAR tester = test();
        tester.believe("(<($x, door) --> open> ==>+5 <($x, room) --> enter>)",
                0.95f, 0.9f);
        tester.believe("(<($x, room) --> enter> <=>+0 <($x, corridor_100) --> leave>)",
                1.0f, 0.9f);

        tester.mustBelieve(cycles, "(<($1, door) --> open> ==>+5 <($1, corridor_100) --> leave>)", 0.95f, 0.81f);

    }


    @Test public void updating_and_revision() throws Narsese.NarseseException {
        testTemporalRevision(10, 0.50f, 0.95f, "<(John,key) --> hold>");
    }
    @Test public void updating_and_revision2() throws Narsese.NarseseException {
        testTemporalRevision(1, 0.50f, 0.95f, "<(John,key) --> hold>");
    }

    void testTemporalRevision(int delay, float freq, float conf, String belief) {
        TestNAR tester = test();
        tester.input(belief + ". :|: %1%");
        tester.inputAt(delay, belief + ". :|: %0%");
        tester.mustBelieve(delay+1, belief,  freq, conf, delay);
    }


}
