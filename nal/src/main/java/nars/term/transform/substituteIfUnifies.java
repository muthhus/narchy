package nars.term.transform;

import nars.$;
import nars.Op;
import nars.nal.meta.PremiseEval;
import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.OneMatchFindSubst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.NEG;
import static nars.time.Tense.DTERNAL;

/**
 * substituteIfUnifies....(term, varFrom, varTo)
 */
abstract public class substituteIfUnifies extends TermTransformOperator  {

    //private final OneMatchFindSubst subMatcher;
    protected final PremiseEval parent; //parent matcher context

    protected substituteIfUnifies(String id, PremiseEval parent) {
        super(id);
        this.parent = parent;
        //this.subMatcher = sub;
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
        final Term[] a = p.terms();
//        if (xx.length < 3) {
//            throw new UnsupportedOperationException();
//        }

        Term term = a[0];
        Term x = a[1];
        Term y = a[2];

        return unify(term, x, y);
    }

    public @NotNull Term unify(Term term, Term x, Term y) {
        @Nullable Op op = unifying();

        boolean hasAnyOp = term.hasAny(op);

        if (!hasAnyOp && mustSubstitute()) {
            return False; //FAILED?
        }

        boolean equals = x.equals(y);
        if (!equals) {
            boolean xn = (x.op()==NEG);
            boolean yn = (y.op()==NEG);
            Term px = (xn) ? $.unneg(x).term() : x; //positive X
            Term py = (yn) ? $.unneg(y).term() : y; //positive Y
            if (Term.equalAtemporally(px, py)) {
                equals = true;
                if (xn ^ yn) {
                    if (yn && !xn) { //x isnt negated and y is, so
                        y = py;
                    } else { //if (xn && !yn) { //x is negated and y isn't, so
                        y = $.neg(y);
                    }

                    term = $.neg(term);

                    //now x and y have matching polarities
                } else if (xn && yn) {
                    //both negated
                } else {
                    //shouldnt hapen?
                }
            }
        }

        if (!equals && hasAnyOp) {
            OneMatchFindSubst m = new OneMatchFindSubst(parent);

            Term newTerm = m.tryMatch(op, parent, term, x, y);

            return (newTerm!=null) ? newTerm : term;
        } else {
            return equals ? term : False;
        }
    }

    public static final class substituteIfUnifiesDep extends substituteIfUnifies {


        public substituteIfUnifiesDep(PremiseEval parent) {
            super("substituteIfUnifiesDep", parent);
        }

        @NotNull
        @Override
        public Op unifying() {
            return Op.VAR_DEP;
        }
    }

    public static final class substituteOnlyIfUnifiesDep extends substituteIfUnifies {

        public substituteOnlyIfUnifiesDep(PremiseEval parent) {
            super("substituteOnlyIfUnifiesDep", parent);
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

        public substituteIfUnifiesIndep(PremiseEval parent) {
            super("substituteIfUnifiesIndep",parent);
        }

        @NotNull
        @Override
        public Op unifying() {
            return Op.VAR_INDEP;
        }
    }


    /** specifies a forward ordering constraint, for example:
     *      B, (C && A), time(decomposeBelief) |- substituteIfUnifiesIndepForward(C,A,B), (Desire:Strong)
     *
     *  if B unifies with A then A must be eternal, simultaneous, or future with respect to C
     *
     *  for now, this assumes the decomposed term is in the belief position
     */
    public static final class substituteIfUnifiesIndepForward extends substituteIfUnifies {

        public substituteIfUnifiesIndepForward(PremiseEval parent) {
            super("substituteIfUnifiesIndepForward",parent);
        }

        @Override
        public @NotNull Term unify(Term C, Term A, Term B) {
            Compound decomposed = (Compound) parent.beliefTerm;
            int dt = decomposed.dt();
            if (dt == DTERNAL || dt == 0) {
                //valid
            } else {
                //check C's position

                if (decomposed.term(0).equals(C)) {
                    if (dt < 0)
                        return False;
                } else if (decomposed.term(1).equals(C)) {
                    if (dt > 0)
                        return False;
                } else {
                    throw new RuntimeException("missing C in decomposed");
                }
            }

            return super.unify(C, A, B);
        }

        @NotNull
        @Override
        public Op unifying() {
            return Op.VAR_INDEP;
        }
    }

    public static final class substituteOnlyIfUnifiesIndep extends substituteIfUnifies {

        public substituteOnlyIfUnifiesIndep(PremiseEval parent) {

            super("substituteOnlyIfUnifiesIndep", parent);
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
