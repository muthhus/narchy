package nars.term.container;

import nars.term.Term;
import org.apache.commons.collections4.iterators.ArrayIterator;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;

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
    public final boolean equalTerms(@NotNull TermContainer c) {

        int s = terms.length;
        if (s !=c.size())
            return false;
        for (int i = 0; i < s; i++) {
            Term y = c.term(i);
            Term x = terms[i];

            if (x == y)
                continue;

            if (!x.equals(y)) {
                return false;
            } else {
                //share the ref
                terms[i] = y;
            }
        }
        return true;
    }

    public final boolean equivalent(@NotNull TermContainer c) {
        return (hash == c.hashCodeSubTerms()) && equalTerms(c);
    }

    @Override
    @NotNull public final Term term(int i) {
        return terms[i];
    }

    @NotNull @Override public final Term[] terms() {
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

}
