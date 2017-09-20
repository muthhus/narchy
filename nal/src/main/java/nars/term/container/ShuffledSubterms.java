package nars.term.container;

import jcog.math.ShuffledPermutations;
import nars.term.Term;
import nars.term.Terms;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Random;

/**
 * proxy to a TermContainer providing access to its subterms via a shuffling order
 * warning: don't use as subterms of a Compound
 */
public final class ShuffledSubterms extends ShuffledPermutations implements TermContainer {

    public final TermContainer srcsubs;

    public ShuffledSubterms(Random rng, Term[] subterms) {
        this(rng, TermVector.the(subterms) /* must be unique, private instance */);
    }

    public ShuffledSubterms(Random rng, TermContainer subterms) {
        this.srcsubs = subterms;
        reset(rng);
    }

    @Override
    public void init(@NotNull int[] meta) {
        throw new UnsupportedOperationException();
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
    public int subs() {
        return srcsubs.subs();
    }

    @NotNull
    @Override
    public Term sub(int i) {
        return srcsubs.sub(permute(i));
    }


//    @Override
//    public boolean equalTerms(@NotNull TermContainer c) {
//        //to compare them in-order
//        return TermContainer.equ(this, c);
//    }

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
        if (this == obj) return true;
        if (!(obj instanceof TermContainer)) return false;

        TermContainer c = (TermContainer) obj;

        int s = subs();
        if (s != c.subs())
            return false;
        for (int i = 0; i < s; i++) {
            if (!sub(i).equals(c.sub(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Terms.hashSubterms(this);
    }

    @NotNull
    @Override
    public Iterator iterator() {
        throw new UnsupportedOperationException();
    }








    protected void reset(Random rng) {
        restart(srcsubs.subs(), rng);
    }


}
