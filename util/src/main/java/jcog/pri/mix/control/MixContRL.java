package jcog.pri.mix.control;

import jcog.Loop;
import jcog.Util;
import jcog.data.FloatParam;
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
public class MixContRL<X extends Priority> extends Loop implements PSinks<X, CLink<X>>, FloatFunction<RoaringBitmap> {

    private final MixChannel[] mix;
    public final FloatParam priMin = new FloatParam(Pri.EPSILON, 0f, 1f);
    public final FloatParam gainMax = new FloatParam(4f, 0f, 16f);

    /** the active tests to apply to input (doesnt include aux's which will already have applied their id)  */
    private final AbstractClassifier<X>[] tests;

    /** should probably be calibrated in relation to the executioner's processing rate */
    private float activeTaskMomentum = 0.5f;

    public static class MixChannel {

        public final AtomicSummaryStatistics input = new AtomicSummaryStatistics();
        public final AtomicSummaryStatistics active = new AtomicSummaryStatistics();
        private String id;

        public MixChannel(String id) {
            this.id = id;
        }

        public void accept(float pri, boolean input, boolean active) {
            if (input)
                this.input.accept(pri);
            if (active)
                this.active.accept(pri);
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

    /**
     * dim >= size
     */
    public final int dim;

    final int maxAux;
    final List<PSink<X,CLink<X>>> aux = new FasterList();

    /**
     * values less than 1 eventually lowers channel volume levels to zero (flat, ie. x1)
     * unless otherwise boosted or cut
     */
    //float decay = 1f;

    //private final double[] delta; //delta accumulated
    //double controlSpeed = 0.05; //increments up/down per action
    public float lastScore;

    final int auxStart;


    public MixContRL(float fps, MixAgent agent, FloatSupplier score, int aux, AbstractClassifier<X>... tests) {
        super(fps);

        this.maxAux = aux;

        int dim = 0;
        for (AbstractClassifier t : tests)
            dim += t.dimension();
        this.auxStart = dim;

        AbstractClassifier[] aa = new AbstractClassifier[aux];
        for (int a = 0; a < aux; a++) {
            aa[a] = new BooleanClassifier("ax" + a, (x) -> false);
            dim++;
        }
        this.tests = tests;
        tests = ArrayUtils.addAll(tests, aa);

        assert(dim >= tests.length);
        this.dim = dim;



        int j = 0;
        this.mix = new MixChannel[dim];
        for (AbstractClassifier t : tests) {
            int n = t.dimension();
            for (int i = 0; i < n; i++) {
                mix[j++] = new MixChannel(t.name(i));
            }
        }
        for (; j < dim; ) {
            mix[j++] = new MixChannel("aux" + "_" +j);
        }


        /** level values. between 0 and 1: 0 = max cut, 1 = max boost, 0.5 = x1 */
        this.mixControl = new ArrayTensor(this.dim);

        this.nextInput = new ArrayTensor(this.dim);
        this.nextActive = new ArrayTensor(this.dim);
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
                        mixControl
                )
        //      , 2)
        //,12)
        ;


        this.agent = agent;

        //this.delta = new double[outs];
        this.score = score;
    }

    public CLink<X> test(X x) {
        return test(new CLink<X>(x));
    }

    public CLink<X> test(CLink<X> x) {
        int t = 0;


        for (AbstractClassifier c : tests) {
            c.classify(x.ref, x, t);
            t += c.dimension();
        }

        //record input
        float p = x.priElseZero();
        if (p > 0)
            x.forEach((int i) -> mix[i].accept(p, true, false));

        return x;
    }


    public float gain(int dimension) {
        return 2f * (mixControl.get(dimension) - 0.5f); //bipolarize
    }

    /** computes the gain, and records the (pre-amplified) traffic */
    public float gain(CLink<X> x) {
        float p = x.priElseZero();
        if (p > priMin.floatValue()) {
            x.forEach((int i) -> {
                mix[i].accept(p, false, true);
            });
            return Math.min(gainMax.floatValue(), floatValueOf(x));
        } else {
            return 0;
        }

        //TODO record the post traffic?

    }

    @Override
    public float floatValueOf(RoaringBitmap t) {
        final float[] preGain = {0};
        t.forEach((int i) -> {
            preGain[0] += gain(i);
        });

        //preGain[0] += levels.get(size - 1); //bias

        return sqr( //l^4
                sqr(1f + preGain[0]) //l^2
        )
                ;
    }

    @Override
    public boolean next() {

        //HACK
        if (mixControl == null || score == null || agentIn == null) return true;

        agent.act(agentIn, this.lastScore = score.asFloat(), mixControl);

        updateTraffic();

        return true;
    }

    private void updateTraffic() {
        float totalInput = 0, totalActive = 0;
        float[] nextInput = this.nextInput.data;
        float[] nextActive = this.nextActive.data;
        float[] prevActive = this.nextActive.data.clone();
        for (int i = 0; i < dim; i++) {
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
        for (int i = 0; i < dim; i++) {
            boolean inputEmpty;
            if (nextInput[i] < Pri.EPSILON) {
                inputEmpty = true;
                nextInput[i] = 0;
            } else {
                inputEmpty = false;
                nextInput[i] /= total;
            }
//            if (nextActive[i] < Pri.EPSILON) {
////                if (inputEmpty) {
////                    //set the gain for this knob to neutral, so it doesnt need to learn that whtever particular setting exists had any effect
////                    mixControl.set(0.5f, i);
////                    nextActive[i] = 0;
////                }
//            } else {
                //TODO lerp the activation in proportion to the executor's rate, to ammortize the actual loss rather than just reset each cycle
            float a = total >= Pri.EPSILON ? nextActive[i] / total : 0f;
            nextActive[i] = Util.lerp((1f-activeTaskMomentum), prevActive[i], a);;
            //}
        }

    }

    public String summary() {
        return IntStream.range(0, dim).mapToObj(i -> id(i) + " " + n4(trafficInput(i)) + "->" + n4(trafficActive(i))).collect(Collectors.joining(", "));
    }


    @Override
    public PSink<X,CLink<X>> newStream(Object x, Consumer<CLink<X>> each) {
        synchronized (aux) {

            int aux = this.aux.size();
            if (aux >= maxAux)
                throw new RuntimeException("no more sinks available");

            //TODO return a previously created sink with exact name, using Map

            int id = auxStart + aux;
            MixChannel mm = this.mix[id];
            mm.id = x.toString();

            PSink<X,CLink<X>> p = new PSink<X,CLink<X>>(x, xx -> new CLink(xx, id), each);


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
