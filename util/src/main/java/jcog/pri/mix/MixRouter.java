package jcog.pri.mix;

import jcog.pri.Priority;

import java.util.function.Consumer;
import java.util.function.Predicate;

/** prioritized by N separate predicate classifiers which add their activation
 *  in each case matched.  */
public class MixRouter<X, Y extends Priority> extends Mix<X,Y> implements Consumer<Y> {

    public final PSink[] outs;
    public final Classifier<Y, X>[] possibles;
    private final Consumer<Y> actualTarget;

    public int size() {
        return outs.length;
    }

    public static class Classifier<X, Y> {

        public final Y name;

        final Predicate<X> pred;

        public Classifier(Y name, Predicate<X> pred) {
            this.name = name;
            this.pred = pred;
        }

        public boolean test(X x) { return pred.test(x); }
    }

    public MixRouter(Consumer<Y> target, Classifier<Y, X>... outs) {
        super(null);

        this.actualTarget = target;
        int i = 0;
        this.possibles = outs;
        this.outs = new PSink[outs.length];
        for (Classifier<Y, X> s : outs) {
            this.outs[i++] = stream(s.name);
        }
    }

    @Override
    public void accept(Y y) {
        float p = 0f;
        for (int i = 0, outsLength = outs.length; i < outsLength; i++) {
            Classifier<Y, X> c = possibles[i];
            if (c.test(y)) {
                p += outs[i].floatValue(); //just use the stream for its priority value
                outs[i].out.accept(y.priElseZero());
            }
        }
        if (p > 0) {
            y.priMult(p);

            actualTarget.accept(y);
        }
    }

}
