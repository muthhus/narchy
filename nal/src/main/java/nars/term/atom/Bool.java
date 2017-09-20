package nars.term.atom;

import nars.Op;
import nars.term.Term;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;

import static nars.Op.BOOL;
import static nars.Op.Null;


/** special/reserved/keyword representing fundamental absolute boolean truth states:
 *      True - absolutely true
 *      False - absolutely false
 *      Null - absolutely nonsense
 *
 *  these represent an intrinsic level of truth that exist within the context of
 *  an individual term.  not to be confused with Task-level Truth
 */
abstract public class Bool extends AtomicConst {

    private final String id;

    protected Bool(@NotNull String id) {
        super(BOOL, id);
        this.id = id;
    }

    @Override
    public /*@NotNull*/ Op op() {
        return BOOL;
    }



    @Override
    public String toString() {
        return id;
    }

    @Override
    abstract public int opX();

    @NotNull
    @Override
    abstract public Term unneg();

    @Override
    public final boolean equals(Object u) {
        return u == this;
    }


    @Override
    @NotNull
    public final Term conceptual() {
        return Null;
    }

    @Override
    public int compareTo(@NotNull Term y) {
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
    public final boolean unify(@NotNull Term y, @NotNull Unify subst) {
        throw never("unify");
    }

    @Override
    public final Term dt(int dt) {
        return this; //allow
        //throw never("dt");
    }


    UnsupportedOperationException never(String eval) {
        return new UnsupportedOperationException(this + " Bool leak attemping: " + eval);
    }

}
