package nars.term.container;

import nars.Op;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.Terms;
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

    public final TermContainer srcsubs;
    private final Random rng;

    public ShuffledSubterms(Random rng, Term[] subterms) {
        this(rng, TermVector.the(subterms));
    }

    public ShuffledSubterms(Random rng, TermContainer subterms) {
        this.rng = rng;
        this.srcsubs = subterms;
        reset();
    }

    @Override
    public int structure() {
        return srcsubs.structure();
    }

    @Override
    public int volume() {
        return srcsubs.volume();
    }

    @Override
    public int complexity() {
        return srcsubs.complexity();
    }

    @Override
    public int size() {
        return srcsubs.size();
    }

    @NotNull
    @Override
    public Term term(int i) {
        return srcsubs.term(super.get(i));
    }

    @Override
    public boolean isTerm(int i, @NotNull Op o) {
        return term(i).op() == o;
    }

    @Override
    public boolean equalTerms(@NotNull TermContainer c) {
        //to compare them in-order
        return TermContainer.equals(this, c);
    }

    @Override
    public String toString() {
        return TermContainer.toString(this);
    }

    @Override
    public boolean impossibleSubTermVolume(int otherTermVolume) {
        return srcsubs.impossibleSubTermVolume(otherTermVolume);
    }

//    @Override
//    public Ellipsis firstEllipsis() {
//        return srcsubs.firstEllipsis();
//    }

    @Override
    public boolean containsTerm(Termlike term) {
        return srcsubs.containsTerm(term);
    }

    @Override
    public void forEach(@NotNull Consumer action, int start, int stop) {
        TermContainer.forEach(this, action, start, stop);
    }

    @Override
    public int varDep() {
        return srcsubs.varDep();
    }

    @Override
    public int varIndep() {
        return srcsubs.varIndep();
    }

    @Override
    public int varQuery() {
        return srcsubs.varQuery();
    }

    @Override
    public int varPattern() {
        return srcsubs.varPattern();
    }

    @Override
    public int vars() {
        return srcsubs.vars();
    }

    @Override
    public boolean equals(Object obj) {
        return TermContainer.equals(this, (TermContainer) obj);
    }

    @Override
    public int hashCode() {
        return Terms.hashSubterms(this);
    }

    @Override
    public int compareTo(@NotNull Object o) {
        return TermContainer.compareTo(this, o);
    }

    @NotNull
    @Override
    public Iterator iterator() {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException(); //TODO maybe is valid
    }


    @Override
    public void addAllTo(@NotNull Collection<Term> set) {
        forEach(set::add);
    }

    public void reset() {
        restart(srcsubs.size(), rng);
    }


}
