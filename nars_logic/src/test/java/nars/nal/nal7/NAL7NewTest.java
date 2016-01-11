package nars.nal.nal7;

import nars.NAR;
import nars.Narsese;
import nars.nal.AbstractNALTester;
import nars.util.meter.TestNAR;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Supplier;

import static nars.$.$;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by me on 1/9/16.
 */
@RunWith(Parameterized.class)
public class NAL7NewTest extends AbstractNALTester {

    final int cycles = 104;

    public NAL7NewTest(Supplier<NAR> b) {
        super(b);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable configurations() {
        return AbstractNALTester.nars(7, true, true);
    }

    @Test public void parseTemporalRelation() {
        //TODO move to NarseseTest
        assertEquals("<x ==>+5 y>", $("<x ==>+5 y>").toString());
        assertEquals("<x &&+5 y>", $("<x &&+5 y>").toString());

        assertEquals("<x ==>-5 y>", $("<x ==>-5 y>").toString());

        assertEquals("<<before-->x> ==>+5 <after-->x>>", $("<x:before ==>+5 x:after>").toString());
    }
    @Test public void temporalEqualityAndCompare() {
        assertNotEquals( $("<x ==>+5 y>"), $("<x ==>+0 y>") );
        assertNotEquals( $("<x ==>+5 y>").hashCode(), $("<x ==>+0 y>").hashCode() );
        assertNotEquals( $("<x ==> y>"), $("<x ==>+0 y>") );
        assertNotEquals( $("<x ==> y>").hashCode(), $("<x ==>+0 y>").hashCode() );

        assertEquals( $("<x ==>+0 y>"), $("<x ==>-0 y>") );

        assertEquals(0,   $("<x ==>+0 y>").compareTo( $("<x ==>+0 y>") ) );
        assertEquals(-1,  $("<x ==>+0 y>").compareTo( $("<x ==>+1 y>") ) );
        assertEquals(+1,  $("<x ==>+1 y>").compareTo( $("<x ==>+0 y>") ) );
    }


    @Test public void testReversibilityOfCommutive() {
        assertEquals("<a <=>+5 b>", $("<a <=>+5 b>").toString());
        assertEquals("<a <=>-5 b>", $("<b <=>+5 a>").toString());

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
        float intersectionConf = 0.45f;

        TestNAR t = test();
        //t.nar.log();

        t
        .input("x:before. :|:")
        .inputAt(10, "x:after. :|:")
        .mustBelieve(cycles, "<x:before ==>+10 x:after>", 1.00f, inductionConf, 10)
        .mustBelieve(cycles, "<x:after ==>-10 x:before>", 1.00f, abductionConf, 0)
        .mustBelieve(cycles, "<x:before <=>+10 x:after>", 1.00f, comparisonConf, 11)
        .mustBelieve(cycles, "<x:after <=>-10 x:before>", 1.00f, comparisonConf, 11)
        .mustBelieve(cycles, "<x:before &&+10 x:after>", 1.00f, intersectionConf, 11)
        ;

    }

}
