package nars.derive;

import nars.derive.meta.BoolPredicate;
import nars.term.ProxyTerm;
import nars.term.Term;

/**
 * Created by me on 4/17/17.
 */
public abstract class InstrumentedBoolPredicate<C> extends ProxyTerm<Term> implements BoolPredicate<C> {
    protected final BoolPredicate<C> ref;

    public InstrumentedBoolPredicate( BoolPredicate<C> ref) {
        super(ref);
        this.ref = ref;
    }

    @Override
    public abstract boolean test(C p);
}
