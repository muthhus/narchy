package nars.concept;

import nars.Task;
import nars.bag.Bag;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.transform.TermTransform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static nars.$.the;


abstract public class Functor extends AtomConcept implements TermTransform, PermanentConcept {

    public Functor(@NotNull String atom) {
        this(fName(atom));
    }

    public Functor(@NotNull Atom atom) {
        this(atom, Bag.EMPTY, Bag.EMPTY);
    }

    public Functor(@NotNull Atom atom, Bag<Term> termLinks, Bag<Task> taskLinks) {
        super(atom, termLinks, taskLinks);
    }

    public static Atom fName(@NotNull String termAtom) {
        return (Atom)the(termAtom);
    }

    /** creates a new functor from a term name and a lambda */
    public static Concept f(@NotNull String termAtom, @NotNull Function<Term[], Term> f) {
        return f(fName(termAtom), f);
    }

    /** creates a new functor from a term name and a lambda */
    public static Concept f(@NotNull Atom termAtom, @NotNull Function<Term[], Term> f) {
        return new LambdaFunctor(termAtom, f);
    }

    public static Concept f(@NotNull Atom termAtom, int arityRequired, @NotNull Function<Term[], Term> ff) {
        return f(termAtom, (tt)->{
            if (tt.length!=arityRequired)
                throw new RuntimeException(termAtom + " requires " + arityRequired + " arguments: " + tt);
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

    /** two argument functor (convenience method) */
    public static Concept f2(@NotNull Atom termAtom, @NotNull BiFunction<Term, Term, Term> ff) {
        return f(termAtom, 2, (tt)-> ff.apply(tt[0], tt[1]));
    }
    /** two argument functor (convenience method) */
    public static Concept f2(@NotNull String termAtom, @NotNull BiFunction<Term, Term, Term> ff) {
        return f2(fName(termAtom), ff);
    }

    public static final class LambdaFunctor extends Functor {

        @NotNull private final Function<Term[], Term> f;

        public LambdaFunctor(@NotNull Atom termAtom, @NotNull Function<Term[], Term> f) {
            super(termAtom);
            this.f = f;
        }

        @Nullable @Override public Term apply(@NotNull Term[] terms) {
            return f.apply(terms);
        }
    }

}
