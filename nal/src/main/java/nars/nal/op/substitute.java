package nars.nal.op;

import nars.nal.meta.PremiseEval;
import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.MapSubst;
import org.jetbrains.annotations.NotNull;


public final class substitute extends TermTransformOperator  {

    @NotNull private final PremiseEval parent;

    public substitute(@NotNull PremiseEval parent) {
        super("substitute");
        this.parent = parent;
    }


    @NotNull
    @Override
    public Term function(@NotNull Compound p) {

        final Term[] xx = p.terms();

        //term to possibly transform
        final Term term = xx[0];

        //original term (x)
        final Term x = xx[1];

        //replacement term (y)
        final Term y = xx[2];


        return parent.resolve(term,
                new MapSubst.MapSubstWithOverride(parent.yx,
                        parent.yxResolve(x),
                        parent.yxResolve(y)));
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
