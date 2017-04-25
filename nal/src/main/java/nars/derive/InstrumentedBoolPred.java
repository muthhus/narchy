package nars.derive;

import nars.derive.meta.BoolPred;
import nars.term.ProxyTerm;
import nars.term.Term;

/**
 * Created by me on 4/17/17.
 */
public abstract class InstrumentedBoolPred<C> extends ProxyTerm<Term> implements BoolPred<C> {

    public InstrumentedBoolPred(BoolPred<C> ref) {
        super(ref);
    }

    @Override
    public abstract boolean test(C p);
}
