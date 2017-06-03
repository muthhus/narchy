package jcog.pri.mix.control;

import jcog.Loop;
import jcog.learn.ql.HaiQAgent;
import jcog.math.AtomicSummaryStatistics;
import jcog.math.FloatSupplier;
import jcog.pri.Priority;
import jcog.pri.classify.AbstractClassifier;
import jcog.pri.classify.BooleanClassifier;
import jcog.pri.mix.MixRouter;
import jcog.tensor.*;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.roaringbitmap.RoaringBitmap;

import java.util.function.Consumer;

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

    public final ArrayTensor levels;
    public final ArrayTensor traffic;

    public final BufferedTensor agentIn;
    private final int size;

    /** values less than 1 eventually lowers channel volume levels to zero (flat, ie. x1)
     *  unless otherwise boosted or cut */
    float decay = 1f;

    private final double[] delta; //delta accumulated
    double controlSpeed = 0.1; //increments up/down per action
    public float lastScore;

    public RLMixControl(Consumer<Y> target, float fps, FloatSupplier score, AbstractClassifier<Y,X>... tests) {
        super(fps);

        this.mix = new MixRouter<X,Y>(target, this, tests);

        this.size = mix.size();
        int outs = size + 1 /* bias */;

        this.levels = new ArrayTensor(size);
        this.traffic = new ArrayTensor(size);
        this.agentIn = new BufferedTensor(new RingBufferTensor(
                new TensorChain(levels, traffic),4) );

        int numInputs = agentIn.volume();
        agent = new HaiQAgent(numInputs, numInputs / 2, outs * 2);
        agent.setQ(0.05f, 0.5f, 0.9f, 0.01f); // 0.1 0.5 0.9
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
            preGain[0] += levels.get(i);
        });

        preGain[0] += levels.get(size-1); //bias

        return sqr(1f + 2 * (-0.5f + Math.max(0, Math.min(1, preGain[0]) ))); //l^2
    }

    @Override
    public boolean next() {

        if (size == 0)
            return true;

        if (levels == null)
            return true;


        updateTraffic();


        int action = agent.act(this.lastScore = score.asFloat(), agentIn.get());

        int which = action/2;
        if (action%2==0)
            delta[which] = -1;
        else
            delta[which] = +1;

        for (int i = 0; i < size; i++) {

            //level prefer/reject in [-1, +1]
            float next;
            float prev = levels.get(i);
            if (!Double.isFinite(prev))
                next = 0;
            else {
                next = (float) Math.min(+1f, Math.max(0, decay * prev + delta[i] * controlSpeed));
            }

            levels.set(next, i);

        }

        return true;
    }

    private void updateTraffic() {
        float[] v = new float[size];
        float total = 0;
        for (int i = 0, vLength = size; i < vLength; i++) {
            AtomicSummaryStatistics m = mix.traffic[i];
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
        traffic.set(v);
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
        return size;
    }


}
