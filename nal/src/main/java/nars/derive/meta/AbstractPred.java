package nars.derive.meta;

import nars.$;
import nars.term.Compound;
import nars.term.ProxyCompound;

/**
 * Created by me on 4/21/17.
 */
public abstract class AbstractPred<X> extends ProxyCompound implements BoolPred<X> {

    public AbstractPred(String x) {
        this($.$safe(x));
    }

    public AbstractPred(Compound term) {
        super(term);
    }

}
