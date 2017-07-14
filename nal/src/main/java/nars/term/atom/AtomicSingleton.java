package nars.term.atom;

import nars.index.term.TermContext;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;


/** special */
public class AtomicSingleton extends Atom {

    public AtomicSingleton(@NotNull String id) {
        super(id);
    }

    @Override
    public final boolean equals(Object u) {
        return u == this;
    }

    @Override
    public int compareTo(@NotNull Termlike y) {
        if (this == y) {
            return 0;
        } else {
            int c = super.compareTo(y);
            if (c == 0) {
                throw new RuntimeException("AtomicSingleton leak");
            }
            return c;
        }
    }


    @Override
    public Term eval(TermContext index) {
        return this;
    }

    @Override
    public boolean unify(@NotNull Term y, @NotNull Unify subst) {
        return this == y;
        //throw new UnsupportedOperationException("AtomicSingleton leak");
        //return false;
    }

}
