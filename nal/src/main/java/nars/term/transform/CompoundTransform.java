package nars.term.transform;

import nars.$;
import nars.Op;
import nars.control.Derivation;
import nars.derive.match.EllipsisMatch;
import nars.index.term.NewCompound;
import nars.index.term.TermContext;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermContainer;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;

/** I = input term type, T = transformable subterm type */
public interface CompoundTransform extends TermContext {


    /** transforms non-compound subterms */
    @Override default @Nullable Termed apply(Term t) {
        return t;
    }

    /**
     * change all query variables to dep vars
     */
    CompoundTransform queryToDepVar = new CompoundTransform() {
        @Override
        public Term apply(Term subterm) {
            if (subterm.op() == VAR_QUERY) {
                return $.varDep((((Variable) subterm).id()));
            }
            return subterm;
        }
    };

    default int dt(Compound c) {
        return c.dt();
    }

    default Term transform(Compound x, Op op, int dt) {

        boolean boolFilter = !op.allowsBool;

        @NotNull TermContainer srcSubs = x.subterms(); //for faster access, generally

        int s = srcSubs.subs(), subtermMods = 0;

        NewCompound target = new NewCompound(op, s);

        for (int i = 0; i < s; i++) {

            Term y0 = srcSubs.sub(i);

            Term y = y0.transform(this); //x instanceof Compound ? x.transform(t) : t.apply(this, x);

            if (Term.invalidBoolSubterm(y, boolFilter)) {
                return null;
            }

            if (y instanceof EllipsisMatch) {
                EllipsisMatch xx = (EllipsisMatch) y;
                int xxs = xx.subs();
                for (int j = 0; j < xxs; j++) {
                    @Nullable Term k = xx.sub(j).transform(this);
                    if (Term.invalidBoolSubterm(k, boolFilter)) {
                        return null;
                    } else {
                        target.add(k);
                    }
                }
                subtermMods += xxs;
            } else {
                if (y != y0 /*&& !y.equals(x)*/) {
                    subtermMods++;
                } /*else {
                    y = x;
                }*/

                target.add(y);
            }


        }


        //TODO does it need to recreate the container if the dt has changed because it may need to be commuted ... && (superterm.dt()==dt) but more specific for the case: (XTERNAL -> 0 or DTERNAL)

        //        if (subtermMods == 0 && !opMod && dtMod && (op.image || (op.temporal && concurrent(dt)==concurrent(src.dt()))) ) {
//            //same concurrency, just change dt, keep subterms
//            return src.dt(dt);
//        }
        Term y;
        Op xo = x.op();
        if (subtermMods > 0 || op != xo/* || dt != src.dt()*/) {

            //if (target.internable())
            y = op.the(dt, target.theArray());
            //else
            //return Op.compound(op, target.theArray(), false).dt(dt); //HACK

        } else {
            y = xo.temporal ? x.dt(dt) : x;
        }

        if (y instanceof Compound && this instanceof Derivation && y.op() == INH && y.subIs(1, ATOM) && y.subIs(0, PROD)) {
            return y.eval(((TermContext) this));
        }

        return y;

    }

//    CompoundTransform Identity = (parent, subterm) -> subterm;

//    CompoundTransform<Compound,Term> None = new CompoundTransform<Compound,Term>() {
//        @Override
//        public boolean test(Term o) {
//            return true;
//        }
//
//        @Nullable
//        @Override
//        public Term apply(Compound parent, Term subterm) {
//            return subterm;
//        }
//    };

}
