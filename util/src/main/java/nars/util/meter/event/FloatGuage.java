package nars.util.meter.event;

import nars.util.DoubleSummaryReusableStatistics;

/**
 * Sums guaged objects, and returns
 */
public class FloatGuage extends DoubleSummaryReusableStatistics {


    private final String id;

    public FloatGuage(String id) {
        super();
        this.id = id;
    }
}
