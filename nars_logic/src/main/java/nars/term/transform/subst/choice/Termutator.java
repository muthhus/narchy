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
        chain[current+1].run(f, chain, current+1);
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

}
