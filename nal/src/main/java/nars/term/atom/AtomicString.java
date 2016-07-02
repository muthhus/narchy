package nars.term.atom;

import nars.term.Termlike;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 1/1/16.
 */
public abstract class AtomicString implements Atomic {

    /** Assumes that the op()
     *  is encoded within its string such that additional op()
     *  comparison would be redundant. */
    @Override public boolean equals(Object u) {
        if (this == u) return true;

        if (u instanceof Atomic) {
            Atomic tu = (Atomic) u;
            return op() == tu.op() && toString().equals(tu.toString());
        }
        return false;
    }


    @Override
    public int complexity() {
        return 1;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * @param that The Term to be compared with the current Term
     */
    @Override public int compareTo(@NotNull Termlike that) {
        if (that==this) return 0;

        if (!(that instanceof Atomic))
            return -1;

        Atomic t = (Atomic)that;
        int d = op().compareTo(t.op());
        if (d!=0)
            return d;

        //if the op is the same, it is required to be a subclass of Atomic
        //which should have an ordering determined by its toString()
        return toString().compareTo((/*(Atomic)*/that).toString());
    }

}
