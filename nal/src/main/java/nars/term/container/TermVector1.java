package nars.term.container;

import com.google.common.collect.Iterators;
import nars.term.Term;
import nars.term.Termlike;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Size 1 TermVector
 */
public final class TermVector1 extends TermVector implements Set<Term> {

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


    @Override @NotNull public Set<Term> toSet() {
        return this; //this is why this class implements Set
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(Object o) {
        return the.equals(o);
    }

    @Override
    public Iterator<Term> iterator() {
        return Iterators.singletonIterator(the);
    }

    @NotNull
    @Override
    public Term[] toArray() {
        return terms();
    }

    @NotNull
    @Override
    public <T> T[] toArray(T[] ts) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(Term term) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        int cs = collection.size();
        switch (cs) {
            case 0:
                throw new UnsupportedOperationException(); //?
            case 1:
                return the.equals(collection.iterator().next());
        }
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends Term> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
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

    @Override public boolean equalTo(@NotNull TermContainer b) {
        return (hash == b.hashCode()) &&
                //(structure() == b.structure()) &&
                //(volume() == b.volume()) &&
                (b.size()==1) &&
                the.equals(b.term(0));
    }

    @Override
    public boolean containsTerm(@NotNull Termlike t) {
        return the.equals(t);
    }

    @Override
    public boolean or(@NotNull Predicate<Term> p) {
        return p.test(the);
    }

    @Override
    public boolean and(@NotNull Predicate<Term> p) {
        return p.test(the);
    }


}
