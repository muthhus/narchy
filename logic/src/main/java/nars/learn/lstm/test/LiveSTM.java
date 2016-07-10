package nars.learn.lstm.test;

import nars.learn.lstm.AbstractTraining;
import nars.learn.lstm.Interaction;
import nars.learn.lstm.SimpleLSTM;
import nars.util.data.random.XorShift128PlusRandom;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.util.MathArrays;

import java.util.Random;
import java.util.function.Consumer;

/**
 * Created by me on 7/9/16.
 */
abstract public class LiveSTM extends AbstractTraining {

    private final SimpleLSTM agent;

    @Deprecated
    private final int ERROR_WINDOW_SIZE = 8;

    public boolean train = true;
    DescriptiveStatistics errorHistory = new DescriptiveStatistics();

    public LiveSTM(int inputs, int outputs, int cellBlocks, double initialLearningRate) {
        this(new XorShift128PlusRandom(1), inputs, outputs, cellBlocks, initialLearningRate);
    }

    public LiveSTM(Random random, int inputs, int outputs, int cellBlocks, double initialLearningRate) {
        super(random, inputs, outputs);

        this.agent = lstm(cellBlocks, initialLearningRate);

        errorHistory.setWindowSize(ERROR_WINDOW_SIZE);
    }

    public double next() {


        Interaction inter = observe();


        double[] predicted;

        double dist;
        if (inter.expected == null) {

            inter.predicted = agent.predict(inter.actual, true);

            dist = Float.NaN;

        } else {

            if (validation_mode)
                predicted = agent.predict(inter.actual, true);
            else
                predicted = agent.learn(inter.actual, inter.expected, true);

//                max_fit++;
//
//                if (util.argmax(predicted) == util.argmax(inter.expected))
//                    fit++;

            inter.predicted = predicted;

            dist = MathArrays.distance1(inter.expected, predicted); //manhattan / hamming distance

        }

        if (inter.forget > 0)
            agent.forget(inter.forget);

        errorHistory.addValue(dist);

        return errorHistory.getMean();

    }

    /**
     * the content of the returned Interaction determines the following modes:
     * * expected = null:     prediction only
     * * expected = non-null: learn (optional validation mode)
     * <p>
     * the input and output arrays are not modified or retained, so you may re-use them
     */
    protected abstract Interaction observe();

    @Override
    protected void interact(Consumer<Interaction> each) {
        throw new UnsupportedOperationException();
    }
}
