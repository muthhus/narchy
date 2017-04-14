package nars.term.mutate;

import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * AIKR choicepoint used in deciding possible mutations to apply in deriving new compounds
 */
public abstract class Termutator  {

    /** should have .equals() consistency */
    @NotNull
    public final Object key;

    protected Termutator(@NotNull Object key) {
        this.key = key;
    }

    /** match all termutations recursing to the next after each successful one */
    public abstract void mutate(Unify f, List<Termutator> chain, int current);

    public abstract int getEstimatedPermutations();


    @Override
    public final boolean equals(@NotNull Object obj) {
        return (this == obj) || key.equals(((Termutator)obj).key);
    }

    @Override
    public final int hashCode() {
        return key.hashCode();
    }

}
