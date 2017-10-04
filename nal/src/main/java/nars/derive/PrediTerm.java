package nars.derive;

import jcog.Util;
import nars.Op;
import nars.control.Derivation;
import nars.control.Deriver;
import nars.term.Term;

import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * a term representing a predicate (boolean-returning) function of a state
 *
 * @param X the type of state that is relevant to implementations
 */
public interface PrediTerm<X> extends Term, Predicate<X> {



    static final Comparator<PrediTerm> sortByCost = (a, b) -> {
        if (a.equals(b)) return 0;
        float ac = a.cost();
        float bc = b.cost();
        if (ac > bc) return +1;
        else if (ac < bc) return -1;
        else return a.compareTo(b);
    };

    public static Comparator<PrediTerm<?>> sort(ToIntFunction<PrediTerm<?>> count) {
        return (a, b) -> {

            if (a.equals(b)) return 0;

            float ac = count.applyAsInt(a) / a.cost();
            float bc = count.applyAsInt(b) / b.cost();
            if (ac > bc) return -1;
            else if (ac < bc) return +1;
            else return a.compareTo(b);
        };
    }

    static <X> PrediTerm<X>[] transform(Function<PrediTerm<X>, PrediTerm<X>> f, PrediTerm[] cache) {
        return Util.map(x -> x.transform(f), new PrediTerm[cache.length], cache);
    }

    default PrediTerm<X> transform(Function<PrediTerm<X>, PrediTerm<X>> f) {
        return f != null ? f.apply(this) : this;
    }

    /**
     * a relative global estimate (against its possible sibling PrediTerm's)
     * of the average computational cost of running the test method
     * warning: these need to return constant values for sort consistency
     */
    default float cost() {
        return 1;
    }

//    /** returns null on success; returns this instance on the test failure. go figure */
//    default PrediTerm<X> exec(X context, CPU cpu) {
//        if (!test(context))
//            return this;
//        else
//            return null;
//    }

}
