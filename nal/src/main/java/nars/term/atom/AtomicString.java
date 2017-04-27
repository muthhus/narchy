package nars.term.atom;

import jcog.Util;

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
                        u instanceof Atomic &&
                        hashCode() == u.hashCode() &&
                        op() == ((Atomic) u).op()) &&
                        toString().equals(u.toString()
                );

    }

    @Override abstract public String toString();

    @Override
    public int complexity() {
        return 1;
    }

    @Override
    public final int hashCode() {
        //return toString().hashCode();
        //return Util.hashCombine(toString().hashCode(), op().bit);
        return Util.hashWangJenkins( toString().hashCode() );
    }

}
