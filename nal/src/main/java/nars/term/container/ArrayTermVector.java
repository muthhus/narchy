package nars.term.container;

import nars.term.Term;
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
        return Arrays.stream(terms).iterator();
    }

    @Override
    public final void forEach(@NotNull Consumer<? super Term> action, int start, int stop) {
        Term[] tt = terms;
        for (int i = start; i < stop; i++)
            action.accept(tt[i]);
    }

//---

    @Override
    public final void forEach(@NotNull Consumer<? super Term> action) {
        for (Term x : terms)
            action.accept(x);
    }

}
