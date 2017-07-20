package nars.term.atom;

import nars.Op;
import nars.index.term.TermContext;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;

import static nars.Op.ATOM;


/** special/reserved/keyword representing fundamental absolute boolean truth states:
 *      True - absolutely true
 *      False - absolutely false
 *      Null - nonsense
 *
 *  these represent an intrinsic level of truth that exist within the context of
 *  an individual term.  not to be confused with Task-level Truth
 */
abstract public class Bool extends AtomicToString {

    private final String id;

    protected Bool(@NotNull String id) {
        super(ATOM, id);
        this.id = id;
    }

    @Override
    public @NotNull Op op() {
        return ATOM;
    }

    @Override
    public String toString() {
        return id;
    }

    public final static int AtomBool = Term.opX(ATOM, 0);
    @Override public final int opX() {
        return AtomBool;
    }

    abstract public Term unneg();

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
                throw never("compare as Atom");
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
        throw never("unify");
    }

    @Override
    public Term dt(int dt) {
        throw never("dt");
    }


    static UnsupportedOperationException never(String eval) {
        return new UnsupportedOperationException("Bool leak attemping: " + eval);
    }

}
