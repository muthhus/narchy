package jcog.pri.mix.control;

import jcog.Loop;
import jcog.Util;
import jcog.list.FasterList;
import jcog.math.FloatSupplier;
import jcog.pri.Priority;
import jcog.pri.classify.AbstractClassifier;
import jcog.pri.classify.BooleanClassifier;
import jcog.pri.mix.MixRouter;
import jcog.pri.mix.PSink;
import jcog.pri.mix.PSinks;
import jcog.tensor.ArrayTensor;
import jcog.tensor.Tensor;
import jcog.tensor.TensorChain;
import jcog.tensor.TensorLERP;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.roaringbitmap.RoaringBitmap;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static jcog.Texts.n4;
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



    public final MixAgent agent;


    public final FloatSupplier score;

    /** range 0..1.0, 0.5 is middle (1x) */
    public final ArrayTensor agentOut;

    public final ArrayTensor rawTraffic, traffic;

    /** unipolar vector, 0..1.0 */
    public final Tensor agentIn;

    public final int size;



    final int maxAux;
    final List<PSink<X, Y>> aux = new FasterList();

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


        /** level values. between 0 and 1: 0 = max cut, 1 = max boost, 0.5 = x1 */
        this.agentOut = new ArrayTensor(size);

        this.rawTraffic = new ArrayTensor(size);
        this.rawTraffic.fill(0.5f);

        this.agentIn =
                //new AutoTensor(
                //RingBufferTensor.get(
                            TensorChain.get(
                                this.traffic = rawTraffic,
                                //this.traffic = new TensorLERP(rawTraffic, 0.75f), //sum is normalized to 1
                                agentOut.scale(1f/(size/2f))
                            )
                  //      , 2)
                //,12)
        ;


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

        return /*sqr*/( //l^4
                sqr(1f + preGain[0]) //l^2
                )
                ;
    }

    @Override
    public boolean next() {

        //HACK
        if (size == 0 || agentOut == null || score == null || agentIn == null) return true;

        agent.act(agentIn, this.lastScore = score.asFloat(), agentOut);

        updateTraffic();

        return true;
    }

    private void updateTraffic() {
        float total = 0;
        float[] nextTraffic = rawTraffic.data;
        for (int i = 0, vLength = size; i < vLength; i++) {
            float s = mix.traffic[i].sumThenClear();
            nextTraffic[i] = s;
            total += s;
        }
        if (total > 0) {
            //normalize
            for (int i = 0, vLength = nextTraffic.length; i < vLength; i++) {
                if (nextTraffic[i]==0) {
                    agentOut.set(0f, i); //set the gain for this knob to minimum, so it doesnt need to learn that whtever particular setting exists had any effect
                    nextTraffic[i] = 0;
                } else {
                    nextTraffic[i] /= total;
                }
            }
        } else {
            Arrays.fill(nextTraffic, 0);
        }

        //System.out.println(summary());
    }

    public String summary() {
        return IntStream.range(0, size).mapToObj(i -> name(i) + " " + n4(traffic(i)) ).collect(Collectors.joining(", "));
    }


    public int size() {
        return size;
    }

    @Override
    public PSink<X, Y> stream(X x) {
        synchronized (aux) {

            int aux = this.aux.size();
            if (aux >= maxAux)
                throw new RuntimeException("no more sinks available");

            //TODO return a previously created sink with exact name, using Map

            int id = auxStart + aux;
            names[id] = x.toString();

            PSink<X, Y> p = new PSink<>(x, (y) -> mix.accept(y, id));

            this.aux.add(p);

            return p;
        }
    }

    public String name(int i) {
        return names[i];
    }

    /** value in 0..1.0, percentage of traffic this channel contributes */
    public double traffic(int i) {
        return traffic.get(i);
    }

}
