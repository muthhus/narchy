package nars.term.atom;

import jcog.Util;

/**
 * an Atomic impl which relies on the value provided by toString()
 */
public abstract class ToStringAtomic implements Atomic {

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
    public int hashCode() {
        //return toString().hashCode();
        //return Util.hashCombine(toString().hashCode(), op().bit);
        return Util.hashWangJenkins( toString().hashCode() );
    }

}
