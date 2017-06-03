package jcog.pri.mix.control;

import jcog.Loop;
import jcog.learn.ql.HaiQAgent;
import jcog.math.FloatSupplier;
import jcog.pri.Priority;
import jcog.pri.mix.MixRouter;
import jcog.pri.mix.PSink;

import java.util.stream.Stream;

import static java.lang.System.arraycopy;
import static jcog.Util.*;

/**
 * record a finite history of mix parameters values
 * in order to assign credit
 * to historic classifications or something
 * which resembles a differentiable
 * RNN backprop
 */
public class RLMixControl<Y, X extends Priority> extends Loop {

    private final MixRouter<Y, X> mix;
    public final HaiQAgent agent;
    private final FloatSupplier score;
    private final float[] agentIn;

    /** values less than 1 eventually lowers channel volume levels to zero (flat, ie. x1)
     *  unless otherwise boosted or cut */
    float decay = 1f;

    public double[] levels; //first order only so far
    private double[] delta; //delta accumulated
    double controlSpeed = 0.3;


    public RLMixControl(MixRouter<Y, X> mix, float fps, FloatSupplier score) {
        super(fps);
        this.mix = mix;

        int outs = mix.size();

        int inputs = outs + outs; //+..
        this.agentIn = new float[inputs];
        agent = new HaiQAgent(inputs, inputs * outs * 2, outs * 2);
        agent.setQ(0.02f, 0.5f, 0.9f, 0.05f); // 0.1 0.5 0.9
        this.levels = new double[outs];
        this.delta = new double[outs];
        this.score = score;
    }

    @Override
    public boolean next() {

        int size = mix.size();
        if (size == 0)
            return true;

        if (levels == null)
            return true;

        //INPUT TO AGENT
        int s = levels.length;
        arraycopy(doubleToFloatArray(levels) /* HACK */, 0, agentIn,
                0, s);
        arraycopy(levelVolume() /* HACK */, 0, agentIn,
                s, s);


        int action = agent.act(score.asFloat(), agentIn);
        int which = action/2;
        if (action%2==0)
            delta[which] = -1;
        else
            delta[which] = +1;

        for (int i = 0; i < size; i++) {

            if (!Double.isFinite(levels[i]))
                levels[i] = 0;

            //gain in [-1, +1]
            double preGain = (levels[i] = Math.min(+1f, Math.max(-1, decay * levels[i] + delta[i] * controlSpeed)));
            float postGain =  sqr( (float) (1f+preGain)); //l^2
            mix.outs[i].setValue( postGain );
        }

        return true;
    }

    private float[] levelVolume() {
        float[] v = new float[levels.length];
        float total = 0;
        for (int i = 0, vLength = v.length; i < vLength; i++) {
            PSink m = this.mix.outs[i];
            double s = m.out.getSum();
            total += s;
            v[i] = (float) s;
            m.clear();
        }
        if (total > 0) {
            //normalize
            for (int i = 0, vLength = v.length; i < vLength; i++) {
                v[i] /= total;
            }
        }
        return v;
    }

//    /** returns a snapshot of the mixer levels, in an array directly corresponding to channel id */
//    public double[] levels() {
//        return Stream.of(mix.outs).mapToDouble(MutableFloat::floatValue).toArray();
//    }

    /**
     * score in range -1..+1
     */
    public void train(X x, float score) {
        double[] l = this.levels;
        if (l != null) {
            int[] eligible = eligibile(x);
            for (int i = 0; i < delta.length; i++) {
                if (eligible[i] > 0) {
                    delta[i] += score;
                }
            }
        }
    }

    private int[] eligibile(X x) {
        return Stream.of(mix.possibles).mapToInt(z -> z.test(x) ? 1 : 0).toArray();
    }

    public int size() {
        return mix.size();
    }
}
