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
public class NAL7NewTest extends AbstractNALTester {

    final int cycles = 25;

    public NAL7NewTest(Supplier<NAR> b) {
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
        .mustBelieve(cycles, "(x:before ==>+10 x:after)", 1.00f, inductionConf, 0)
        .mustBelieve(cycles, "(x:after ==>-10 x:before)", 1.00f, abductionConf, 0)
        .mustBelieve(cycles, "(x:after <=>-10 x:before)", 1.00f, comparisonConf, 0)
        .mustBelieve(cycles, "(x:after &&-10 x:before)", 1.00f, intersectionConf, 0)
        ;

//        tester.mustBelieve(cycles, "<<(John, room) --> enter> =\\> (&/, <(John, door) --> open>, /6)>",
//                1.00f, 0.45f,
//                11);
    }

}
