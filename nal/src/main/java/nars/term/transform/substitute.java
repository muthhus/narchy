package nars.term.transform;

import nars.Op;
import nars.premise.Derivation;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import nars.term.subst.MapSubst1;
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

        final Term input = xx.sub(0); //term to possibly transform
        Term x = xx.sub(1); //original term (x)

        boolean hasYX = !parent.yx.map.isEmpty(); //optimize in case where yx is empty
        if (hasYX)
            x = parent.yxResolve(xx.sub(1));

        boolean strict = (xx.size() > 3) && xx.sub(3).equals(STRICT);

        if (strict && (!(input instanceof Compound) || !input.containsRecursively(x)))
            return Op.False;

        Term y = xx.sub(2); //replacement term (y)
        if (hasYX)
            y = parent.yxResolve(xx.sub(2));

        Term output = parent.transform(input,
            hasYX ?
                new MapSubstWithOverride(parent.yx, x, y) :
                new MapSubst1(x,y)  //optimized impl
        );
        return (output != null) ? output : Op.False;
    }

}
