package nars.index.term;

import nars.Op;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * a lightweight Compound builder
 *      - fast-write
 *      - hashed (for use as a key, etc)
 *
 * used for tentative construction of terms during critical
 * derivation and other purposes.
 *
 */
public interface ProtoCompound extends TermContainer {

    Op op();

    int dt();

    /** returns true if the predicate is true for all items */
    boolean AND(@NotNull Predicate<Term> t);

    /** returns true if the predicate is true for any items */
    boolean OR(@NotNull Predicate<Term> t);

    /** subterms as an array for construction */
    @NotNull Term[] subterms();

    /** number subterms */
    int size();

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();

    Term sub(int i);
}
