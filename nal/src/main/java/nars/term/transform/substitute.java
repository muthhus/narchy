package nars.term.transform;

import com.google.common.base.Objects;
import nars.$;
import nars.premise.Derivation;
import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.MapSubstWithOverride;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public final class substitute extends Functor {

    @NotNull private final Derivation parent;

    final static Term STRICT = $.the("strict");

    public substitute(@NotNull Derivation parent) {
        super("substitute");
        this.parent = parent;
    }

    @Nullable @Override public Term apply(@NotNull Term[] xx) {

        final Term term = xx[0]; //term to possibly transform

        final Term x = parent.yxResolve(xx[1]); //original term (x)

        if (term instanceof Compound && (xx.length > 3) && xx[3].equals(STRICT) && !((Compound) term).containsTermRecursively(x)) {
            return False;
        }

        final Term y = parent.yxResolve(xx[2]); //replacement term (y)

        return parent.transform(term, new MapSubstWithOverride(parent.yx,  x, y));
    }

}
