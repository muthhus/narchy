package nars.term.transform;

import nars.$;
import nars.Op;
import nars.premise.Derivation;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import nars.term.subst.MapSubst1;
import nars.term.subst.MapSubstWithOverride;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.False;


public final class substitute extends Functor {

    @NotNull private final Derivation parent;

    final static Term STRICT = Atomic.the("strict");

    final static Atom func = (Atom) $.the("substitute");

    public substitute(@NotNull Derivation parent) {
        super(func);
        this.parent = parent;
    }

    @Nullable @Override public Term apply(@NotNull TermContainer xx) {

        final Term input = xx.sub(0); //term to possibly transform
        Term x = xx.sub(1); //original term (x)

        boolean hasYX = !parent.yx.map.isEmpty(); //optimize in case where yx is empty
        if (hasYX)
            x = parent.yxResolve(x);

        boolean strict = xx.subEquals(3, STRICT);

        if (strict && (!(input instanceof Compound) || !input.containsRecursively(x)))
            return False;

        Term y = xx.sub(2); //replacement term (y)
        if (hasYX)
            y = parent.yxResolve(y);

        if (x.equals(y)) {
            return strict ? False : input;
        }

        Term output =
                (hasYX ?
                    new MapSubstWithOverride(parent.yx, x, y) :
                    new MapSubst1(x,y)) //optimized case
                        .transform(input, parent.index);

        return (output != null) ? output : False;
    }

}
