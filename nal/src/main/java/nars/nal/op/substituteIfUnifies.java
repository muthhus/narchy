package nars.nal.op;

import nars.Op;
import nars.nal.meta.PremiseEval;
import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.OneMatchFindSubst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @Nullable
    @Override
    //public Term function(@NotNull Compound p, @NotNull PremiseEval r) {
    public Term function(@NotNull Compound p) {
        final Term[] xx = p.terms();
//        if (xx.length < 3) {
//            throw new UnsupportedOperationException();
//        }

        Term term = xx[0];

        @Nullable Op op = unifying();

        final Term x = xx[1];
        final Term y = xx[2];

        boolean hasAnyOp = term.hasAny(op);

        if (!hasAnyOp && mustSubstitute()) {
            return null;
        }

        //boolean equals = Term.equalAtemporally(x, y);
        boolean equals = x.equals(y);
        if (!equals && hasAnyOp) {
            OneMatchFindSubst m = this.subMatcher;
            term = m.tryMatch(op, parent, term, x, y);
            m.clear();
            return term;
        } else {
            return equals ? term : null;
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
