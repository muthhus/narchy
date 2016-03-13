package nars.term.transform.subst.choice;

import nars.$;
import nars.term.Termlike;
import nars.term.transform.subst.FindSubst;
import org.jetbrains.annotations.NotNull;

/**
 * AIKR choicepoint used in deciding possible mutations to apply in deriving new compounds
 */
public abstract class Termutator  {

    public final Termlike key;
    private String asStringCached;

    public Termutator(String key) {
        this($.the(key));
    }

    public Termutator(Termlike key) {
        this.key = key;
    }

    /** match all termutations recursing to the next after each successful one */
    public abstract void run(FindSubst f, Termutator[] chain, int current);

    /** call this to invoke the next termutator in the chain */
    protected static void next(FindSubst f, Termutator[] chain, int current) {
        int next = current + 1;
        chain[next].run(f, chain, next);
    }

    public abstract int getEstimatedPermutations();


    @Override
    public final boolean equals(@NotNull Object obj) {
        if (this == obj) return true;
        return key.equals(((Termutator)obj).key);
    }

    @Override
    public final int hashCode() {
        return key.hashCode();
    }

    public final String toStringCached() {
        String s = this.asStringCached;
        if (s == null)
            s = this.asStringCached = toString();
        return s;
    }

}
