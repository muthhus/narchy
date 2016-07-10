package nars.predict;

import com.google.common.collect.Lists;
import nars.learn.lstm.Interaction;
import nars.learn.lstm.test.LiveSTM;
import nars.util.math.DelayedFloat;
import nars.util.math.FloatSupplier;
import org.apache.commons.lang3.mutable.MutableFloat;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by me on 7/9/16.
 */
public class LSTMPredictor {

    private final LiveSTM net;
    private final List<FloatSupplier> INSprev;
    //private final List<FloatSupplier> OUTSSprev;
    private final List<FloatSupplier> INS;
    private final List<FloatSupplier> OUTS;
    boolean training = true;

    public LSTMPredictor(List<FloatSupplier> INS, List<FloatSupplier> OUTS /* used when training */) {

        int numInputs = INS.size();
        int numOutputs = OUTS.size();


        this.INS = INS;
        this.OUTS = OUTS;
        this.INSprev = delay(INS, 1);
        //this.OUTSSprev = delay(OUTS, 1);

        this.net = new LiveSTM(numInputs, numOutputs, numInputs * numOutputs) {

            final Interaction i = Interaction.the(numInputs, numOutputs); //reused

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

    public static List<FloatSupplier> delay(List<FloatSupplier> vector, int history) {
        List<FloatSupplier> delayed = new ArrayList(vector.size());
        for (int i = 0; i< vector.size(); i++)
            delayed.add( new DelayedFloat(vector.get(i), history) );
        return delayed;
    }

    public double[] next() {
        //train on previous
        net.agent.forget(0.002f);
        net.agent.learn(d(INSprev),d(OUTS), 0.1f);

        //predict with current
        double[] p = net.agent.predict(d(INS));
        return p;
    }

    public static double[] d(List<FloatSupplier> f) {
        double[] d = new double[f.size()];
        for (int i = 0; i < f.size(); i++)
            d[i] = f.get(i).asFloat();
        return d;
    }

    public static void main(String[] args) {
        MutableFloat m = new MutableFloat();

        LSTMPredictor l = new LSTMPredictor(
                Lists.newArrayList(
                        () -> m.floatValue() % 2,
                        () -> (m.floatValue() % 3) > 1 ?  1 : 0
                ),
                Lists.newArrayList(
                        () -> ((m.floatValue() % 2) + (m.floatValue() % 3)) > 2 ? 1 : 0
                )
        );

        for (int i= 0 ;i < 5000; i++) {
            m.increment();
            l.next();
        }

    }

}

