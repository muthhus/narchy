package nars.derive;

import nars.Op;
import nars.control.Derivation;
import nars.term.Term;

import java.util.function.Function;
import java.util.function.Predicate;

/** a term representing a native predicate */
public interface PrediTerm<X> extends Term, Predicate<X> {


    PrediTerm<Derivation> NullDeriver = new AbstractPred<Derivation>(Op.ZeroProduct) {
        @Override
        public boolean test(Derivation o) {
            return true;
        }
    };

    default PrediTerm<X> transform(Function<PrediTerm<X>, PrediTerm<X>> f) {
        return f.apply(this);
    }

    /** returns null on success; returns this instance on the test failure. go figure */
    default PrediTerm<X> exec(X context, CPU cpu) {
        if (!test(context))
            return this;
        else
            return null;
    }

}
