package nars.term.container;

import nars.nal.meta.match.Ellipsis;
import nars.term.Term;
import nars.util.math.ShuffledPermutations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.function.Consumer;

/**
 * proxy to a TermContainer providing access to its subterms via a shuffling order
 */
public final class ShuffledSubterms extends ShuffledPermutations implements TermContainer<Term> {

    public final TermContainer source;
    private final Random rng;

    public ShuffledSubterms(Random rng, TermContainer x) {
        this.rng = rng;
        this.source = x;
        reset();
    }

    @Override
    public int structure() {
        return source.structure();
    }

    @Override
    public int volume() {
        return source.volume();
    }

    @Override
    public int complexity() {
        return source.complexity();
    }

    @Override
    public int size() {
        return source.size();
    }

    @Nullable
    @Override
    public Term term(int i) {
        return source.term(get(i));
    }

    @Override
    public boolean equalTerms(TermContainer c) {
        return source.equalTerms(c);
    }

    @Override
    public String toString() {
        return TermContainer.toString(this);
    }

    @Override
    public boolean impossibleSubTermVolume(int otherTermVolume) {
        return source.impossibleSubTermVolume(otherTermVolume);
    }

    @Override
    public Ellipsis firstEllipsis() {
        return source.firstEllipsis();
    }

    @Override
    public boolean containsTerm(Term term) {
        return source.containsTerm(term);
    }

    @Override
    public void forEach(Consumer action, int start, int stop) {
        source.forEach(action, start, stop);
    }

    @Override
    public int varDep() {
        return source.varDep();
    }

    @Override
    public int varIndep() {
        return source.varIndep();
    }

    @Override
    public int varQuery() {
        return source.varQuery();
    }

    @Override
    public int varPattern() {
        return source.varPattern();
    }

    @Override
    public int vars() {
        return source.vars();
    }

    @Override
    public boolean equals(Object obj) {
        return source.equals(obj);
    }

    @Override
    public int hashCode() {
        return source.hashCode();
    }

    @Override
    public int compareTo(@NotNull Object o) {
        return source.compareTo(o);
    }

    @Override
    public Iterator iterator() {
        return source.iterator();
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

    @NotNull
    @Override
    public TermContainer replacing(int subterm, Term replacement) {
        throw new RuntimeException("n/a for shuffle"); //TODO maybe is valid
    }


    @Override
    public void addAllTo(@NotNull Collection<Term> set) {
        forEach(set::add);
    }

    public void reset() {
        restart(source.size(), rng);
    }


}
