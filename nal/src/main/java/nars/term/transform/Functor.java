package nars.term.transform;

import jcog.bag.Bag;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.concept.AtomConcept;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static nars.$.the;
import static nars.Op.INT;
import static nars.term.Terms.atomOrNull;

/** a functor is a term transform which immediately returns
 *  a result Term from the Term[] arguments of
 *  a function term, for example: f(x) or f(x, y).
 */
abstract public class Functor extends AtomConcept implements PermanentConcept, Function<Term[],Term> {

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
    public static Concept f(@NotNull String termAtom, @NotNull Function<Term[], Term> f) {
        return f(fName(termAtom), f);
    }

    /** creates a new functor from a term name and a lambda */
    public static Concept f(@NotNull Atom termAtom, @NotNull Function<Term[], Term> f) {
        return new LambdaFunctor(termAtom, f);
    }

    public static Concept f(@NotNull String termAtom, int arityRequired, @NotNull Function<Term[], Term> ff) {
        return f(fName(termAtom), arityRequired, ff);
    }

    public static Concept f(@NotNull Atom termAtom, int arityRequired, @NotNull Function<Term[], Term> ff) {
        return f(termAtom, (tt)->{
            if (tt.length!=arityRequired)
                return null;
                //throw new RuntimeException(termAtom + " requires " + arityRequired + " arguments: " + Arrays.toString(tt));

            return ff.apply(tt);
        });
    }

    /** zero argument (void) functor (convenience method) */
    public static Concept f0(@NotNull Atom termAtom, @NotNull Supplier<Term> ff) {
        return f(termAtom, 0, (tt)-> ff.get());
    }

    public static Concept f0(@NotNull String termAtom, @NotNull Supplier<Term> ff) {
        return f0(fName(termAtom), ff);
    }

    public static Concept f0(@NotNull String termAtom, @NotNull Runnable ff) {
        return f0(fName(termAtom), ()-> {
            ff.run();
            return null;
        });
    }

    /** one argument functor (convenience method) */
    public static Concept f1(@NotNull Atom termAtom, @NotNull Function<Term, Term> ff) {
        return f(termAtom, 1, (tt)-> ff.apply(tt[0]));
    }

    public static Concept f1(@NotNull String termAtom, @NotNull Function<Term, Term> ff) {
        return f1(fName(termAtom), ff);
    }
    public static Concept f1Const(@NotNull String termAtom, @NotNull Function<Term, Term> ff) {
        return f1(fName(termAtom), x -> x instanceof Variable ? null : ff.apply(x));
    }

    /** a functor involving a concept resolved by the 1st argument term */
    public static Concept f1Concept(@NotNull String termAtom, NAR nar, @NotNull BiFunction<Concept, NAR, Term> ff) {
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
    public static Concept f2(@NotNull Atom termAtom, @NotNull BiFunction<Term, Term, Term> ff) {
        return f(termAtom, 2, (tt)-> ff.apply(tt[0], tt[1]));
    }

    /** two argument functor (convenience method) */
    public static Concept f2(@NotNull String termAtom, @NotNull BiFunction<Term, Term, Term> ff) {
        return f2(fName(termAtom), ff);
    }

    /** two argument non-variable integer functor (convenience method) */
    @FunctionalInterface public interface IntIntFunc {
        int apply(int x, int y);
    }

    public static Concept f2Int(@NotNull String termAtom, @NotNull IntIntFunc ff) {
        return f2(fName(termAtom), (xt, yt) -> {
            if ((xt.op() != INT) || (yt.op() != INT))
                return null;
            else
                return $.the(ff.apply(Term.intValue(xt), Term.intValue(yt)));
        });
    }

    @NotNull
    @Override
    public final Op op() {
        return Op.ATOM;
    }

    public static final class LambdaFunctor extends Functor {

        @NotNull private final Function<Term[], Term> f;

        public LambdaFunctor(@NotNull Atom termAtom, @NotNull Function<Term[], Term> f) {
            super(termAtom);
            this.f = f;
        }

        @Nullable
        @Override public Term apply(@NotNull Term[] terms) {
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
        @Override public final Term apply(@NotNull Term[] x) {
            if (x.length!=1)
                return null;
                //throw new UnsupportedOperationException("# args must equal 1");

            return apply(x[0]);
        }

        @Nullable
        public abstract Term apply(Term x);
    }

    /**
     * Created by me on 12/12/15.
     */
    public abstract static class BinaryFunctor extends Functor {

        protected BinaryFunctor(@NotNull String id) {
            super(id);
        }

        @Nullable
        @Override public final Term apply(@NotNull Term[] x) {
            if (x.length!=2)
                throw new UnsupportedOperationException("# args must equal 2");

            return apply(x[0], x[1]);
        }

        @Nullable
        public abstract Term apply(Term a, Term b);
    }
}
