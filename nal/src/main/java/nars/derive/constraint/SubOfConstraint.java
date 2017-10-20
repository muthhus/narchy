package nars.derive.constraint;

import nars.term.Term;
import nars.term.subst.Unify;

public class SubOfConstraint extends MatchConstraint {
    private final Term y;
    private final boolean reverse;

    public SubOfConstraint(Term x, Term y, boolean reverse) {
        super(x, reverse ? "compoundOf" :"subOf", y);
        this.y = y;
        this.reverse = reverse;
    }

    @Override
    public float cost() {
        return 0.5f;
    }

    @Override
    public boolean invalid(Term xx, Unify f) {
        Term yy = f.xy(y);
        if (yy == null)
            return false; //unknown yet

        return reverse ? !xx.contains(yy) : !yy.contains(xx);
    }
}
