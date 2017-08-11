package nars.term.subst;

import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 1-pair substitution
 */
public class MapSubst1 implements Subst {

    private final Term from;
    private final Term to;

    /**
     * creates a substitution of one variable; more efficient than supplying a Map
     */
    public MapSubst1(@NotNull Term from, @NotNull Term to) {
        assert(!from.equals(to)): "pointless substitution";

        this.from = from;
        this.to = to;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public @Nullable Term xy(Term t) {
        return t.equals(from) ? to : null;
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

//    @Override
//    public boolean put(@NotNull Unify copied) {
//        throw new UnsupportedOperationException();
//    }
}
