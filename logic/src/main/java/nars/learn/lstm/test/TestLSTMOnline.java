package nars.learn.lstm.test;

import nars.learn.lstm.AbstractTraining;
import nars.learn.lstm.Interaction;
import nars.learn.lstm.SimpleLSTM;
import nars.util.Texts;
import nars.util.data.random.XorShift128PlusRandom;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.util.MathArrays;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Tests an LSTM in continuous active mode
 */
public class TestLSTMOnline {

    abstract public static class LiveSTM extends AbstractTraining {

        private final SimpleLSTM agent;

        @Deprecated private final int ERROR_WINDOW_SIZE = 8;

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
         *   * expected = null:     prediction only
         *   * expected = non-null: learn (optional validation mode)
         *
         *  the input and output arrays are not modified or retained, so you may re-use them
         */
        protected abstract Interaction observe();

        @Override
        protected void interact(Consumer<Interaction> each) {
            throw new UnsupportedOperationException();
        }
    }

    public static void main(String[] args) throws Exception {

        final int seqPeriod = 12;

        int inputs = 4;
        int outputs = 8;
        int cells = 8;

        Interaction i = Interaction.the(inputs, outputs);
        double[] expect = i.expected;
        i.zero();


        LiveSTM l = new LiveSTM(inputs, outputs, cells, 0.1) {

            int t;


            @Override
            protected Interaction observe() {


                i.expected = expect;
                int tt = t % seqPeriod;


                i.forget =
                        //(tt == 0) ? 0.9f : 0.5f;
                        0.1f;

                Arrays.fill(i.actual, 0);
                i.actual[(tt/3)%4] = 1;
                Arrays.fill(expect, 0);
                expect[(int)Math.round(  ((Math.sin(tt)+1f)/2f)*7f ) ] = 1f;

                //for (int x = 0; x < inputs; x++) {
                    //i.actual[x] = ((t + x * 37) ^ 5)%2 * 0.9f + random.nextDouble()*0.1f; //trippy tri-tone xor line chart
                //}

                //for (int x = 0; x < outputs; x++) {
                //    expect[x] = ((t * 3 + x) ^ 13) % 2;
                //}

                if ((t/20)%2 == 0) {
                    //train
                    validation_mode = false;
                } /*else {
                    validation_mode = true;
                    //predict
                    //i.expected = null;
                }*/

                t++;

                return i;
            }
        };

        for (int c = 0; c < 400000; c++) {
            System.out.println(c + "\t\t" + Texts.n4( l.next()) + "\t\t" + i);
        }
    }

}
