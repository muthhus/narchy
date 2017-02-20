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


    @Nullable
    @Override
    public Term apply(@NotNull Term[] xx) {


        //term to possibly transform
        final Term term = xx[0];

        //original term (x)
        final Term x = parent.yxResolve(xx[1]);


        final Term strict = xx.length > 3 ? xx[3] : null;
        if (Objects.equal(strict, STRICT)) {
            if (term instanceof Compound && !((Compound)term).containsTermRecursively(x)) {
                return False;
            }
        }

        //replacement term (y)
        final Term y = parent.yxResolve(xx[2]);

        return parent.transform(term, new MapSubstWithOverride(parent.yx,  x, y));
    }




//
//    @Nullable
//    public static Term resolve(@NotNull PremiseMatch r, Term x) {
//        Term x2 = r.yx.get(x);
//        if (x2 == null) {
//
//            x2 = r.xy.get(x);
//            if (x2!=null)
//                System.out.println(x2);
//        }
//        return (x2 == null) ? x : x2;
//    }


    //    protected boolean substitute(Compound p, MapSubst m, Term a, Term b) {
//        final Term type = p.term(1);
//        Op o = getOp(type);
//
//
//        Random rng = new XorShift128PlusRandom(1);
//
//        FindSubst sub = new FindSubst(o, rng) {
//            @Override public boolean onMatch() {
//                return false;
//            }
//        };
//
//        boolean result = sub.match(a, b);
//        m.subs.putAll(sub.xy); //HACK, use the same map instance
//        return result;
//
////        boolean result;
////        if (sub.match(a, b)) { //matchAll?
////            //m.secondary.putAll(sub.xy);
////            result = true;
////        }
////        else {
////            result = false;
////        }
////
////        return result;
//    }


//    @Nullable
//    public static Op getOp(@NotNull Term type) {
//
//        switch (type.toString()) {
//            case "\"$\"":
//                return Op.VAR_INDEP;
//            case "\"#\"":
//                return Op.VAR_DEP;
//            case "\"?\"":
//                return Op.VAR_QUERY;
//        }
//
//        return null;
//    }

}
