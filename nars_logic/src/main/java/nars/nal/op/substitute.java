package nars.nal.op;

import nars.$;
import nars.Op;
import nars.nal.meta.PremiseAware;
import nars.nal.meta.PremiseMatch;
import nars.term.Compound;
import nars.term.Term;
import nars.term.TermBuilder;
import nars.term.atom.Atom;
import nars.term.transform.subst.MapSubst;
import nars.term.transform.subst.Subst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/** TODO is this better named "substituteAny" */
public class substitute extends ImmediateTermTransform implements PremiseAware {

    public static final Atom INDEP_VAR = $.the("$", true);
    public static final Atom QUERY_VAR = $.the("?", true);
    public static final Atom DEP_VAR = $.the("#", true);

    @NotNull
    @Override
    public Term function(Compound x, TermBuilder i) {
        throw new RuntimeException("n/a");
    }

    @Nullable
    @Override
    public Term function(@NotNull Compound p, @NotNull PremiseMatch r) {
        final Term[] xx = p.terms();

        //term to possibly transform
        final Term term = xx[0];

        //original term (x)
        final Term x = xx[1];

        //replacement term (y)
        final Term y = xx[2];

        return subst(r, term, x, y);
    }

    @Nullable
    public static Term resolve(@NotNull PremiseMatch r, Term x) {
        Term x2 = r.yx.get(x);
        if (x2 == null)
            x2 = x;
        return x2;
    }

    @Nullable
    public static Term subst(@NotNull PremiseMatch r, Term term, @NotNull Term x, Term y) {
        if (x.equals(y))
            return term;

        x = resolve(r, x);
        y = resolve(r, y);

        MapSubst m = new MapSubst(r.yx);
        m.xy.put(x, y);

        return subst(r, m, term);
    }

    @Nullable
    public static Term subst(@NotNull PremiseMatch r, @NotNull Subst m, Term term) {
        //copy the new mappings to the match
        m.forEach( (k,v) -> {
            if (!r.putXY(k, v)) {
                //throw new RuntimeException("what does this mean");
                r.xy.put(k, v); //HACK
            }
        });
//        if (!m.yx.isEmpty()) {
//            throw new RuntimeException("do these need copied too?");
//        }

        return r.premise.memory().index.apply(m, term);
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


    public static Op getOp(@NotNull Term type) {
        Op o;

        //TODO cache the type
        if (type.equals(INDEP_VAR)) o = Op.VAR_INDEP;
        else if (type.equals(DEP_VAR)) o = Op.VAR_DEP;
        else if (type.equals(QUERY_VAR)) o = Op.VAR_QUERY;
            //...else
        else
            throw new RuntimeException("unrecognizd subst type: " + type);
        return o;
    }

}
