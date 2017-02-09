package nars.term.container;

import com.google.common.collect.Iterators;
import nars.Op;
import nars.term.Term;
import nars.term.Termlike;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Size 1 TermVector
 */
public class TermVector1 implements TermContainer, Set<Term> {

    public final Term the;

    public TermVector1(Term the) {
        this.the = the;
    }

    @Override
    public int hashCode() {
        return 31 + the.hashCode(); //HACK consistent with Terms.hash(..)
    }



    @Override
    public boolean equals(@NotNull Object obj) {
        return
                (this == obj)
                        ||
                ((obj instanceof TermContainer) && equalTo((TermContainer)obj));
    }

    @NotNull
    @Override
    public Term[] terms() {
        return new Term[] {  the };
    }

    @Override
    public final @NotNull Term term(int i) {
        if (i!=0)
            throw new ArrayIndexOutOfBoundsException();
        return the;
    }

    @Override
    public boolean isTerm(int i, @NotNull Op o) {
        return i == 0 && the.op() == o;
    }


    /** vol and complexity are reported as if they were already part of an enclosing Compound */
    @Override public int volume() {
        return the.volume() + 1;
    }

    /** vol and complexity are reported as if they were already part of an enclosing Compound */
    @Override public int complexity() {
        return the.complexity() + 1;
    }

    @Override
    public int structure() {
        return the.structure();
    }

    @Override
    public int size() {
        return 1;
    }


    @Override @NotNull public Set<Term> toSet() {
        return this; //this is why this class implements Set
    }

    @NotNull
    @Override
    public String toString() {
        return "(" + the + ')';
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
        return //(hashCode() == b.hashCode()) &&
                (b.size()==1) &&
                the.equals(b.term(0));
    }

    @Override
    public void copyInto(Collection<Term> target) {
        target.add(the);
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
    public int varIndep() {
        return the.varIndep();
    }

    @Override
    public int varDep() {
        return the.varDep();
    }

    @Override
    public int varQuery() {
        return the.varQuery();
    }

    @Override
    public int varPattern() {
        return the.varPattern();
    }

    @Override
    public boolean and(@NotNull Predicate<Term> p) {
        return p.test(the);
    }


}
