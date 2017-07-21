package nars.derive;

import nars.$;
import nars.term.Compound;

import java.util.function.Predicate;


public final class LambdaPred<X> extends AbstractPred<X> {

    private final Predicate<X> test;

    public LambdaPred(Predicate<X> p) {
        this($.p($.the(p.toString())), p);
    }

    public LambdaPred(Compound term, Predicate<X> p) {
        super(term);
        this.test = p;
    }

    @Override
    public boolean test(X x) {
        return test.test(x);
    }
}
