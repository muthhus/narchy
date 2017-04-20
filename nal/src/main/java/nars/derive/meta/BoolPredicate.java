package nars.derive.meta;

import nars.term.Compound;
import nars.term.ProxyCompound;
import nars.term.Term;

import java.util.function.Predicate;

/**
 * Created by me on 12/31/15.
 */
public interface BoolPredicate<X> extends Term, Predicate<X> {

    abstract class AbstractBoolPredicate<X> extends ProxyCompound implements BoolPredicate<X> {


        public AbstractBoolPredicate(Compound term) {
            super(term);
        }

    }

    class DefaultBoolPredicate<X> extends AbstractBoolPredicate<X> {

        private final Predicate<X> test;

        public DefaultBoolPredicate(Compound term, Predicate<X> p) {
            super(term);
            this.test = p;
        }

        @Override
        public boolean test(X x) {
            return test.test(x);
        }
    }


}
