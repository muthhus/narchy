package nars.term.container;

import jcog.list.ArrayIterator;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.Op.Null;

/**
 * Holds a vector or tuple of terms.
 * Useful for storing a fixed number of subterms

 */
public class ArrayTermVector extends TermVector {

    @NotNull
    private Term[] terms;

    public ArrayTermVector(@NotNull Collection<Term> terms) {
        this(terms.toArray(new Term[terms.size()]));
    }

    public ArrayTermVector(@NotNull Term... terms) {
         super(terms);
         this.terms = terms;
    }

    @Override
    public final boolean equals(@NotNull Object obj) {
        if (this == obj) return true;

        @NotNull final Term[] x = this.terms;
        if (obj instanceof ArrayTermVector) {
            //special case for ArrayTermVector fast compare and sharing
            ArrayTermVector c = (ArrayTermVector) obj;
            Term[] y = c.terms;
            if (x == y)
                return true;

            if (hash != c.hash)
                return false;

            int s = x.length;
            if (s != y.length)
                return false;
            for (int i = 0; i < s; i++) {
                Term xx = x[i];
                Term yy = y[i];
                if (xx == yy) {
                    continue;
                } else if (!xx.equals(yy)) {
                    return false;
                } else {
                    x[i] = yy; //share
                }
            }

            terms = y; //share since array is equal

            return true;

        } else if (obj instanceof TermContainer) {

            TermContainer c = (TermContainer) obj;
            if (hash != c.hashCodeSubTerms())
                return false;

            int s = x.length;
            if (s != c.size())
                return false;
            for (int i = 0; i < s; i++)
                if (!x[i].equals(c.sub(i)))
                    return false;
            return true;
        }
        return false;
    }


    @Override
    @NotNull public final Term sub(int i) {
        return terms.length > i ? terms[i] : Null;
    }

    @NotNull @Override public final Term[] toArray() {
        return terms.clone();
    }

    @Override
    public final int size() {
        return terms.length;
    }

    @Override
    public final Iterator<Term> iterator() {
        return new ArrayIterator<>(terms);
    }

    @Override
    public final void forEach(@NotNull Consumer<? super Term> action, int start, int stop) {
        for (int i = start; i < stop; i++)
            action.accept(terms[i]);
    }

    @Override
    public final void forEach(@NotNull Consumer<? super Term> action) {
        for (Term x : terms)
            action.accept(x);
    }

    @Override
    public final boolean OR(@NotNull Predicate<Term> p) {
        for (Term i : terms)
            if (p.test(i))
                return true;
        return false;
    }

    @Override public final boolean AND(@NotNull Predicate<Term> p) {
        for (Term i : terms)
            if (!p.test(i))
                return false;
        return true;
    }

    @Override public final void recurseTerms(@NotNull Consumer<Term> v) {
        for (Term sub : terms)
            sub.recurseTerms(v);
    }


}
