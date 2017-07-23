package nars.util.signal;

import jcog.Texts;
import jcog.Util;
import jcog.math.FloatNormalized;
import jcog.math.FloatPolarNormalized;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.concept.FuzzyScalarConcepts;
import nars.nar.Terminal;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.junit.Ignore;
import org.junit.Test;

import java.util.stream.StreamSupport;

import static org.junit.Assert.assertTrue;

/**
 * Created by me on 7/2/16.
 */
public class FuzzyScalarConceptsTest {

    //HACK TODO make sure this is smaller
    final static float tolerance = 0.2f;

//    @Test
//    public void testRewardConceptsFuzzification1() {
//        NAR d = new Default();
//        MutableFloat m = new MutableFloat(0f);
//
//        testSteadyFreqCondition(m,
//            new FuzzyScalarConcepts(
//                new FloatNormalized(() -> m.floatValue()).updateRange(-1).updateRange(1),
//                d, FuzzyScalarConcepts.FuzzyTriangle, $.p("x")),
//                (f) -> Util.equals(f, 0.5f + 0.5f * m.floatValue(), tolerance)
//        );
//    }

    @Ignore
    @Test
    public void testRewardConceptsFuzzification3() {
        NAR d = new Terminal();
        MutableFloat m = new MutableFloat(0f);

        FloatPolarNormalized range = new FloatPolarNormalized(() -> m.floatValue());
        range.radius(1f);
        FuzzyScalarConcepts f = new FuzzyScalarConcepts(range, d, FuzzyScalarConcepts.FuzzyTriangle,
                $.p("low"), $.p("mid"), $.p("hih"));



//        {
//            f.clear();
//            m.setValue(0); d.next();
//            System.out.println(Texts.n4(m.floatValue()) + "\t" + f.toString());
//            assertEquals("(I-->[sad]) %0.25;.90%\t(I-->[neutral]) %1.0;.90%\t(I-->[happy]) %0.0;.90%", f.toString());
//        }
//
//        {
//            f.clear();
//            m.setValue(-1); d.next();
//            System.out.println(Texts.n4(m.floatValue()) + "\t" + f.toString());
//            assertEquals("(I-->[sad]) %1.0;.90%\t(I-->[neutral]) %0.0;.90%\t(I-->[happy]) %0.0;.90%", f.toString());
//        }
//
//        {
//            f.clear();
//            m.setValue(+1); d.next();
//            System.out.println(Texts.n4(m.floatValue()) + "\t" + f.toString());
//            assertEquals("(I-->[sad]) %0.0;.90%\t(I-->[neutral]) %0.0;.90%\t(I-->[happy]) %1.0;.90%", f.toString());
//        }


        testSteadyFreqCondition(m, f, (freqSum) -> {
            System.out.println(freqSum + " " + tolerance);
            return Util.equals(freqSum, 1f, tolerance);
        });
    }

    public void testSteadyFreqCondition(MutableFloat m, FuzzyScalarConcepts f, FloatPredicate withFreqSum) {
        NAR n = f.nar;
        //run a few oscillations
        for (int i = 0; i < 5; i++) {
            m.setValue(Math.sin(i/2f));
            n.cycle();


            double freqSum = StreamSupport.stream(f.sensors.spliterator(), false)
                    .peek(x -> n.input(x.apply(n)))
                    .map(x -> n.beliefTruth(x, n.time()))
                    .mapToDouble(x -> x!=null ? x.freq() : 0f).sum();

            System.out.println(
                    Texts.n4(m.floatValue()) + "\t" +
                            f + " " +
                            freqSum

                    //confWeightSum(beliefs)
            );

            assertTrue(withFreqSum.accept((float)freqSum));


        }
    }

    @Test
    public void testRewardConceptsFuzzification2() {
        NAR d = new NARS().get();
        MutableFloat m = new MutableFloat(0f);

        testSteadyFreqCondition(m,
                new FuzzyScalarConcepts(
                        new FloatNormalized(() -> m.floatValue()).updateRange(-1).updateRange(1),
                        d, FuzzyScalarConcepts.FuzzyBinary, $.p("x0"), $.p("x1"), $.p("x2")),
                (f) -> true /*Util.equals(f, 0.5f + 0.5f * m.floatValue(), tolerance)*/
        );
    }
}