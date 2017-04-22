package nars.term.container;

import nars.term.Term;
import org.apache.commons.collections4.iterators.ArrayIterator;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Holds a vector or tuple of terms.
 * Useful for storing a fixed number of subterms

 */
public class ArrayTermVector extends TermVector {


    @NotNull
    public final Term[] terms;

    public ArrayTermVector(@NotNull Term... terms) {
         super(terms);
         this.terms = terms;
    }

    /**
     * size should already be known equal
     */
    @Override
    public final boolean equalTerms(@NotNull TermContainer c) {

        int s = terms.length;
        if (s !=c.size())
            return false;
        for (int i = 0; i < s; i++) {
            Term y = c.sub(i);
            Term x = terms[i];

            if (x == y) {
                //continue;
            } else if (!x.equals(y)) {
                return false;
            } else {
                //share the ref
                terms[i] = y;
            }
        }
        return true;
    }


    @Override
    @NotNull public final Term sub(int i) {
        return terms[i];
    }

    @NotNull @Override public final Term[] toArray() {
        return terms;
    }

    @Override
    public final int size() {
        return terms.length;
    }

    @Override
    public final Iterator<Term> iterator() {
        return new ArrayIterator<Term>(terms);
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
}
