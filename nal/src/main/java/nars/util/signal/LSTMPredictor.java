package nars.util.signal;

import jcog.learn.lstm.Interaction;
import jcog.learn.lstm.test.LiveSTM;
import jcog.list.FasterList;
import jcog.math.FloatDelay;
import jcog.math.FloatSupplier;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static jcog.Texts.n4;

/**
 * NOT TESTED YET
 * http://www.jakob-aungiers.com/articles/a/LSTM-Neural-Network-for-Time-Series-Prediction
 */
public class LSTMPredictor {

    @NotNull
    private final LiveSTM net;
    @NotNull
    private final DelayedFloats Ihistory;
    private final DelayedFloats Ohistory;
    //private final List<FloatSupplier> OUTSSprev;
    private final @NotNull FloatSupplier[] INS;
    private final @NotNull FloatSupplier[] OUTS;


    float learningRate = 0.01f;

    //boolean training = true;

    public LSTMPredictor(@NotNull FloatSupplier[] INS, int iHistory, @NotNull FloatSupplier[] OUTS, int oHistory) {

        int numInputs = INS.length;
        int numOutputs = OUTS.length;


        this.INS = INS;
        this.OUTS = OUTS;
        this.Ihistory = DelayedFloats.delay(INS, iHistory);
        this.Ohistory = DelayedFloats.delay(OUTS, oHistory);

        this.net = new LiveSTM(numInputs * iHistory, numOutputs * oHistory, numInputs * numOutputs * Math.max(iHistory, oHistory)) {

            //final Interaction i = Interaction.the(numInputs, numOutputs); //reused

            @NotNull
            @Override
            protected Interaction observe() {
                throw new UnsupportedOperationException();
//
//                /*if (i.actual!=null)
//                    System.out.print(Texts.n4(i.actual) + " -| ");*/
//
//                if (i.predicted!=null)
//                    System.out.print(Texts.n4(i.predicted) + " |- ");
//
//                i.zero(); //just to be sure, may not be necessary
//
//                i.forget = 0; // (learningRate/4f);
//
//                //load percept
//                for (int n = 0; n < numInputs; n++)
//                    i.actual[n] = INS.get(n).asFloat();
//
//                if (training) {
//
//                    for (int n = 0; n < numOutputs; n++)
//                        i.expected[n] = EXPECTS.get(n).asFloat();
//                } else {
//                    i.expected = null;
//                }
//
//                if (i.expected!=null)
//                    System.out.print(Texts.n4(i.expected) );
//
//                System.out.println();
//
//                return i;
            }
        };

    }

    public static class DelayedFloats extends FasterList<FloatDelay> {

        public DelayedFloats(int size) {
            super(size);
        }

        public void next() {
            forEach(FloatDelay::next);
        }

        @NotNull
        static DelayedFloats delay(@NotNull FloatSupplier[] vector, int history) {
            DelayedFloats delayed = new DelayedFloats(vector.length);
            for (FloatSupplier f : vector)
                delayed.add(new FloatDelay(f, history));
            return delayed;
        }
        public void print() {
            forEach(System.out::println);
        }
    }


    public double[] next() {
        Ihistory.next();
        //Ihistory.print();

        Ohistory.next();
        //Ohistory.print();

        //train on previous
        //net.agent.forget(0.002f);

        double[] q = net.agent.learn(dH(Ihistory,Ihistory.get(0).data.length), dH(Ohistory, Ohistory.get(0).data.length), learningRate);

        //System.out.println();

        //double[] p = net.agent.predict(d(INS)); //predict with current
        //System.out.println(n4(q) + " " + n4(p));

        return q;
    }

    @NotNull
    public static double[] d(@NotNull FloatSupplier[] f) {
        double[] d = new double[f.length];
        int i = 0;
        for (FloatSupplier g : f)
            d[i++] = g.asFloat();
        return d;
    }

    @NotNull
    public static double[] d(@NotNull Collection<? extends FloatSupplier> f) {
        double[] d = new double[f.size()];
        int i = 0;
        for (FloatSupplier g : f)
            d[i++] = g.asFloat();
        return d;
    }

    @NotNull
    static double[] dH(@NotNull Collection<? extends FloatDelay> f, int history) {
        double[] d = new double[f.size() * history];
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
        LSTMPredictor l = new LSTMPredictor(
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

