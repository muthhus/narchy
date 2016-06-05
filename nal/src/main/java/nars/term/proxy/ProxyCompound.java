package nars.term.proxy;

import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import nars.term.subst.FindSubst;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 5/28/16.
 */
public interface ProxyCompound<T extends Compound<Term>> extends ProxyTerm<T>, Compound<Term> {



    @Override
    default @NotNull TermContainer subterms() {
        return proxy().subterms();
    }

    @Override
    default boolean match(@NotNull Compound y, @NotNull FindSubst subst) {
        return proxy().match(y, subst);
    }


    @Override
    default int relation() {
        return proxy().relation();
    }

    @Override
    default boolean isNormalized() {
        return proxy().isNormalized();
    }

    @Override
    default int dt() {
        return proxy().dt();
    }


}
