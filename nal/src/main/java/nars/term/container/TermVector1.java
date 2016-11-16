package nars.term.container;

import com.google.common.collect.Iterators;
import nars.term.Term;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Size 1 TermVector
 */
public class TermVector1 extends TermVector {

    public final Term the;

    public TermVector1(Term the) {
        super(the);
        this.the = the;
    }

    @NotNull
    @Override
    public Term[] terms() {
        return new Term[] {  the };
    }

    @Override
    public @NotNull Term term(int i) {
        if (i!=0)
            throw new ArrayIndexOutOfBoundsException();
        return the;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public Iterator<Term> iterator() {
        return Iterators.singletonIterator(the);
    }

    @Override
    public void forEach(@NotNull Consumer<? super Term> action, int start, int stop) {
        if (start != 0 || stop!= 0)
            throw new ArrayIndexOutOfBoundsException();
        forEach(action);
    }

    @Override
    public void forEach(@NotNull Consumer<? super Term> action) {
        action.accept(the);
    }
}
