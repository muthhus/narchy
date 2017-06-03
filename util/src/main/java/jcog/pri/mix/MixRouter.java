package jcog.pri.mix;

import jcog.math.AtomicSummaryStatistics;
import jcog.pri.Priority;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.roaringbitmap.RoaringBitmap;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * prioritized by N separate predicate classifiers which add their activation
 * in each case matched.
 */
public class MixRouter<X, Y extends Priority> implements Consumer<Y> {

    public final Classifier<Y, X>[] tests;
    private final FloatFunction<RoaringBitmap> gain;

    public int size() {
        return tests.length;
    }

    private final Consumer<Y> target;

    public static class Classifier<X, Y> {

        public final Y name;
        final Predicate<X> pred;
        public final AtomicSummaryStatistics in = new AtomicSummaryStatistics();

        public Classifier(Y name, Predicate<X> pred) {
            this.name = name;
            this.pred = pred;
        }

        public boolean test(X x) {
            return pred.test(x);
        }
    }

    public MixRouter(Consumer<Y> target, FloatFunction<RoaringBitmap> gain, Classifier<Y, X>... outs) {
        this.target = target;
        this.tests = outs;
        this.gain = gain;
    }


    @Override
    public void accept(Y y) {
        float g = gain.floatValueOf(classify(y));
        if (g > 0) {
            y.priMult( g );
            target.accept(y);
        }
    }

    public RoaringBitmap classify(Y y) {
        RoaringBitmap truths = new RoaringBitmap();
        for (int i = 0, outsLength = tests.length; i < outsLength; i++) {
            Classifier<Y, X> c = tests[i];
            if (c.test(y)) {
                c.in.accept(y.priElseZero());
                truths.add(i);
            }
        }
        return truths;
    }

}
