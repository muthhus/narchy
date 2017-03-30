package nars.index.term.map;

import jcog.Util;
import jcog.bag.impl.HijackMemoize;
import jcog.list.FasterList;
import jcog.random.XorShift128PlusRandom;
import nars.Op;
import nars.Param;
import nars.concept.PermanentConcept;
import nars.conceptualize.ConceptBuilder;
import nars.index.term.TermIndex;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.transform.VariableNormalization;
import nars.term.util.InvalidTermException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Index which is supported by Map/Cache-like operations
 */
public abstract class MaplikeTermIndex extends TermIndex {

    final Function<TermBuilder,Term> build = new HijackMemoize<>(
            64384, 3, new XorShift128PlusRandom(1),
            (C) -> super.the(C.op, C.dt, C.subs)
    );

    final Function<Compound,Term> normalize = new HijackMemoize<Compound,Term>(
            16384, 2, new XorShift128PlusRandom(1),
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



    private static class TermBuilder {

        private final int hash;

        private final Op op;
        private final int dt;
        final Term[] subs;

        public TermBuilder(Op op, int dt, Term[] subterms) {

            int hash = Util.hashCombine(op.hashCode(), dt);

            for (Term x : subterms) {
                hash = Util.hashCombine(hash, x.hashCode());
            }

            this.subs = subterms;
            this.op = op;
            this.dt = dt;

            this.hash = hash;
        }

        @Override
        public boolean equals(Object obj) {
            TermBuilder f = (TermBuilder) obj;
            return f.hash == hash && f.op == op && f.dt == dt && Arrays.equals(f.subs, subs);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }




    @Override
    public @NotNull Term the(@NotNull Op op, int dt, @NotNull Term[] u) throws InvalidTermException {
        if (u.length < 2)
            return super.the(op, dt, u);

        return build.apply(new TermBuilder( op, dt, u ));
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
