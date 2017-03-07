package jcog.meter.event;

import jcog.math.RecycledSummaryStatistics;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;

import java.io.Serializable;

/**
 * Sums guaged objects, and returns
 */
public class FloatGuage extends RecycledSummaryStatistics {

    public final String id;

    public FloatGuage(String id) {
        super();
        this.id = id;
    }


}
