package nars.index.term;

import jcog.list.FasterList;
import nars.Op;
import nars.term.Term;

import java.util.Arrays;
import java.util.function.Predicate;

/**
 * a lightweight Compound builder
 *      - fast-write
 *      - hashed (for use as a key, etc)
 *
 * used for tentative construction of terms during critical
 * derivation and other purposes.
 */
public interface ProtoCompound  {

    Op op();

    int dt();

    /** returns true if the predicate is true for all items */
    boolean AND(Predicate<Term> t);

    /** returns true if the predicate is true for any items */
    boolean OR(Predicate<Term> t);

    /** subterms as an array for construction */
    Term[] subterms();

    /** number subterms */
    int size();

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();

    Term sub(int i);
}
