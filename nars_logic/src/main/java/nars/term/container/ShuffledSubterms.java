package nars.term.container;

import nars.term.Term;
import nars.util.math.ShuffledPermutations;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.function.Consumer;

/**
 * proxy to a TermContainer providing access to its subterms via a shuffling order
 */
public final class ShuffledSubterms extends ShuffledPermutations implements TermContainer<Term> {

    public final TermContainer compound;
    private final Random rng;

    public ShuffledSubterms(Random rng, TermContainer x) {
        this.rng = rng;
        this.compound = x;
        reset();
    }

    @Override
    public int structure() {
        return compound.structure();
    }

    @Override
    public int volume() {
        return compound.volume();
    }

    @Override
    public int complexity() {
        return compound.complexity();
    }

    @Override
    public int size() {
        return compound.size();
    }

    @Override
    public Term term(int i) {
        return compound.term(get(i));
    }

    @Override
    public String toString() {
        return TermContainer.toString(this);
    }

    @Override
    public boolean impossibleSubTermVolume(int otherTermVolume) {
        return compound.impossibleSubTermVolume(otherTermVolume);
    }

    @Override
    public boolean hasEllipsis() {
        return compound.hasEllipsis();
    }

    @Override
    public boolean containsTerm(Term term) {
        return compound.containsTerm(term);
    }

    @Override
    public void forEach(Consumer action, int start, int stop) {
        compound.forEach(action, start, stop);
    }

    @Override
    public int varDep() {
        return compound.varDep();
    }

    @Override
    public int varIndep() {
        return compound.varIndep();
    }

    @Override
    public int varQuery() {
        return compound.varQuery();
    }

    @Override
    public int vars() {
        return compound.vars();
    }

    @Override
    public boolean equals(Object obj) {
        return compound.equals(obj);
    }

    @Override
    public int hashCode() {
        return compound.hashCode();
    }

    @Override
    public int compareTo(@NotNull Object o) {
        return compound.compareTo(o);
    }

    @Override
    public Iterator iterator() {
        return compound.iterator();
    }


    @NotNull
    @Override
    public Term[] terms() {
        int s = size();
        Term[] x = new Term[s];
        for (int i = 0; i < s; i++)
            x[i] = term(i);
        return x;
    }

    @Override
    public TermContainer replacing(int subterm, Term replacement) {
        throw new RuntimeException("n/a for shuffle"); //TODO maybe is valid
    }


    @Override
    public void addAllTo(@NotNull Collection<Term> set) {
        forEach(set::add);
    }

    public void reset() {
        restart(compound.size(), rng);
    }


}
