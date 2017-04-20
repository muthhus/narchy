package nars.term.transform;

import nars.Op;
import nars.premise.Derivation;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import nars.term.subst.MapSubstWithOverride;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public final class substitute extends Functor {

    @NotNull private final Derivation parent;

    final static Term STRICT = Atomic.the("strict");

    public substitute(@NotNull Derivation parent) {
        super("substitute");
        this.parent = parent;
    }

    @Nullable @Override public Term apply(@NotNull TermContainer xx) {

        final Term term = xx.get(0); //term to possibly transform

        final Term x = parent.yxResolve(xx.get(1)); //original term (x)

        if (term instanceof Compound && (xx.size() > 3) && xx.get(3).equals(STRICT) && !term.containsTermRecursively(x)) {
            return Op.False;
        }

        final Term y = parent.yxResolve(xx.get(2)); //replacement term (y)

        Term z = parent.transform(term, new MapSubstWithOverride(parent.yx,  x, y));
        if (z != null) {
            return z;
        }

        return Op.False;
    }

}
