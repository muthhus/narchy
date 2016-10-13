package nars.util.signal;

import nars.NAR;
import nars.concept.FuzzyScalarConcepts;
import nars.concept.SensorConcept;
import nars.nar.Default;
import nars.util.Texts;
import nars.util.Util;
import nars.util.math.FloatNormalized;
import nars.util.math.FloatPolarNormalized;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.junit.Test;

import java.util.stream.StreamSupport;

import static org.junit.Assert.assertTrue;

/**
 * Created by me on 7/2/16.
 */
public class FuzzyScalarConceptsTest {

    //HACK TODO make sure this is smaller
    final static float tolerance = 0.2f;

    @Test
    public void testRewardConceptsFuzzification1() {
        NAR d = new Default();
        MutableFloat m = new MutableFloat(0f);

        testSteadyFreqCondition(m,
            new FuzzyScalarConcepts(
                new FloatNormalized(() -> m.floatValue()).updateRange(-1).updateRange(1),
                d, "(x)"),
                (f) -> Util.equals(f, 0.5f + 0.5f * m.floatValue(), tolerance)
        );
    }

    @Test
    public void testRewardConceptsFuzzification3() {
        NAR d = new Default();
        MutableFloat m = new MutableFloat(0f);

        FloatPolarNormalized range = new FloatPolarNormalized(() -> m.floatValue());
        range.radius(1f);
        FuzzyScalarConcepts f = new FuzzyScalarConcepts(range, d,
                "(low)", "(mid)", "(hih)");



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


        testSteadyFreqCondition(m, f, (freqSum) -> Util.equals(freqSum, 1f, tolerance));
    }

    public void testSteadyFreqCondition(MutableFloat m, FuzzyScalarConcepts f, FloatPredicate withFreqSum) {
        NAR d = f.nar;
        for (int i = 0; i < 32; i++) {
            m.setValue(Math.sin(i/2f));
            d.next();


            double freqSum = StreamSupport.stream(f.spliterator(), false)
                    .peek(SensorConcept::run)
                    .map(x -> x.belief(d.time()))
                    .mapToDouble(x -> x.freq()).sum();

            System.out.println(
                    Texts.n4(m.floatValue()) + "\t" +
                            f.toString() + " " +
                            freqSum

                    //confWeightSum(beliefs)
            );

            assertTrue(withFreqSum.accept((float)freqSum));


        }
    }
}