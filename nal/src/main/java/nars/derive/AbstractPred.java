package nars.derive;

import nars.$;
import nars.term.ProxyTerm;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 4/21/17.
 */
public abstract class AbstractPred<X> extends ProxyTerm<Term> implements PrediTerm<X> {

    protected AbstractPred(@NotNull String x) {
        this($.$safe(x));
    }

    protected AbstractPred(@NotNull Term term) {
        super(term);
    }

}
