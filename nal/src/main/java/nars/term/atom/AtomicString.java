package nars.term.atom;

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
            if (toString().equals(u.toString()) && op()!=tu.op())
                throw new RuntimeException("inconsistent op");

            return toString().equals(tu.toString()) && op() == tu.op();
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


}
