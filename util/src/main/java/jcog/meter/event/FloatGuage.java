package jcog.meter.event;

import jcog.math.FloatSummaryReusableStatistics;

/**
 * Sums guaged objects, and returns
 */
public class FloatGuage extends FloatSummaryReusableStatistics {

    public final String id;

    public FloatGuage(String id) {
        super();
        this.id = id;
    }
}
