package nars.op;

import nars.$;
import nars.NAR;
import nars.nal.Tense;
import nars.term.Compound;
import nars.util.math.FloatSupplier;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static nars.$.logger;

/**
 * Created by me on 6/27/16.
 */
public abstract class RelativeSignalClassifier  {
    protected final DescriptiveStatistics history;
    private final FloatSupplier input;
    private final IntConsumer output;

    public RelativeSignalClassifier(FloatSupplier input, int windowSize, IntConsumer classified) {
        this.history = new DescriptiveStatistics(windowSize);
        this.input = input;
        this.output = classified;
    }

    public void run() {
        float h = input();

        float dMean = (float)(h - history.getMean());
        double varianceThresh = history.getVariance();
        history.addValue(h);

        //TODO add more discretization range parmeters
        int y;

        Compound e;
        if (dMean > varianceThresh) {
            y = +1;
        } else if (dMean < -varianceThresh) {
            y = -1;
        } else {
            y = 0;
        }

        output.accept(y);

    }

    public float input() {
        return input.asFloat();
    }


}
