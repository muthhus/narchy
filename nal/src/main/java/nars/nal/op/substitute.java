package nars.nal.op;

import nars.$;
import nars.Op;
import nars.nal.meta.PremiseAware;
import nars.nal.meta.PremiseEval;
import nars.term.Compound;
import nars.term.Term;
import nars.term.TermIndex;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.term.transform.subst.MapSubst;
import nars.term.transform.subst.Subst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/** TODO is this better named "substituteAny" */
public class substitute extends ImmediateTermTransform implements PremiseAware {


    @NotNull
    @Override
    public Term function(Compound x, TermIndex i) {
        throw new RuntimeException("n/a");
    }

    @Nullable
    @Override
    public Term function(@NotNull Compound p, @NotNull PremiseEval r) {
        final Term[] xx = p.terms();

        //term to possibly transform
        final Term term = xx[0];

        //original term (x)
        final Term x = xx[1];

        //replacement term (y)
        final Term y = xx[2];

        return subst(r, term, x, y);
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

    @Nullable
    public static final Term resolve(@NotNull PremiseEval r, Term x) {
        //TODO make a half resolve that only does xy?

        Term ret = r.yx.get(x);
        if(ret != null) {
//            Term ret2 = r.xy.get(ret);
//            if (ret2!=null)
//                return ret2;
//            else
                return ret;
        }
        return x;

    }

    @Nullable
    public static Term subst(@NotNull PremiseEval r, @NotNull Term term, @NotNull Term x, Term y) {
//        if (x.equals(y))
//            return term;

        x = resolve(r, x);
        y = resolve(r, y);

        return resolve(r, new MapSubst.MapSubstWithOverride(r.yx, x, y), term);
    }



    public
    @Nullable
    static Term resolve(@NotNull PremiseEval r, @NotNull Subst m, @NotNull Term term) {
        Termed resolved = r.premise.nar().index.apply(m, term);
        return Termed.termOrNull(resolved);
    }

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


    @Nullable
    public static Op getOp(@NotNull Term type) {

        switch (type.toString()) {
            case "\"$\"":
                return Op.VAR_INDEP;
            case "\"#\"":
                return Op.VAR_DEP;
            case "\"?\"":
                return Op.VAR_QUERY;
        }

        return null;
    }

}
