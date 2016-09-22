package nars.index;

import nars.Op;
import nars.Param;
import nars.concept.PermanentConcept;
import nars.concept.util.ConceptBuilder;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Index which is supported by Map/Cache-like operations
 */
public abstract class MaplikeIndex extends TermIndex {

    @NotNull protected final ConceptBuilder conceptBuilder;

    public MaplikeIndex(@NotNull ConceptBuilder conceptBuilder) {
        this.conceptBuilder = conceptBuilder;
    }


    abstract public @NotNull TermContainer internSubterms(@NotNull TermContainer s);


    @Nullable @Override abstract public Termed get(@NotNull Term key, boolean createIfMissing);

//    @NotNull
//    protected Termed buildConcept(@NotNull Termed interned) {
//
//        return conceptBuilder.apply(interned.term());
//    }

//    @Nullable
//    protected final Term buildCompound(@NotNull Op op, int dt, @NotNull TermContainer subs) {
//        TermContainer s = theSubterms(subs);
//        if (op == INH && (subs.term(1).op() == OPER) && subs.term(0).op() == PROD)
//            return termOrNull(the(INH, dt, s.terms())); //HACK send through the full build process in case it is an immediate transform
//        else
//            return finish(op, dt, s);
//    }

    @Override
    public final ConceptBuilder conceptBuilder() {
        return conceptBuilder;
    }

    @Override
    public abstract void forEach(@NotNull Consumer<? super Termed> c);

    public static final BiFunction<? super Termed, ? super Termed, ? extends Termed> setIfNotAlreadyPermanent = (prev, next) -> {
        if (prev instanceof PermanentConcept)
            return prev;
        return next;
    };

}
