package nars.budget;

import jcog.math.MultiStatistics;
import nars.term.Termed;

/**
 * Created by me on 1/12/17.
 */
public class BudgetFairnessTest {


    private static <X extends Termed> MultiStatistics.Condition<X> complexityLTE(int complexityLTE) {
        return new MultiStatistics.Condition<X>("complexity<=" + complexityLTE, t -> t.complexity() <= complexityLTE);
    }
    private static <X extends Termed> MultiStatistics.Condition<X> volumeLTE(int complexityLTE) {
        return new MultiStatistics.Condition<X>("volume<=" + complexityLTE, t -> t.volume() <= complexityLTE);
    }
    private static <X extends Termed> MultiStatistics.Condition<X> volumeIn(int min, int max) {
        return new MultiStatistics.Condition<X>("volume=" + min + ".." + max, t -> {
            int v = t.volume();
            return (v >= min && v <= max);
        });
    }
}
