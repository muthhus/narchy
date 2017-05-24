package nars.index.term.map;

import nars.concept.PermanentConcept;
import nars.conceptualize.ConceptBuilder;
import nars.term.Termed;
import nars.term.util.CachedTermIndex;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Index which is supported by Map/Cache-like operations
 */
public abstract class MaplikeTermIndex extends CachedTermIndex {

    //final static Logger logger = LoggerFactory.getLogger(MaplikeTermIndex.class);

    @NotNull
    protected final ConceptBuilder conceptBuilder;


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
        return "CACHE term=" + terms.summary()
                //+ " normalize=" + normalize.summary()
                ;
    }
}
