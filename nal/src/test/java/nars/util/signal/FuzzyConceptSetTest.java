package nars.util.signal;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import nars.NAR;
import nars.concept.CompoundConcept;
import nars.concept.Concept;
import nars.nar.Default;
import nars.truth.Truth;
import nars.util.Texts;
import nars.util.math.PolarRangeNormalizedFloat;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.junit.Test;

import java.util.stream.StreamSupport;

import static nars.truth.Truthed.confSum;
import static nars.truth.Truthed.confWeightSum;
import static org.junit.Assert.assertEquals;

/**
 * Created by me on 7/2/16.
 */
public class FuzzyConceptSetTest {


    @Test
    public void testRewardConceptsFuzzification3() {
        NAR d = new Default();
        MutableFloat m = new MutableFloat(0f);

        PolarRangeNormalizedFloat range = new PolarRangeNormalizedFloat(() -> m.floatValue());
        range.set(1f);
        FuzzyConceptSet f = new FuzzyConceptSet(range, d,
                "(I --> [sad])", "(I --> [neutral])", "(I --> [happy])");



//        {
//            f.clear();
//            m.setValue(0); d.step();
//            System.out.println(Texts.n4(m.floatValue()) + "\t" + f.toString());
//            assertEquals("(I-->[sad]) %0.25;.90%\t(I-->[neutral]) %1.0;.90%\t(I-->[happy]) %0.0;.90%", f.toString());
//        }
//
//        {
//            f.clear();
//            m.setValue(-1); d.step();
//            System.out.println(Texts.n4(m.floatValue()) + "\t" + f.toString());
//            assertEquals("(I-->[sad]) %1.0;.90%\t(I-->[neutral]) %0.0;.90%\t(I-->[happy]) %0.0;.90%", f.toString());
//        }
//
//        {
//            f.clear();
//            m.setValue(+1); d.step();
//            System.out.println(Texts.n4(m.floatValue()) + "\t" + f.toString());
//            assertEquals("(I-->[sad]) %0.0;.90%\t(I-->[neutral]) %0.0;.90%\t(I-->[happy]) %1.0;.90%", f.toString());
//        }


        for (int i = 0; i < 32; i++) {
            m.setValue(Math.sin(i/2f));
            d.step();
            Iterable<Truth> beliefs = Iterables.transform(f, x -> x.belief(d.time()));

            double freqSum = StreamSupport.stream(beliefs.spliterator(), false).mapToDouble(x -> x.freq()).sum();

            assertEquals(1f, freqSum, 0.03f);

            System.out.println(
                    Texts.n4(m.floatValue()) + "\t" +
                    f.toString() + " " +
                    freqSum

                    //confWeightSum(beliefs)
            );
        }
    }
}