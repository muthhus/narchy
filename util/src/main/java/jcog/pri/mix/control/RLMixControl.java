package jcog.pri.mix.control;

import jcog.Loop;
import jcog.Util;
import jcog.learn.ql.HaiQAgent;
import jcog.list.FasterList;
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
    private final float[] nextTraffic;
    private final MixAgent agent;


    public final ArrayTensor preAgentIn;

    public final FloatSupplier score;

    public final ArrayTensor agentOut;
    public final ArrayTensor traffic;

    public final BufferedTensor agentIn;
    public final int size;

    /**
     * values less than 1 eventually lowers channel volume levels to zero (flat, ie. x1)
     * unless otherwise boosted or cut
     */
    //float decay = 1f;

    //private final double[] delta; //delta accumulated
    //double controlSpeed = 0.05; //increments up/down per action
    public float lastScore;

    final int auxStart;

    final String[] names;

    public RLMixControl(Consumer<Y> target, float fps, MixAgent agent, FloatSupplier score, int aux, AbstractClassifier<Y, X>... tests) {
        super(fps);

        this.maxAux = aux;

        int dim = 0;
        for (AbstractClassifier t : tests)
            dim += t.dimension();
        this.auxStart = dim;

        AbstractClassifier[] aa = new AbstractClassifier[aux];
        for (int a = 0; a < aux; a++) {
            aa[a] = new BooleanClassifier("ax" + a, (x) -> false);
        }
        tests = ArrayUtils.addAll(tests, aa);

        this.mix = new MixRouter<>(target, this, tests);

        this.size = mix.size();
        this.names = new String[size];

        int j = 0;
        for (AbstractClassifier t : tests) {
            int n = t.dimension();
            for (int i = 0; i < n; i++) {
                names[j++] = t.name(i);
            }
        }
        for (; j < names.length; j++)
            names[j] = "aux" + j;


        /** level values */
        this.agentOut = new ArrayTensor(size);
        this.nextTraffic = new float[size];


        this.agentIn = new BufferedTensor(
                //new AutoTensor(
                this.preAgentIn = new RingBufferTensor(

                                this.traffic = new ArrayTensor(size)
                        , 1)
                //,12)
        );


        this.agent = agent;

        //this.delta = new double[outs];
        this.score = score;
    }

    public void accept(Y x) {
        mix.accept(x);
    }

    public float gain(int i) {
        return 2f * (agentOut.get(i) - 0.5f); //bipolarize
    }

    @Override
    public float floatValueOf(RoaringBitmap t) {
        final float[] preGain = {0};
        t.forEach((int i) -> {
            preGain[0] += gain(i);
        });

        //preGain[0] += levels.get(size - 1); //bias

        return //sqr( //l^4
                sqr(1f + preGain[0]) //l^2
                //)
                ;
    }

    @Override
    public boolean next() {

        //HACK
        if (size == 0 || agentOut == null || score == null || agentIn == null) return true;

        updateTraffic();

        agent.act(agentIn, this.lastScore = score.asFloat(), agentOut);


        return true;
    }

    private void updateTraffic() {
        float total = 0;
        for (int i = 0, vLength = size; i < vLength; i++) {
            float s = mix.traffic[i].sumThenClear();
            nextTraffic[i] = s;
            total += s;
        }
        if (total > 0) {
            //normalize
            for (int i = 0, vLength = nextTraffic.length; i < vLength; i++) {
                nextTraffic[i] /= total;
            }
        }
        traffic.set(nextTraffic);
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

            int id = auxStart + aux;
            names[id] = x.toString();

            PSink<X, Y> p = new PSink<X, Y>(x, (y) -> {


                RoaringBitmap c = mix.classify(y);

                c.add(id); //attach stream-specific classification

                float yp = y.priElseZero();
                if (yp > 0) {
                    RLMixControl.this.aux.get(aux).in.accept(yp /* input value before multiplying */);
                    float g = mix.gain.floatValueOf(c);
                    if (g > 0) {
                        if (yp > 0) {
                            y.priMult(g);
                            mix.target.accept(y);
                        }
                    }
                }
            });

            this.aux.add(p);

            return p;
        }
    }

    public String name(int i) {
        return names[i];
    }
}
