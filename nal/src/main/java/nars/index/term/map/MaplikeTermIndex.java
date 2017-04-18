package nars.index.term.map;

import jcog.bag.impl.hijack.HijackMemoize;
import jcog.random.XorShift128PlusRandom;
import nars.Op;
import nars.concept.PermanentConcept;
import nars.conceptualize.ConceptBuilder;
import nars.index.term.CompoundBuilder;
import nars.index.term.TermIndex;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.util.InvalidTermException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Index which is supported by Map/Cache-like operations
 */
public abstract class MaplikeTermIndex extends TermIndex {

    final Function<CompoundBuilder,Term> build = new HijackMemoize<>(
            64384, 3, new XorShift128PlusRandom(1),
            (C) -> super.the(C.op, C.dt, C.toArray(new Term[C.size()]))
    );

    final Function<Compound,Term> normalize = new HijackMemoize<Compound,Term>(
            16384, 3, new XorShift128PlusRandom(1),
            super::normalize
    );


    @NotNull protected final ConceptBuilder conceptBuilder;

    public MaplikeTermIndex(@NotNull ConceptBuilder conceptBuilder) {
        this.conceptBuilder = conceptBuilder;
    }


    @Nullable @Override abstract public Termed get(@NotNull Term key, boolean createIfMissing);

    @Override
    public final ConceptBuilder conceptBuilder() {
        return conceptBuilder;
    }

    @Override
    public abstract void forEach(@NotNull Consumer<? super Termed> c);

    public static final BiFunction<? super Termed, ? super Termed, ? extends Termed> setOrReplaceNonPermanent = (prev, next) -> {
        if (prev instanceof PermanentConcept && !(next instanceof PermanentConcept))
            return prev;
        return next;
    };


    @Override
    public @NotNull Term the(@NotNull Op op, int dt, @NotNull Term[] u) throws InvalidTermException {
        if (u.length < 2)
            return super.the(op, dt, u);

        return the(new CompoundBuilder( op, dt, u ));
    }

    @Override protected Term the(CompoundBuilder c) {
        if (c.size() < 2)
            return super.the(c); //immediate construct

        return build.apply(c);
    }

    @Nullable
    public final Term normalize(@NotNull Compound x) {

        if (x.isNormalized()) {
            return x;
        } else {

            return normalize.apply(x);
        }

    }






}
