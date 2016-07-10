package nars.predict;

import com.google.common.collect.Lists;
import nars.learn.lstm.Interaction;
import nars.learn.lstm.test.LiveSTM;
import nars.util.Texts;
import nars.util.math.FloatSupplier;
import org.apache.commons.lang3.mutable.MutableFloat;

import java.util.List;

/**
 * Created by me on 7/9/16.
 */
public class LSTMPredictor {

    private final LiveSTM net;
    boolean training = true;

    public LSTMPredictor(List<FloatSupplier> INS, List<FloatSupplier> EXPECTS /* used when training */) {

        int numInputs = INS.size();
        int numOutputs = EXPECTS.size();

        float learningRate = 0.1f;

        this.net = new LiveSTM(numInputs, numOutputs, numInputs * numOutputs, learningRate) {

            final Interaction i = Interaction.the(numInputs, numOutputs); //reused

            @Override
            protected Interaction observe() {

                if (i.actual!=null)
                    System.out.print(Texts.n4(i.actual) + " -| ");

                if (i.predicted!=null)
                    System.out.print(Texts.n4(i.predicted) + " |- ");

                i.zero(); //just to be sure, may not be necessary

                i.forget = 0; // (learningRate/4f);

                //load percept
                for (int n = 0; n < numInputs; n++)
                    i.actual[n] = INS.get(n).asFloat();

                if (training) {
                    for (int n = 0; n < numOutputs; n++)
                        i.expected[n] = EXPECTS.get(n).asFloat();
                } else {

                }

                if (i.expected!=null)
                    System.out.print(Texts.n4(i.expected) );

                System.out.println();

                return i;
            }
        };

    }

    public void next() {
        net.next();
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
