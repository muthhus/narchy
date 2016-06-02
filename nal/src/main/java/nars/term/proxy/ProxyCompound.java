package nars.term.proxy;

import nars.Op;
import nars.term.Compound;
import nars.term.SubtermVisitor;
import nars.term.Term;
import nars.term.container.TermContainer;
import nars.term.subst.FindSubst;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by me on 5/28/16.
 */
public interface ProxyCompound<T extends Compound<Term>> extends ProxyTerm<T>, Compound<Term> {

    @Override
    default @NotNull TermContainer subterms() {
        return target().subterms();
    }

    @Override
    default void recurseTerms(@NotNull SubtermVisitor v, Compound parent) {
        target().recurseTerms(v,parent);
    }



    @Override
    default boolean hasTemporal() {
        return target().hasTemporal();
    }

    @Override
    default boolean match(@NotNull Compound y, @NotNull FindSubst subst) {
        return target().match(y, subst);
    }

    @Override
    default int opRel() {
        return target().opRel();
    }

    @Override
    default int relation() {
        return target().relation();
    }

    @Override
    default boolean isNormalized() {
        return target().isNormalized();
    }

    @NotNull
    @Override
    default Compound dt(int cycles) {
        return target().dt(cycles);
    }

    @Override
    default int dt() {
        return target().dt();
    }


}
