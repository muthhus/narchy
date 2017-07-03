package nars.term.transform;

import nars.$;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import nars.term.subst.MapSubst1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.Null;


public class substitute extends Functor {


    final static Term STRICT = Atomic.the("strict");

    final static Atom func = (Atom) $.the("substitute");

    public substitute() {
        super(func);
    }

    @Nullable @Override public Term apply(@NotNull TermContainer xx) {

        final Term input = xx.sub(0); //term to possibly transform
        Term x = xx.sub(1); //original term (x)

//        boolean hasYX = !parent.yx.map.isEmpty(); //optimize in case where yx is empty
//        if (hasYX) {
//            @NotNull Term x2 = parent.yxResolve(x);
//            if (x2!=x)
//                System.out.println(x + " "+ x2);
//            x = x2;
//        }

        boolean strict = xx.subEquals(3, STRICT);

        if (strict && (!(input instanceof Compound) || !input.containsRecursively(x)))
            return Null;

        Term y = xx.sub(2); //replacement term (y)
//        if (hasYX) {
//            @NotNull Term y2 = parent.yxResolve(y);
//            if (y2!=y)
//                System.out.println(y + " "+ y2);
//            y = y2;
//        }

        if (x.equals(y)) {
            return strict ? Null : input;
        }

        Term output =
//                (hasYX ?
//                    new MapSubstWithOverride(parent.yx, x, y) :
                    new MapSubst1(x,y) //optimized case
                        .transform(input, $.terms);

        return (output != null) ? output : Null;
    }

}
