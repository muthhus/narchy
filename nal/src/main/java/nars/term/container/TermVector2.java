package nars.term.container;

import jcog.list.ArrayIterator;
import nars.Op;
import nars.term.Term;
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
    public Term[] toArray() {
        return new Term[] { x, y };
    }

    @Override
    public @NotNull Term sub(int i) {
        switch (i) {
            case 0: return x;
            case 1: return y;
            default:
                throw new ArrayIndexOutOfBoundsException();
        }
    }

    @Override
    public boolean subIs(int i, Op o) {
        switch (i) {
            case 0: return x.op()==o;
            case 1: return y.op()==o;
            default:
                return false;
                //throw new ArrayIndexOutOfBoundsException();
        }
    }

    @Override
    public boolean subIs(int i, Term maybeEquals) {
        switch (i) {
            case 0: return x.equals(maybeEquals);
            case 1: return y.equals(maybeEquals);
            default:
                return false;
                //throw new ArrayIndexOutOfBoundsException();
        }
    }

    @Override
    public boolean equals(@NotNull Object obj) {
        if (this == obj) return true;
        if (obj instanceof TermContainer) {
            if (hash == obj.hashCode()) {
                TermContainer t = (TermContainer) obj;
                return (t.size() == 2 && t.subIs(0, x) && t.subIs(1, y));
            }
        }
        return false;
    }

    @Override
    public int size() {
        return 2;
    }

    @Override
    public Iterator<Term> iterator() {
        return new ArrayIterator(toArray());
    }

    @Override
    public void forEach(@NotNull Consumer<? super Term> action, int start, int stop) {
        if (start == stop) {
            action.accept( start == 0 ? x : y );
        } else if (start == 0 && stop == 1) {
            action.accept(x);
            action.accept(y);
        }
    }

    @Override
    public void forEach(@NotNull Consumer<? super Term> action) {
        action.accept(x);
        action.accept(y);
    }

    @Override public void recurseTerms(@NotNull Consumer<Term> v) {
        x.recurseTerms(v);
        y.recurseTerms(v);
    }

}
