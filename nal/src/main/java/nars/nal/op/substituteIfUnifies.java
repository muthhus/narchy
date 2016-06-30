package nars.nal.op;

import nars.NAR;
import nars.Op;
import nars.index.TermIndex;
import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.OneMatchFindSubst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * substituteIfUnifies(term, variableType, varFrom, varTo)
 * TODO is this better named "substituteAll"
 */
abstract public class substituteIfUnifies extends substitute {

    private final ThreadLocal<OneMatchFindSubst> matcher;

    public substituteIfUnifies(String id) {
        super(id);
        this.matcher = ThreadLocal.withInitial(()->{
            return new OneMatchFindSubst();
        });
        //this.matcher = new OneMatchFindSubst(nar);
    }


    abstract public Op op();

    /**
     * whether an actual substitution is required to happen; when true and no substitution occurrs, then fails
     */
    protected boolean mustSubstitute() {
        return false;
    }

    @Nullable
    @Override
    //public Term function(@NotNull Compound p, @NotNull PremiseEval r) {
    public Term function(@NotNull Compound p, @NotNull TermIndex r) {
        final Term[] xx = p.terms();
//        if (xx.length < 3) {
//            throw new UnsupportedOperationException();
//        }

        Term term = xx[0];

        @Nullable Op op = op();

        final Term x = xx[1];
        final Term y = xx[2];

        boolean hasAnyOp = term.hasAny(op);

        if (!hasAnyOp && mustSubstitute()) {
            return null;
        }

        if (!x.equals(y) && hasAnyOp) {

            OneMatchFindSubst m = this.matcher.get();
            term = m.tryMatch(op, null, term, x, y);
            m.clear();

        }
        return term;
    }


    public static final class substituteIfUnifiesDep extends substituteIfUnifies {


        public substituteIfUnifiesDep(NAR nar) {
            super("substituteIfUnifiesDep");
        }

        @Override
        public Op op() {
            return Op.VAR_DEP;
        }
    }

    public static final class substituteOnlyIfUnifiesDep extends substituteIfUnifies {


        public substituteOnlyIfUnifiesDep(NAR nar) {

            super("substituteOnlyIfUnifiesDep");
        }

        @Override
        protected boolean mustSubstitute() {
            return true;
        }

        @Override
        public Op op() {
            return Op.VAR_DEP;
        }
    }

    public static final class substituteIfUnifiesIndep extends substituteIfUnifies {


        public substituteIfUnifiesIndep(NAR nar) {
            super("substituteIfUnifiesIndep");
        }

        @Override
        public Op op() {
            return Op.VAR_INDEP;
        }
    }
//    public static final class substituteOnlyIfUnifiesIndep extends substituteIfUnifies {
//
//
//        public substituteOnlyIfUnifiesIndep(NAR nar) {
//            super(nar);
//        }
//
//        @Override protected boolean mustSubstitute() { return true; }
//
//        @Override public Op op() {
//            return Op.VAR_INDEP;
//        }
//    }
}
