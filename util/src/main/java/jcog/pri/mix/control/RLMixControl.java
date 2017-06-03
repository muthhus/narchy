package jcog.pri.mix.control;

import jcog.Loop;
import jcog.learn.ql.HaiQAgent;
import jcog.math.AtomicSummaryStatistics;
import jcog.math.FloatSupplier;
import jcog.pri.Priority;
import jcog.pri.mix.MixRouter;
import jcog.pri.mix.PSink;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.roaringbitmap.RoaringBitmap;

import java.util.function.Consumer;
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
public class RLMixControl<X, Y extends Priority> extends Loop implements FloatFunction<RoaringBitmap> {

    private final MixRouter<X, Y> mix;
    public final HaiQAgent agent;
    public final FloatSupplier score;
    public final float[] agentIn;

    /** values less than 1 eventually lowers channel volume levels to zero (flat, ie. x1)
     *  unless otherwise boosted or cut */
    float decay = 1f;

    public double[] levels; //first order only so far


    private final double[] delta; //delta accumulated
    double controlSpeed = 0.1; //increments up/down per action
    public float lastScore;

    public RLMixControl(Consumer<Y> target, float fps, FloatSupplier score, MixRouter.Classifier<Y,X>... tests) {
        super(fps);

        this.mix = new MixRouter<X,Y>(target, this, tests);

        int outs = mix.size() + 1 /* bias */;

        int inputs = outs + outs; //+..
        this.agentIn = new float[inputs];
        agent = new HaiQAgent(inputs, inputs / 2, outs * 2);
        agent.setQ(0.05f, 0.5f, 0.9f, 0.01f); // 0.1 0.5 0.9
        this.levels = new double[outs];
        this.delta = new double[outs];
        this.score = score;
    }

    public void accept(Y x) {
        mix.accept(x);
    }

    @Override
    public float floatValueOf(RoaringBitmap t) {
        final float[] preGain = {0};
        t.forEach((int i)->{
            preGain[0] += levels[i];
        });

        preGain[0] += levels[levels.length-1]; //bias

        return (sqr(1f + Math.max(-1, Math.min(+1, preGain[0]) ) )); //l^2
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
        arraycopy( /* HACK */ doubleToFloatArray(levels, 0, levels.length,
            x->(float)((x+1)/2) //bipolar to unipolar conversion
        ), 0, agentIn,
                0, s);
        arraycopy(levelVolume() /* HACK */, 0, agentIn,
                s, s);


        int action = agent.act(this.lastScore = score.asFloat(), agentIn);

        int which = action/2;
        if (action%2==0)
            delta[which] = -1;
        else
            delta[which] = +1;

        for (int i = 0; i < size; i++) {

            if (!Double.isFinite(levels[i]))
                levels[i] = 0;

            //gain in [-1, +1]
            levels[i] = Math.min(+1f, Math.max(-1, decay * levels[i] + delta[i] * controlSpeed));

        }

        return true;
    }

    private float[] levelVolume() {
        float[] v = new float[levels.length];
        float total = 0;
        for (int i = 0, vLength = size(); i < vLength; i++) {
            AtomicSummaryStatistics m = mix.tests[i].in;
            double s = m.getSum();
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

//    /**
//     * score in range -1..+1
//     */
//    public void train(X x, float score) {
//        double[] l = this.levels;
//        if (l != null) {
//            int[] eligible = eligibile(x);
//            for (int i = 0; i < delta.length; i++) {
//                if (eligible[i] > 0) {
//                    delta[i] += score;
//                }
//            }
//        }
//    }

//    private int[] eligibile(X x) {
//        return Stream.of(mix.tests).mapToInt(z -> z.test(x) ? 1 : 0).toArray();
//    }

    public int size() {
        return mix.size();
    }


}
