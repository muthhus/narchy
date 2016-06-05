package nars.term.proxy;

import nars.Op;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

public interface ProxyTerm<T extends Term> extends Term {


    @NotNull
    T proxy();

    @Override
    default @NotNull Op op() {
        return proxy().op();
    }


}
