package nars.derive.meta;

import nars.$;
import nars.term.Compound;
import nars.term.compound.ProxyCompound;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 4/21/17.
 */
public abstract class AbstractPred<X> extends ProxyCompound implements BoolPred<X> {

    public AbstractPred(@NotNull String x) {
        this($.$safe(x));
    }

    public AbstractPred(@NotNull Compound term) {
        super(term);
    }

}
