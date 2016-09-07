package nars.nal.op;

import nars.nal.meta.PremiseEval;
import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.MapSubst;
import org.jetbrains.annotations.NotNull;


public final class substitute extends TermTransformOperator {

    @NotNull
    private final PremiseEval parent;

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

}