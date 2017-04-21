package nars.term.transform;

import jcog.version.VersionMap;
import nars.Op;
import nars.premise.Derivation;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import nars.term.subst.MapSubst;
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

        final Term input = xx.get(0); //term to possibly transform
        Term x = xx.get(1); //original term (x)

        boolean hasYX = !parent.yx.map.isEmpty(); //optimize in case where yx is empty
        if (hasYX)
            x = parent.yxResolve(xx.get(1));

        boolean strict = (xx.size() > 3) && xx.get(3).equals(STRICT);

        if (strict && (!(input instanceof Compound) || !input.containsTermRecursively(x)))
            return Op.False;

        Term y = xx.get(2); //replacement term (y)
        if (hasYX)
            y = parent.yxResolve(xx.get(2));

        Term output = parent.transform(input,
            hasYX ?
                new MapSubstWithOverride(parent.yx, x, y) :
                new MapSubst1(x,y)  //optimized impl
        );
        return (output != null) ? output : Op.False;
    }

}
