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
 */
public class LSTMPredictor {

    @NotNull
    private final LiveSTM net;
    @NotNull
    private final DelayedFloats INhistoric;
    //private final List<FloatSupplier> OUTSSprev;
    private final @NotNull FloatSupplier[] INS;
    private final @NotNull FloatSupplier[] OUTS;

    /** history length */
    private final int history;

    float learningRate = 0.05f;

    //boolean training = true;

    public LSTMPredictor(@NotNull FloatSupplier[] INS, @NotNull FloatSupplier[] OUTS, int history  /* used when training */) {

        int numInputs = INS.length;
        int numOutputs = OUTS.length;


        this.INS = INS;
        this.OUTS = OUTS;
        this.INhistoric = delay(INS, history);
        this.history = history;

        this.net = new LiveSTM(numInputs*history, numOutputs, numInputs * numOutputs) {

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
    }


    @NotNull
    public static DelayedFloats delay(@NotNull FloatSupplier[] vector, int history) {
        DelayedFloats delayed = new DelayedFloats(vector.length);
        for (FloatSupplier f : vector)
            delayed.add( new FloatDelay(f, history) );
        return delayed;
    }

    public double[] next() {
        INhistoric.next();

        //train on previous
        //net.agent.forget(0.002f);

        double[] q = net.agent.learn(dH(INhistoric, history),d(OUTS), learningRate);

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
                () -> 1f * (m.floatValue() % 2),
                () -> 1f * ((m.floatValue() % 3) > 1 ? 1 : 0)
        };
        FloatSupplier[] out = {
                () -> 1f * (((m.floatValue() % 2) + (m.floatValue() % 3)) > 2 ? 1 : 0)
        };
        LSTMPredictor l = new LSTMPredictor(
                in,
                out,
                5
        );

        double[] lastPredict = ArrayUtils.EMPTY_DOUBLE_ARRAY;
        for (int i= 0 ;i < 500; i++) {
            m.increment();
            System.out.println(n4(d(in)) + "\t\t" + n4(d(out)) + "\t=?=\t" + n4(lastPredict));
            double[] prediction = l.next();
            lastPredict = prediction;
        }

    }

}

