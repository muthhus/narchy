package jcog.pri.mix.control;

import jcog.Loop;
import jcog.Util;
import jcog.learn.ql.CMAESAgent;
import jcog.learn.ql.HaiQAgent;
import jcog.list.FasterList;
import jcog.math.AtomicSummaryStatistics;
import jcog.math.FloatSupplier;
import jcog.pri.Priority;
import jcog.pri.classify.AbstractClassifier;
import jcog.pri.classify.BooleanClassifier;
import jcog.pri.mix.MixRouter;
import jcog.pri.mix.PSink;
import jcog.pri.mix.PSinks;
import jcog.tensor.ArrayTensor;
import jcog.tensor.BufferedTensor;
import jcog.tensor.RingBufferTensor;
import jcog.tensor.TensorChain;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.roaringbitmap.RoaringBitmap;

import java.util.List;
import java.util.function.Consumer;

import static jcog.Util.floatToDoubleArray;
import static jcog.Util.sqr;

/**
 * record a finite history of mix parameters values
 * in order to assign credit
 * to historic classifications or something
 * which resembles a differentiable
 * RNN backprop
 */
public class RLMixControl<X, Y extends Priority> extends Loop implements PSinks<X, Y>, FloatFunction<RoaringBitmap> {

    private final MixRouter<X, Y> mix;

    //public final HaiQAgent agent;
    CMAESAgent agent;

    public final FloatSupplier score;

    public final ArrayTensor levels;
    public final ArrayTensor traffic;

    public final BufferedTensor agentIn;
    private final int size;

    /**
     * values less than 1 eventually lowers channel volume levels to zero (flat, ie. x1)
     * unless otherwise boosted or cut
     */
    float decay = 1f;

    private final double[] delta; //delta accumulated
    double controlSpeed = 0.05; //increments up/down per action
    public float lastScore;

    final int auxStart;

    public RLMixControl(Consumer<Y> target, float fps, FloatSupplier score, int aux, AbstractClassifier<Y, X>... tests) {
        super(fps);

        this.maxAux = aux;

        int dim = 0;
        for (AbstractClassifier t : tests)
            dim += t.dimension();
        this.auxStart = dim;

        AbstractClassifier[] aa = new AbstractClassifier[aux];
        for (int a = 0; a < aux; a++) {
            aa[a] = new BooleanClassifier("ax"+a, (x)->false);
        }
        tests = ArrayUtils.addAll(tests, aa);

        this.mix = new MixRouter<X, Y>(target, this, tests);

        this.size = mix.size();

        int outs = size + 1 /* bias */;

        this.agentIn = new BufferedTensor(new RingBufferTensor(
                new TensorChain(
                        this.levels = new ArrayTensor(size),
                        this.traffic = new ArrayTensor(size)
                ), 1));

        int numInputs = agentIn.volume();

        //agent = new HaiQAgent(numInputs, size*4, outs * 2);
        //agent.setQ(0.05f, 0.5f, 0.9f); // 0.1 0.5 0.9

        agent = CMAESAgent.build(numInputs, size*2 /* plus and minus for each */, (x) -> score.asFloat());

        this.delta = new double[outs];
        this.score = score;
    }

    public void accept(Y x) {
        mix.accept(x);
    }

    @Override
    public float floatValueOf(RoaringBitmap t) {
        final float[] preGain = {0};
        t.forEach((int i) -> {
            preGain[0] += levels.get(i);
        });

        preGain[0] += levels.get(size - 1); //bias

        return sqr(1f + 2 * (-0.5f + Math.max(0, Math.min(1, preGain[0])))); //l^2
    }

    @Override
    public boolean next() {

        //HACK
        if (size == 0 || levels == null || agent == null || score == null || agentIn == null) return true;

        updateTraffic();


        int action = agent.act(this.lastScore = score.asFloat(), floatToDoubleArray(agentIn.get()) );
        if (action == -1)
            return true; //error

        int which = action / 2;
        if (action % 2 == 0)
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

        //commit aux's
        for (int i = 0, auxSize = aux.size(); i < auxSize; i++) {
            traffic.set(aux.get(i).out.sumThenClear(), auxStart + i);
        }

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


    final int maxAux;
    final List<PSink<X, Y>> aux = new FasterList();

    @Override
    public PSink<X, Y> stream(X x) {
        synchronized (aux) {

            int aux = this.aux.size();
            if (aux >= maxAux)
                throw new RuntimeException("no more sinks available");

            //TODO return a previously created sink with exact name, using Map

            PSink<X, Y> p = new PSink<X, Y>(x, (y) -> {


                RoaringBitmap c = mix.classify(y);

                c.add(auxStart + aux); //attach stream-specific classification

                float g = mix.gain.floatValueOf(c);

                if (g > 0) {
                    y.priMult(g);
                    RLMixControl.this.aux.get(aux).out.accept(y.priElseZero());
                    mix.target.accept(y);
                }
            });
            this.aux.add(p);
            return p;
        }
    }
}
