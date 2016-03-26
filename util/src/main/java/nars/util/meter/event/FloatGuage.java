package nars.util.meter.event;

import nars.util.DoubleSummaryReusableStatistics;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

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
