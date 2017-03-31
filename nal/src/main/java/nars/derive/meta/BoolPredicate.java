package nars.derive.meta;

import nars.term.ProxyTerm;
import nars.term.Term;

import java.util.function.Predicate;

/**
 * Created by me on 12/31/15.
 */
public interface BoolPredicate<X> extends Term, Predicate<X> {

    class DefaultBoolPredicate<X> extends ProxyTerm<Term> implements BoolPredicate<X> {

        private final Predicate<X> test;

        public DefaultBoolPredicate(Term term, Predicate<X> p) {
            super(term);
            this.test = p;
        }

        @Override
        public boolean test(X x) {
            return test.test(x);
        }
    }


    //void accept(PremiseEval c, int now);

    @Override
    boolean test(X p);



//    static void run(@NotNull BoolCondition b, @NotNull PremiseEval m) {
//        final int stack = m.now();
//        b.booleanValueOf(m, stack);
//        m.revert(stack);
//    }


}
