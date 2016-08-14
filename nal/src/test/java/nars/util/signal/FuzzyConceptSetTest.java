package nars.util.signal;

import com.google.common.collect.Iterables;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import nars.NAR;
import nars.nar.Default;
import nars.truth.Truth;
import nars.util.Texts;
import nars.util.Util;
import nars.util.math.PolarRangeNormalizedFloat;
import nars.util.math.RangeNormalizedFloat;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.junit.Test;

import java.util.stream.StreamSupport;

import static org.junit.Assert.assertTrue;

/**
 * Created by me on 7/2/16.
 */
public class FuzzyConceptSetTest {

    //HACK TODO make sure this is smaller
    final static float tolerance = 0.2f;

    @Test
    public void testRewardConceptsFuzzification1() {
        NAR d = new Default();
        MutableFloat m = new MutableFloat(0f);

        testSteadyFreqCondition(m,
            new FuzzyConceptSet(
                new RangeNormalizedFloat(() -> m.floatValue()).updateRange(-1).updateRange(1),
                d, "(x)"),
                (f) -> Util.equals(f, 0.5f + 0.5f * m.floatValue(), tolerance)
        );
    }

    @Test
    public void testRewardConceptsFuzzification3() {
        NAR d = new Default();
        MutableFloat m = new MutableFloat(0f);

        PolarRangeNormalizedFloat range = new PolarRangeNormalizedFloat(() -> m.floatValue());
        range.radius(1f);
        FuzzyConceptSet f = new FuzzyConceptSet(range, d,
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

    public void testSteadyFreqCondition(MutableFloat m, FuzzyConceptSet f, FloatPredicate withFreqSum) {
        NAR d = f.nar;
        for (int i = 0; i < 32; i++) {
            m.setValue(Math.sin(i/2f));
            d.next();
            Iterable<Truth> beliefs = Iterables.transform(f, x -> x.belief(d.time()));

            double freqSum = StreamSupport.stream(beliefs.spliterator(), false).mapToDouble(x -> x.freq()).sum();

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