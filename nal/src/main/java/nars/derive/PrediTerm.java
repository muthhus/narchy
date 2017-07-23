package nars.derive;

import nars.term.Term;

import java.util.function.Function;
import java.util.function.Predicate;

/** a term representing a native predicate */
public interface PrediTerm<X> extends Term, Predicate<X> {


    default PrediTerm<X> transform(Function<PrediTerm<X>, PrediTerm<X>> f) {
        return f.apply(this);
    }

}
