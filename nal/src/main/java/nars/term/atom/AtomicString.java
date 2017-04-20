package nars.term.atom;

import nars.index.term.TermIndex;
import nars.term.Term;

/**
 * Created by me on 1/1/16.
 */
public abstract class AtomicString implements Atomic {

    /** Assumes that the op()
     *  is encoded within its string such that additional op()
     *  comparison would be redundant. */
    @Override public boolean equals(Object u) {

        return  (this == u)
                ||
                (
                        hashCode() == u.hashCode() &&
                        (u instanceof Atomic && op() == ((Atomic) u).op())) &&
                        (toString().equals(u.toString())
                );

    }


    @Override
    public int complexity() {
        return 1;
    }

    @Override
    public final int hashCode() {
        return toString().hashCode();
    }

}
