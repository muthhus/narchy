package nars;

import jcog.memoize.CaffeineMemoize;
import jcog.memoize.HijackMemoize;
import jcog.memoize.Memoize;
import jcog.memoize.SoftMemoize;
import nars.index.term.NewCompound;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.compound.GenericCompound;
import nars.term.compound.UnitCompound1;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static nars.Op.Null;
import static nars.Op.PROD;
import static nars.derive.match.Ellipsis.hasEllipsis;

/**
 * memoization/interning for subterms and compounds
 */
public class Builder {

    public static final class Subterms {

        public static final Function<Term[], TermContainer> HeapSubtermBuilder =
                TermVector::the;

        public static final Supplier<Function<Term[], TermContainer>> SoftSubtermBuilder = () ->
                new MemoizeSubtermBuilder(new SoftMemoize<>((n)->HeapSubtermBuilder.apply(n.subs), 64 * 1024, true));

        public static final Supplier<Function<Term[], TermContainer>> WeakSubtermBuilder = () ->
                new MemoizeSubtermBuilder(new SoftMemoize<>((n)->HeapSubtermBuilder.apply(n.subs), 64 * 1024, false));

        public static final Supplier<Function<Term[], TermContainer>> CaffeineSubtermBuilder = () ->
                new MemoizeSubtermBuilder(CaffeineMemoize.build((n)->HeapSubtermBuilder.apply(n.subs), 64 * 1024, false));


        public static final Supplier<Function<Term[], TermContainer>> HijackSubtermBuilder = () ->
                new Function<>() {

                    final HijackMemoize<NewCompound, TermContainer> cache
                            = new HijackMemoize<>((x) -> HeapSubtermBuilder.apply(x.subs),
                            128 * 1024 + 7 /* ~prime */, 4);

                    @Override
                    public TermContainer apply(Term[] o) {
                        return cache.apply(
                                new NewCompound(PROD, o).commit()
                        );
                    }
                };

        public static Function<Term[], TermContainer> the = HeapSubtermBuilder;

        private static class MemoizeSubtermBuilder implements Function<Term[], TermContainer> {
            final Memoize<NewCompound, TermContainer> cache;

            private MemoizeSubtermBuilder(Memoize<NewCompound, TermContainer> cache) {
                this.cache = cache;
            }

            @Override
            public TermContainer apply(Term[] terms) {
                return cache.apply(new NewCompound(PROD, terms).commit());
            }
        }
        //CaffeineSubtermBuilder;
        //HijackSubtermBuilder;

    }

    public static class Compound {

        public static final BiFunction<Op, Term[], Term> HeapCompoundBuilder = (o, subterms) -> {
            assert (!o.atomic) : o + " is atomic, with subterms: " + Arrays.toString(subterms);

            if (!o.allowsBool) {
                for (Term x : subterms)
                    if (x instanceof Bool)
                        return Null;
            }

            int s = subterms.length;
            assert (o.maxSize >= s) :
                    "subterm overflow: " + o + ' ' + Arrays.toString(subterms);

            assert (o.minSize <= s || hasEllipsis(subterms)) : "subterm underflow: " + o + ' ' + Arrays.toString(subterms);

            switch (s) {
                case 1:
                    return new UnitCompound1(o, subterms[0]);

                default:
                    return new GenericCompound(o, Builder.Subterms.the.apply(subterms));
            }

        };

        public static final Supplier<BiFunction<Op, Term[], Term>> SoftCompoundBuilder = ()->
                new BiFunction<>() {

                    final SoftMemoize<NewCompound, Term> cache = new SoftMemoize<NewCompound, Term>((v) -> HeapCompoundBuilder.apply(v.op, v.subs), 64 * 1024, true);

                    @Override
                    public Term apply(Op op, Term[] terms) {
                        return cache.apply(new NewCompound(op, terms).commit());
                    }
                };

        public static final Supplier<BiFunction<Op, Term[], Term>> CaffeineCompoundBuilder = ()->new BiFunction<>() {

            final CaffeineMemoize<NewCompound, Term> cache = CaffeineMemoize.build((v) -> HeapCompoundBuilder.apply(v.op, v.subs), 64 * 1024, false);

            @Override
            public Term apply(Op op, Term[] terms) {
                return cache.apply(new NewCompound(op, terms).commit());
            }
        };

        public static final Supplier<BiFunction<Op, Term[], Term>> HijackCompoundBuilder = ()->new BiFunction<>() {

            final HijackMemoize<NewCompound, Term> cache
                    = new HijackMemoize<>((x) -> HeapCompoundBuilder.apply(x.op, x.subs),
                    128 * 1024 + 7 /* ~prime */, 3);

            @Override
            public Term apply(Op o, Term[] subterms) {
                return cache.apply(
                        new NewCompound(o, subterms).commit()
                );
            }
        };

        public static BiFunction<Op, Term[], Term> the =
                HeapCompoundBuilder;
        //HijackCompoundBuilder;

    }
}
