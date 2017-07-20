package nars.term;

import jcog.bag.Bag;
import nars.$;
import nars.NAR;
import nars.concept.AtomConcept;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.term.atom.Atom;
import nars.term.container.TermContainer;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static nars.term.Terms.atomOrNull;
import static nars.term.atom.Atomic.the;

/** a functor is a term transform which immediately returns
 *  a result Term from the TermContainer arguments of
 *  a function term, for example: f(x) or f(x, y).
 */
abstract public class Functor extends AtomConcept implements PermanentConcept, Function<TermContainer,Term> {

    public Functor(@NotNull String atom) {
        this(fName(atom));
    }

    public Functor(@NotNull Atom atom) {
        super(atom, Bag.EMPTY, Bag.EMPTY);
    }

    public static Atom fName(@NotNull String termAtom) {
        return atomOrNull(the(termAtom));
    }

    /** creates a new functor from a term name and a lambda */
    public static LambdaFunctor f(@NotNull String termAtom, @NotNull Function<TermContainer, Term> f) {
        return f(fName(termAtom), f);
    }

    /** creates a new functor from a term name and a lambda */
    public static LambdaFunctor f(@NotNull Atom termAtom, @NotNull Function<TermContainer, Term> f) {
        return new LambdaFunctor(termAtom, f);
    }

    public static LambdaFunctor f(@NotNull String termAtom, int arityRequired, @NotNull Function<TermContainer, Term> ff) {
        return f(fName(termAtom), arityRequired, ff);
    }

    public static LambdaFunctor f(@NotNull Atom termAtom, int arityRequired, @NotNull Function<TermContainer, Term> ff) {
        return f(termAtom, (tt)->{
            if (tt.size()!=arityRequired)
                return null;
                //throw new RuntimeException(termAtom + " requires " + arityRequired + " arguments: " + Arrays.toString(tt));

            return ff.apply(tt);
        });
    }

    /** zero argument (void) functor (convenience method) */
    public static LambdaFunctor f0(@NotNull Atom termAtom, @NotNull Supplier<Term> ff) {
        return f(termAtom, 0, (tt)-> ff.get());
    }

    public static LambdaFunctor f0(@NotNull String termAtom, @NotNull Supplier<Term> ff) {
        return f0(fName(termAtom), ff);
    }

    public static LambdaFunctor f0(@NotNull String termAtom, @NotNull Runnable ff) {
        return f0(fName(termAtom), ()-> {
            ff.run();
            return null;
        });
    }

    /** one argument functor (convenience method) */
    public static LambdaFunctor f1(@NotNull Atom termAtom, @NotNull Function<Term, Term> ff) {
        return f(termAtom, 1, (tt)-> ff.apply(tt.sub(0)));
    }

    public static <X extends Term> LambdaFunctor f1(@NotNull String termAtom, @NotNull Function<X, Term> ff) {
        return f1(fName(termAtom), safeFunctor(ff));
    }
    public static <X extends Term> LambdaFunctor f1Const(@NotNull String termAtom, @NotNull Function<X, Term> ff) {
        return f1(fName(termAtom), safeFunctor(ff));
    }

    public @NotNull
    static <X extends Term> Function<Term, Term> safeFunctor(@NotNull Function<X, Term> ff) {
        return x ->
                (x==null || x instanceof Variable) ? null
                :
                ff.apply((X) x);
    }

    /** a functor involving a concept resolved by the 1st argument term */
    public static LambdaFunctor f1Concept(@NotNull String termAtom, NAR nar, @NotNull BiFunction<Concept, NAR, Term> ff) {
        return f1(fName(termAtom), t -> {
            Concept c = nar.concept(t);
            if (c!=null) {
                return ff.apply(c, nar);
            } else {
                return null;
            }
        });
    }


    /** two argument functor (convenience method) */
    public static LambdaFunctor f2(@NotNull Atom termAtom, @NotNull BiFunction<Term, Term, Term> ff) {
        return f(termAtom, 2, (tt)-> ff.apply(tt.sub(0), tt.sub(1)));
    }

    /** two argument functor (convenience method) */
    public static LambdaFunctor f2(@NotNull String termAtom, @NotNull BiFunction<Term, Term, Term> ff) {
        return f2(fName(termAtom), ff);
    }

    /** two argument non-variable integer functor (convenience method) */
    @FunctionalInterface public interface IntIntFunc {
        int apply(int x, int y);
    }

    public static Concept f2Int(@NotNull String termAtom, @NotNull IntIntFunc ff) {
        return f2(fName(termAtom), (xt, yt) -> {
            try {
                return $.the(ff.apply($.intValue(xt), $.intValue(yt)));
            } catch(NumberFormatException e) {
                return null;
            }
        });
    }


    public static final class LambdaFunctor extends Functor  {

        @NotNull private final Function<TermContainer, Term> f;

        public LambdaFunctor(@NotNull Atom termAtom, @NotNull Function<TermContainer, Term> f) {
            super(termAtom);
            this.f = f;
        }

        @Nullable
        @Override public Term apply(@NotNull TermContainer terms) {
            return f.apply(terms);
        }
    }

    /**
     * Created by me on 12/12/15.
     */
    public abstract static class UnaryFunctor extends Functor {

        protected UnaryFunctor(@NotNull String id) {
            super(id);
        }

        @Nullable
        @Override public final Term apply(@NotNull TermContainer x) {
            if (x.size()!=1)
                return null;
                //throw new UnsupportedOperationException("# args must equal 1");

            return apply(x.sub(0));
        }

        @Nullable
        public abstract Term apply(@NotNull Term x);
    }

    /**
     * Created by me on 12/12/15.
     */
    public abstract static class BinaryFunctor extends Functor {

        protected BinaryFunctor(@NotNull String id) {
            super(id);
        }

        @Nullable
        @Override public final Term apply(@NotNull TermContainer x) {
            if (x.size()!=2)
                throw new UnsupportedOperationException("# args must equal 2");

            return apply(x.sub(0), x.sub(1));
        }

        @Nullable
        public abstract Term apply(@NotNull Term a, @NotNull Term b);
    }
}
