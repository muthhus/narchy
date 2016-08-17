package nars.nal.op;

import nars.$;
import nars.Op;
import nars.nal.meta.PremiseEval;
import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.OneMatchFindSubst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.NEG;
import static nars.nal.TermBuilder.False;

/**
 * substituteIfUnifies(term, variableType, varFrom, varTo)
 * TODO is this better named "substituteAll"
 */
abstract public class substituteIfUnifies extends TermTransformOperator  {

    private final OneMatchFindSubst subMatcher;
    private final PremiseEval parent; //parent matcher context

    protected substituteIfUnifies(String id, PremiseEval parent, OneMatchFindSubst sub) {
        super(id);
        this.parent = parent;
        this.subMatcher = sub;
    }

//    public substituteIfUnifies(PremiseEval parent, OneMatchFindSubst sub) {
//        this("substituteIfUnifies", parent, sub);
//    }


    /**
     * whether an actual substitution is required to happen; when true and no substitution occurrs, then fails
     */
    protected boolean mustSubstitute() {
        return false;
    }

    @NotNull
    abstract protected Op unifying();

    @NotNull
    @Override
    //public Term function(@NotNull Compound p, @NotNull PremiseEval r) {
    public Term function(@NotNull Compound p) {
        final Term[] xx = p.terms();
//        if (xx.length < 3) {
//            throw new UnsupportedOperationException();
//        }

        Term term = xx[0];

        @Nullable Op op = unifying();

        Term x = xx[1];
        Term y = xx[2];

        boolean hasAnyOp = term.hasAny(op);

        if (!hasAnyOp && mustSubstitute()) {
            return False; //FAILED?
        }

        boolean equals = x.equals(y);
        if (!equals) {
            boolean xn = (x.op()==NEG);
            boolean yn = (y.op()==NEG);
            boolean opposite = xn ^ yn;
            Term px = (opposite && xn) ? $.unneg(x).term() : x; //positive X
            Term py = (opposite && yn) ? $.unneg(y).term() : y; //positive Y
            if (Term.equalAtemporally(px, py)) {
                equals = true;
                if (opposite) {
                    if (yn && !xn) { //x isnt negated and y is, so
                        y = py;
                    } else { //if (xn && !yn) { //x is negated and y isn't, so
                        y = $.neg(y);
                    }


                    //now x and y have matching polarities
                }
            }
        }
        //boolean equals = x.equals(y);


        if (!equals && hasAnyOp) {
            OneMatchFindSubst m = this.subMatcher;
            m.clear();
            Term newTerm = m.tryMatch(op, parent, term, x, y);
            return (newTerm!=null) ? newTerm : term;
        } else {
            return equals ? term : False;
        }
    }

    public static final class substituteIfUnifiesDep extends substituteIfUnifies {


        public substituteIfUnifiesDep(PremiseEval parent, OneMatchFindSubst sub) {
            super("substituteIfUnifiesDep", parent, sub);
        }

        @NotNull
        @Override
        public Op unifying() {
            return Op.VAR_DEP;
        }
    }

    public static final class substituteOnlyIfUnifiesDep extends substituteIfUnifies {

        public substituteOnlyIfUnifiesDep(PremiseEval parent, OneMatchFindSubst sub) {
            super("substituteOnlyIfUnifiesDep", parent, sub);
        }

        @Override
        protected boolean mustSubstitute() {
            return true;
        }

        @NotNull
        @Override
        public Op unifying() {
            return Op.VAR_DEP;
        }
    }

    public static final class substituteIfUnifiesIndep extends substituteIfUnifies {

        public substituteIfUnifiesIndep(PremiseEval parent, OneMatchFindSubst sub) {
            super("substituteIfUnifiesIndep",parent, sub);
        }

        @NotNull
        @Override
        public Op unifying() {
            return Op.VAR_INDEP;
        }
    }
    public static final class substituteOnlyIfUnifiesIndep extends substituteIfUnifies {

        public substituteOnlyIfUnifiesIndep(PremiseEval parent, OneMatchFindSubst sub) {

            super("substituteOnlyIfUnifiesIndep", parent, sub);
        }

        @Override
        protected boolean mustSubstitute() {
            return true;
        }

        @NotNull
        @Override
        public Op unifying() {
            return Op.VAR_INDEP;
        }
    }
}
