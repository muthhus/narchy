package nars.index.term.map;

import jcog.bag.impl.hijack.HijackMemoize;
import nars.Op;
import nars.Param;
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

    final HijackMemoize<ProtoCompound,Term> build = new HijackMemoize<>(
        128*1024, 4,
        (C) -> super.the(C.op(), C.dt(), C.subterms())
    );
//        @Override
//        public float value(@NotNull ProtoCompound protoCompound) {
//            //??
//        }
//    };

    final HijackMemoize<Compound,Term> normalize = new HijackMemoize<>(
        64 * 1024, 3,
        super::normalize
    );

    @Override
    public @NotNull Term the(@NotNull Op op, int dt, @NotNull Term[] u)  {
        if (u.length < 2)
            return super.the(op, dt, u);

        return the(new AppendProtoCompound( op, dt, u ));
    }

    @Override protected Term the(ProtoCompound c) {

        if (!c.isDynamic()) {
            return super.the(c.op(), c.dt(), c.subterms()); //immediate construct
        } else {
            build.miss.increment();
            return build.apply(c);
        }
    }


    @Nullable
    @Override public final Compound normalize(@NotNull Compound x) {

        if (x.isNormalized()) {
            return x;
        } else {
            return compoundOrNull(normalize.apply(x));
        }
    }


    @Override
    public @NotNull String summary() {
        return "CACHE: build=" + build.summary() + " normalize=" + normalize.summary();
    }
}
