package jcog.pri.mix.control;

import jcog.Loop;
import jcog.list.FasterList;
import jcog.math.AtomicSummaryStatistics;
import jcog.math.FloatSupplier;
import jcog.pri.Pri;
import jcog.pri.Priority;
import jcog.pri.classify.AbstractClassifier;
import jcog.pri.classify.BooleanClassifier;
import jcog.pri.mix.PSink;
import jcog.pri.mix.PSinks;
import jcog.tensor.ArrayTensor;
import jcog.tensor.Tensor;
import jcog.tensor.TensorChain;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.roaringbitmap.RoaringBitmap;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static jcog.Texts.n4;
import static jcog.Util.sqr;

/**
 * intelligent stream filter
 * it can be trained with a reinforcement learning agent for any goal
 * it classifies each input in an array of configured classifications
 * each classification has a gain level
 * if it matches an input then then its gain will be applied in combination with all other matching gain values
 * in -1..+1 range
 * so they can override each other
 * then this value is shifted and squared to get the final multiplier amount
 * <p>
 * record a finite history of mix parameters values
 * in order to assign credit
 * to historic classifications or something
 * which resembles a differentiable
 * RNN backprop
 */
public class MixContRL<Y extends Priority> extends Loop implements PSinks<Y,CLink<Y>>, FloatFunction<RoaringBitmap> {

    private final MixChannel<Y>[] mix;

    public static class MixChannel<Y extends Priority> {
        public final AbstractClassifier<Y> test;
        public final AtomicSummaryStatistics input = new AtomicSummaryStatistics();
        public final AtomicSummaryStatistics active = new AtomicSummaryStatistics();
        private String id;

        public MixChannel(String id, AbstractClassifier<Y> test) {
            this.id = id;
            this.test = test;
        }
    }


    public final MixAgent agent;


    public final FloatSupplier score;

    /**
     * range 0..1.0, 0.5 is middle (1x)
     */
    public final ArrayTensor mixControl;

    public final ArrayTensor nextInput, nextActive, active, input;

    /**
     * unipolar vector, 0..1.0
     */
    public final Tensor agentIn;

    public final int size;


    final int maxAux;
    final List<PSink<Y>> aux = new FasterList();

    /**
     * values less than 1 eventually lowers channel volume levels to zero (flat, ie. x1)
     * unless otherwise boosted or cut
     */
    //float decay = 1f;

    //private final double[] delta; //delta accumulated
    //double controlSpeed = 0.05; //increments up/down per action
    public float lastScore;

    final int auxStart;


    public MixContRL(float fps, MixAgent agent, FloatSupplier score, int aux, AbstractClassifier<Y>... tests) {
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
        dim += aux;
        tests = ArrayUtils.addAll(tests, aa);


        this.size = dim;
        String[] names = new String[dim];

        int j = 0;
        for (AbstractClassifier t : tests) {
            int n = t.dimension();
            for (int i = 0; i < n; i++) {
                names[j++] = t.name(i);
            }
        }
        for (; j < size; j++)
            names[j] = "aux" + j;

        this.mix = new MixChannel[size];
        for (int i = 0; i < size; i++)
            mix[i] = new MixChannel<>(names[i], tests[i]);

        /** level values. between 0 and 1: 0 = max cut, 1 = max boost, 0.5 = x1 */
        this.mixControl = new ArrayTensor(size);

        this.nextInput = new ArrayTensor(size);
        this.nextActive = new ArrayTensor(size);
        this.mixControl.fill(0.5f);
        this.nextInput.fill(0f);
        this.nextActive.fill(0f);

        this.agentIn =
                //new AutoTensor(
                //RingBufferTensor.get(
                TensorChain.get(
                        this.input = nextInput,
                        this.active = nextActive,
                        //this.traffic = new TensorLERP(rawTraffic, 0.75f), //sum is normalized to 1
                        mixControl.scale(1f / (size / 2f))
                )
        //      , 2)
        //,12)
        ;


        this.agent = agent;

        //this.delta = new double[outs];
        this.score = score;
    }

    public CLink<Y> test(Y x) {
        return test(new CLink<Y>(x));
    }

    public CLink<Y> test(CLink<Y> x) {
        int t = 0;
        for (int i = 0, outsLength = size; i < outsLength; i++) {
            AbstractClassifier<Y> c = mix[i].test;
            c.classify(x.ref, x, t);
            t += c.dimension();
        }
        return x;
    }

    public float gain(int i) {
        return 2f * (mixControl.get(i) - 0.5f); //bipolarize
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
        if (size == 0 || mixControl == null || score == null || agentIn == null) return true;

        agent.act(agentIn, this.lastScore = score.asFloat(), mixControl);

        updateTraffic();

        return true;
    }

    private void updateTraffic() {
        float totalInput = 0, totalActive = 0;
        float[] nextInput = this.nextInput.data;
        float[] nextActive = this.nextActive.data;
        for (int i = 0, vLength = size; i < vLength; i++) {
            MixChannel mm = this.mix[i];
            float ii = mm.input.sumThenClear();
            float aa = mm.active.sumThenClear();
            nextInput[i] = ii;
            totalInput += ii;
            nextActive[i] = aa;
            totalActive += aa;
        }
        float total = totalInput + totalActive;
        //normalize
        for (int i = 0, vLength = nextInput.length; i < vLength; i++) {
            boolean inputEmpty;
            if (nextInput[i] < Pri.EPSILON) {
                inputEmpty = true;
                nextInput[i] = 0;
            } else {
                inputEmpty = false;
                nextInput[i] /= total;
            }
            if (nextActive[i] < Pri.EPSILON) {
                if (inputEmpty) {
                    //set the gain for this knob to minimum, so it doesnt need to learn that whtever particular setting exists had any effect
                    mixControl.set(0f, i);
                    nextActive[i] = 0;
                } else {
                    nextActive[i] /= total;
                }
            }
        }

    }

    public String summary() {
        return IntStream.range(0, size).mapToObj(i -> id(i) + " " + n4(trafficInput(i))+"->"+n4(trafficActive(i))).collect(Collectors.joining(", "));
    }


    public int size() {
        return size;
    }

    @Override public PSink<Y> newStream(Object x, Consumer<CLink<Y>> each) {
        synchronized (aux) {

            int aux = this.aux.size();
            if (aux >= maxAux)
                throw new RuntimeException("no more sinks available");

            //TODO return a previously created sink with exact name, using Map

            int id = auxStart + aux;
            mix[id].id = x.toString();

            PSink<Y> p = new PSink<>(x, (y) -> each.accept(new CLink(y, id)));

            this.aux.add(p);

            return p;
        }
    }

    public String id(int i) {
        return mix[i].id;
    }

    /**
     * value in 0..1.0, percentage of traffic this channel contributes
     */
    public double trafficInput(int i) {
        return input.get(i);
    }
    public double trafficActive(int i) {
        return active.get(i);
    }


}
