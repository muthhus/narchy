package nars;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jcog.memoize.HijackMemoize;
import nars.index.term.NewCompound;
import nars.term.Term;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;

import java.util.function.Function;

/**
 * memoization/interning for subterms and compounds
 */
public class Builder {

    public static class Subterms {

        private static Function<Term[], TermContainer> HeapSubtermBuilder =
                TermVector::the;

        private static Function<Term[], TermContainer> CaffeineSubtermBuilder =
                new Function<Term[], TermContainer>() {

                    final Cache<NewCompound, TermContainer> cache =
                            Caffeine.newBuilder().maximumSize(64 * 1024).build();

                    @Override
                    public TermContainer apply(Term[] o) {
                        return cache.get(
                                new NewCompound(Op.PROD, o).commit(),
                                (x) -> TermVector.the(x.subs)
                        );
                    }
                };
        private static Function<Term[], TermContainer> HijackSubtermBuilder =
                new Function<Term[], TermContainer>() {

                    final HijackMemoize<NewCompound, TermContainer> cache
                            = new HijackMemoize<>((x) -> TermVector.the(x.subs),
                            64 * 1024, 3);

                    @Override
                    public TermContainer apply(Term[] o) {
                        return cache.apply(
                                new NewCompound(Op.PROD, o).commit()
                        );
                    }
                };

        public static Function<Term[], TermContainer> the =
                //HeapSubtermBuilder;
                //CaffeineSubtermBuilder;
                HijackSubtermBuilder;

    }

}
