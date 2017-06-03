package jcog.pri.mix.control;

import com.google.common.primitives.Doubles;
import jcog.Loop;
import jcog.Util;
import jcog.learn.lstm.test.LiveSTM;
import jcog.learn.ql.HaiQAgent;
import jcog.math.FloatSupplier;
import jcog.pri.Priority;
import jcog.pri.mix.MixRouter;
import jcog.pri.mix.MixSwitch;
import org.apache.commons.lang3.mutable.MutableFloat;

import java.util.stream.Stream;

/** record a finite history of mix parameters values
 *  in order to assign credit
 *  to historic classifications or something
 *  which resembles a differentiable
 *  RNN backprop
 */
public class RLMixControl<Y, X extends Priority> extends Loop {

    private final MixRouter<Y,X> mix;
    public final HaiQAgent agent;
    private final FloatSupplier score;

    public double[] levels; //first order only so far
    private double[] delta; //delta accumulated
    double learningRate = 0.1;

    public RLMixControl(MixRouter<Y,X> mix, float fps, FloatSupplier score) {
        super(fps);
        this.mix = mix;


        agent = new HaiQAgent(mix.size(), mix.size()*mix.size()*2, mix.size()*2);
        agent.setQ(0.05f, 0.9f, 0.9f, 0.02f); // 0.1 0.5 0.9
        this.levels = new double[mix.size()];
        this.delta = new double[mix.size()];
        this.score = score;

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public boolean next() {

        int size = mix.size();
        if (size == 0)
            return true;

            int which = agent.act(score.asFloat(), Util.doubleToFloatArray(levels));
            if (which > size - 1) {
                delta[which - size] = -1;
            } else {
                delta[which] = +1;
            }

            for (int i = 0; i < size; i++) {
                double d = delta[i];
                if (d != 0) {
                    mix.outs[i].setValue(
                        (levels[i] = Math.min(4f,Math.max(0, levels[i] + d * learningRate)))
                    );
                }
            }

        return true;
    }

//    /** returns a snapshot of the mixer levels, in an array directly corresponding to channel id */
//    public double[] levels() {
//        return Stream.of(mix.outs).mapToDouble(MutableFloat::floatValue).toArray();
//    }

    /** score in range -1..+1 */
    public void train(X x, float score) {
        double[] l = this.levels;
        if (l!=null) {
            int[] eligible = eligibile(x);
            for (int i = 0; i < delta.length; i++) {
                if (eligible[i]>0) {
                    delta[i] += score;
                }
            }
        }
    }

    private int[] eligibile(X x) {
        return Stream.of(mix.possibles).mapToInt(z -> z.test(x) ? 1 :  0).toArray();
    }

    public int size() {
        return mix.size();
    }
}
