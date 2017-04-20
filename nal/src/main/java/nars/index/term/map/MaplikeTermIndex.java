package nars.index.term.map;

import jcog.bag.impl.hijack.HijackMemoize;
import jcog.random.XorShift128PlusRandom;
import nars.Op;
import nars.concept.PermanentConcept;
import nars.conceptualize.ConceptBuilder;
import nars.index.term.AppendProtoCompound;
import nars.index.term.ProtoCompound;
import nars.index.term.TermIndex;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static nars.Op.INH;
import static nars.Op.PROD;
import static nars.term.Terms.compoundOrNull;

/**
 * Index which is supported by Map/Cache-like operations
 */
public abstract class MaplikeTermIndex extends TermIndex {

    @NotNull protected final ConceptBuilder conceptBuilder;


    public MaplikeTermIndex(@NotNull ConceptBuilder conceptBuilder) {
        this.conceptBuilder = conceptBuilder;
    }


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

    final Function<ProtoCompound,Term> build = new HijackMemoize<>(
            64384, 3, new XorShift128PlusRandom(1),
            (C) -> super.the(C.op(), C.dt(), C.subterms())
    );

    final Function<Compound,Term> normalize = new HijackMemoize<Compound,Term>(
            16*1024, 2, new XorShift128PlusRandom(1),
            super::normalize
    );

    @Override
    public @NotNull Term the(@NotNull Op op, int dt, @NotNull Term[] u)  {
        if (u.length < 2)
            return super.the(op, dt, u);

        return the(new AppendProtoCompound( op, dt, u ));
    }

    @Override protected Term the(ProtoCompound c) {

        if (!cacheable(c)) {
            return super.the(c.op(), c.dt(), c.subterms()); //immediate construct
        } else {
            return build.apply(c);
        }
    }

    private static final int PROD_or_INH_bits = Op.or(PROD,INH);

    protected boolean cacheable(ProtoCompound c) {

        return (c.size() > 1) &&
               !c.op().in(PROD_or_INH_bits)
               && c.AND(x -> !x.hasAny(PROD_or_INH_bits));
    }

    @Nullable
    @Override public final Compound normalize(@NotNull Compound x) {

        if (x.isNormalized()) {
            return x;
        } else {
            return compoundOrNull(normalize.apply(x));
        }

    }






}
