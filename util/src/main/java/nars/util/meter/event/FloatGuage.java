package nars.util.meter.event;

import nars.util.math.DoubleSummaryReusableStatistics;

/**
 * Sums guaged objects, and returns
 */
public class FloatGuage extends DoubleSummaryReusableStatistics {

    public final String id;

    public FloatGuage(String id) {
        super();
        this.id = id;
    }
}
