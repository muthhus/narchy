package nars.derive;

import nars.$;
import nars.term.Compound;
import nars.term.ProxyCompound;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 4/21/17.
 */
public abstract class AbstractPred<X> extends ProxyCompound implements PrediTerm<X> {

    protected AbstractPred(@NotNull String x) {
        this($.$safe(x));
    }

    protected AbstractPred(@NotNull Compound term) {
        super(term);
    }

}
