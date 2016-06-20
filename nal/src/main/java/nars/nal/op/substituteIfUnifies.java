package nars.nal.op;

import nars.NAR;
import nars.Op;
import nars.nal.meta.PremiseEval;
import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.OneMatchFindSubst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** substituteIfUnifies(term, variableType, varFrom, varTo)
 * TODO is this better named "substituteAll"
 * */
abstract public class substituteIfUnifies extends substitute {

    /** recycled
     *  TODO initialize in construtor
     * */
    private final OneMatchFindSubst matcher;

    public substituteIfUnifies(NAR nar) {
        this.matcher = new OneMatchFindSubst(nar);
    }

    abstract public Op op();

    /** whether an actual substitution is required to happen; when true and no substitution occurrs, then fails */
    protected boolean mustSubstitute() { return false; }

    @Nullable
    @Override public Term function(@NotNull Compound p, @NotNull PremiseEval r) {
        final Term[] xx = p.terms();
//        if (xx.length < 3) {
//            throw new UnsupportedOperationException();
//        }

        Term term = xx[0];

        //final Term opT = xx[1];
        @Nullable Op op = op();

        if (term.hasAny(op)) {

            final Term x = xx[1];
            final Term y = xx[2];

            OneMatchFindSubst m = this.matcher;
            term = m.tryMatch(op, r, term, x, y);
            m.clear();
        } else {
            if (mustSubstitute())
                term = null;
        }
        return term;
    }


    public static final class substituteIfUnifiesDep extends substituteIfUnifies {


        public substituteIfUnifiesDep(NAR nar) {
            super(nar);
        }

        @Override public Op op() {
            return Op.VAR_DEP;
        }
    }

    public static final class substituteOnlyIfUnifiesDep extends substituteIfUnifies {


        public substituteOnlyIfUnifiesDep(NAR nar) {
            super(nar);
        }

        @Override protected boolean mustSubstitute() { return true; }

        @Override public Op op() {
            return Op.VAR_DEP;
        }
    }
    public static final class substituteIfUnifiesIndep extends substituteIfUnifies {


        public substituteIfUnifiesIndep(NAR nar) {
            super(nar);
        }

        @Override public Op op() {
            return Op.VAR_INDEP;
        }
    }
}
