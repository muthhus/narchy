package nars.util.signal;

import jcog.learn.lstm.Interaction;
import jcog.learn.lstm.test.LiveSTM;
import jcog.list.FasterList;
import jcog.math.FloatDelay;
import jcog.math.FloatSupplier;
import org.apache.commons.lang3.mutable.MutableFloat;

import java.util.Collection;

import static jcog.Texts.n4;

/**
 * NOT TESTED YET
 * http://www.jakob-aungiers.com/articles/a/LSTM-Neural-Network-for-Time-Series-Prediction
 */
public class LivePredictor {


    public interface LivePredictorModel {
        /** may be called at any time to reinitialize the architecture */
        public void init(int ins, int inHistory, int outs, int outHistory);

        void learn(double[] ins, double[] outs);

        public double[] predict();
    }

    public static class LSTMPredictor implements LivePredictorModel {
        float learningRate = 0.01f;
        private LiveSTM net;
        private double[] nextPredictions;

        @Override
        public void init(int numInputs, int iHistory, int numOutputs, int oHistory) {
            synchronized (this) {
                net = new LiveSTM(numInputs * iHistory, numOutputs * oHistory, numInputs * numOutputs * Math.max(iHistory, oHistory)) {
                    @Deprecated
                    @Override
                    protected Interaction observe() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        }

        @Override
        public void learn(double[] ins, double[] outs) {
            synchronized (this) {
                nextPredictions = net.agent.learn(ins, outs, learningRate);
            }
        }

        public double[] predict() {
            return nextPredictions;
        }

    }

    /* TODO public static class NTMPredictor implements LivePredictorModel {

    } */

    private final DelayedFloats Ihistory;
    private final DelayedFloats Ohistory;

    /** temporary buffers, re-used */
    private double[] ti, to;

    private final FloatSupplier[] INS;
    private final FloatSupplier[] OUTS;
    public final LivePredictorModel model;


    public LivePredictor(LivePredictorModel model, FloatSupplier[] INS, int iHistory, FloatSupplier[] OUTS, int oHistory) {

        this.INS = INS;
        this.OUTS = OUTS;
        this.Ihistory = DelayedFloats.delay(INS, iHistory);
        this.Ohistory = DelayedFloats.delay(OUTS, oHistory);

        this.model = model;
        model.init(INS.length, iHistory, OUTS.length, oHistory);
    }

    public static class DelayedFloats extends FasterList<FloatDelay> {

        public DelayedFloats(int size) {
            super(size);
        }

        public void next() {
            forEach(FloatDelay::next);
        }

        static DelayedFloats delay(FloatSupplier[] vector, int history) {
            DelayedFloats delayed = new DelayedFloats(vector.length);
            for (FloatSupplier f : vector)
                delayed.add(new FloatDelay(f, history));
            return delayed;
        }
        public void print() {
            forEach(System.out::println);
        }
    }


    public synchronized double[] next() {
        Ohistory.next();
        Ihistory.next();
        //Ihistory.print();
        //Ohistory.print();

        model.learn(ti = historyVector(Ihistory,Ihistory.get(0).data.length, ti), to = historyVector(Ohistory, Ohistory.get(0).data.length, to));

        return model.predict();
    }

    public static double[] d(FloatSupplier[] f) {
        double[] d = new double[f.length];
        int i = 0;
        for (FloatSupplier g : f)
            d[i++] = g.asFloat();
        return d;
    }


    static double[] historyVector(Collection<? extends FloatDelay> f, int history, double[] d) {
        if (d==null || d.length != f.size() * history) {
            d = new double[f.size() * history];
        }
        int i = 0;
        for (FloatDelay g : f) {
            float[] gd = g.data;
            for (int k = 0; k < gd.length; k++)
                d[i++] = gd[k];
        }
        return d;
    }

    public static void main(String[] args) {
        MutableFloat m = new MutableFloat();

        FloatSupplier[] in = {
                () -> 1f * (m.floatValue() % 2) > 0 ? 1 : -1,
                () -> 1f * ((m.floatValue() % 3) > 0 ? 1 : -1)
        };
        FloatSupplier[] out = {
                () -> 1f * (((m.floatValue() % 2) + (m.floatValue() % 3)) > 2 ? 1 : -1)
        };
        LivePredictor l = new LivePredictor(new LSTMPredictor(),
                in,
                5, out, 1
        );

        for (int i = 0; i < 1500; i++) {
            double[] prediction = l.next();

            System.out.print( n4(prediction) + "\t=?=\t");
            m.increment();
            System.out.println(n4(d(in)) + "\t" + n4(d(out)) );
        }

    }

}

//    public static double[] d(Collection<? extends FloatSupplier> f) {
//        double[] d = new double[f.size()];
//        int i = 0;
//        for (FloatSupplier g : f)
//            d[i++] = g.asFloat();
//        return d;
//    }

