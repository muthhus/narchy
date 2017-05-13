package nars.index.term.map;

import jcog.bag.impl.hijack.HijackMemoize;
import nars.Op;
import nars.Param;
import nars.concept.PermanentConcept;
import nars.conceptualize.ConceptBuilder;
import nars.index.term.AppendProtoCompound;
import nars.index.term.ProtoCompound;
import nars.index.term.TermIndex;
import nars.term.Term;
import nars.term.Termed;
import nars.term.util.InvalidTermException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import static nars.Op.False;

/**
 * Index which is supported by Map/Cache-like operations
 */
public abstract class MaplikeTermIndex extends TermIndex {

    final static Logger logger = LoggerFactory.getLogger(MaplikeTermIndex.class);

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

    protected final HijackMemoize<ProtoCompound,Term> terms = new HijackMemoize<>(
        256*1024, 5,
        (C) -> {
            try {
                return super.the(C.op(), C.dt(), C.subterms());
            } catch ( InvalidTermException e) {
                if (Param.DEBUG_EXTRA)
                    logger.error("{}", e);
                return False;
            } catch (Throwable t) {
                logger.error("{}", t);
                return False;
            }
        }
    );
//        @Override
//        public float value(@NotNull ProtoCompound protoCompound) {
//            //??
//        }
//    };

//    final HijackMemoize<Compound,Term> normalize = new HijackMemoize<>(
//        64 * 1024, 2,
//        super::normalize
//    );

    @Override
    public @NotNull Term the(@NotNull Op op, int dt, @NotNull Term... u)  {
        if (u.length < 2)
            return super.the(op, dt, u);

        return terms.apply(new AppendProtoCompound( op, dt, u ));
    }

    @Override public Term the(ProtoCompound c) {
        if (c.size() < 2)
            return super.the(c);

//        if (!c.isDynamic()) {
//            build.miss.increment();
//            return super.the(c.op(), c.dt(), c.subterms()); //immediate construct
//        } else {
            return terms.apply(c);
//        }
    }


    //
//    @Nullable
//    @Override public final Compound normalize(@NotNull Compound x) {
//
//        if (x.isNormalized()) {
//            return x;
//        } else {
//            return compoundOrNull(normalize.apply(x));
//        }
//    }


    @Override
    public @NotNull String summary() {
        return "CACHE build=" + terms.summary()
        //+ " normalize=" + normalize.summary()
        ;
    }
}
