//package jcog.pri.mix;
//
//import jcog.math.AtomicSummaryStatistics;
//import jcog.pri.Priority;
//import jcog.pri.classify.AbstractClassifier;
//import org.apache.commons.lang3.ArrayUtils;
//import org.eclipse.collections.api.block.function.primitive.FloatFunction;
//import org.jetbrains.annotations.NotNull;
//import org.roaringbitmap.RoaringBitmap;
//
//import java.util.function.Consumer;
//
///**
// * prioritized by N separate predicate classifiers which add their activation
// * in each case matched.
// */
//public class MixRouter<X, Y extends Priority> implements Consumer<Y> {
//
//    public final AbstractClassifier<Y, X>[] tests;
//    public final FloatFunction<RoaringBitmap> gain;
//    public final AtomicSummaryStatistics[] traffic;
//    private final int dim;
//
//    public int size() {
//        return dim;
//    }
//
//    public final Consumer<Y> target;
//
//    public MixRouter(Consumer<Y> target, FloatFunction<RoaringBitmap> gain, AbstractClassifier<Y, X>... outs) {
//        this.target = target;
//        this.tests = outs;
//        this.gain = gain;
//        int dim = 0;
//        for (AbstractClassifier t : tests)
//            dim += t.dimension();
//        this.dim = dim;
//
//        traffic = new AtomicSummaryStatistics[dim];
//        for (int i = 0; i < dim; i++)
//            traffic[i] = new AtomicSummaryStatistics();
//    }
//
//
//    @Override
//    public void accept(Y y) {
//        this.accept(y, ArrayUtils.EMPTY_INT_ARRAY);
//    }
//
//    public void accept(Y y, int... additionalBits) {
//        float g = gain.floatValueOf(classify(y, additionalBits));
//        if (g >= 0) {
//            y.priMult(g);
//            target.accept(y);
//        }
//    }
//
//
//    public RoaringBitmap classify(@NotNull Y y, int... additionalBits) {
//        RoaringBitmap truths = new RoaringBitmap();
//        int t = 0;
//        for (int i = 0, outsLength = tests.length; i < outsLength; i++) {
//            AbstractClassifier<Y, X> c = tests[i];
//            c.classify(y, truths, t);
//            t += c.dimension();
//        }
//        for (int b : additionalBits)
//            truths.add(b);
//
//        //truths.runOptimize();
//
//        float inputPri = y.priElseZero();
//        truths.forEach((int b) -> {
//            traffic[b].accept(inputPri);
//        });
//
//        return truths;
//    }
//
//}
