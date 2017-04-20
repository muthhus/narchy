package nars.term.container;

import nars.term.Term;
import org.apache.commons.collections4.IteratorUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Size 1 TermVector
 */
public final class TermVector2 extends TermVector {

    public final Term x, y;

    /** uses an array argument so that the input array can be used directly without needing to create a new one when it calls the superclass constructor */
    public TermVector2(Term... xy) {
        super(xy);
        assert(xy.length == 2);
        this.x = xy[0];
        this.y = xy[1];
    }

    @NotNull
    @Override
    public Term[] subtermsArray() {
        return new Term[] { x, y };
    }

    @Override
    public @NotNull Term get(int i) {
        switch (i) {
            case 0: return x;
            case 1: return y;
            default:
                throw new ArrayIndexOutOfBoundsException();
        }
    }

    @Override
    public int size() {
        return 2;
    }

    @Override
    public Iterator<Term> iterator() {
        return IteratorUtils.arrayIterator(subtermsArray());
    }

    @Override
    public void forEach(@NotNull Consumer<? super Term> action, int start, int stop) {
        if (start == 0)
            action.accept(x);
        if (stop == 1)
            action.accept(y);
    }

    @Override
    public void forEach(@NotNull Consumer<? super Term> action) {
        action.accept(x);
        action.accept(y);
    }
}
