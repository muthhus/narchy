package nars.truth;

import nars.$;
import nars.Global;
import nars.util.Texts;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static nars.$.*;
import static nars.Global.TRUTH_EPSILON;
import static org.junit.Assert.*;

/**
 * Created by me on 5/26/16.
 */
public class TruthFunctionsTest {

    @Test
    public void testBipolarComparison() {
        printTruthChart();

        assertEquals(
            TruthFunctions.comparison( t(1, 0.9f), t(1, 0.9f), TRUTH_EPSILON ),
            TruthFunctions.comparison( t(0, 0.9f), t(0, 0.9f), TRUTH_EPSILON )
        );
    }

    public static void printTruthChart() {
        float c = 0.9f;
        for (float f1 = 0f; f1 <= 1.001f; f1+=0.1f) {
            for (float f2 = 0f; f2 <= 1.001f; f2+=0.1f) {
                Truth t1 = t(f1, c);
                Truth t2 = t(f2, c);
                System.out.println(t1 + " " + t2 + ":\t" +
                        TruthFunctions.comparison(t1, t2, TRUTH_EPSILON));
            }
        }
    }
}