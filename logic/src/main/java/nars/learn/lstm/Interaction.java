package nars.learn.lstm;

import nars.util.Texts;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;

/**
 * Created by me on 5/23/16.
 */
public final class Interaction {

    public double[] actual;
    public double[] expected;
    public double[] predicted = null;

    public boolean reset;


    public static Interaction the(int numActual, int numExpected) {
        return the(new double[numActual], new double[numExpected]);
    }

    public static Interaction the(double[] actual, double[] expected) {
        return the(actual, expected, false);
    }

    public static Interaction the(double[] actual, double[] expected, boolean reset) {
        Interaction i = new Interaction();
        i.actual = actual;
        i.expected = expected;
        i.reset = reset;
        return i;
    }

    @Override
    public String toString() {
        return Texts.n4(actual) + "\t" +
                Texts.n4(expected) + "\t" +
                Texts.n4(predicted) +
                (reset ? "RESET" : "")
                ;
    }

    public void zero() {
        Arrays.fill(actual, 0);
        if (expected!=null)
            Arrays.fill(expected, 0);
        reset = false;
    }
}
