package nars.derive.constraint;

import nars.$;
import nars.control.Derivation;
import nars.derive.AbstractPred;
import nars.term.Term;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;


public abstract class MatchConstraint extends AbstractPred<Derivation> {

    public final Term target;

    protected MatchConstraint(Term target, String func, Term... args) {
        super($.p(target, $.func(func, args)));
        this.target = target;
    }

    /**
     * cost of testing this, for sorting. higher value will be tested later than lower
     */
    @Override
    abstract public float cost();

    @Override
    public boolean test(Derivation p) {
        //this will not be called when it is part of a CompoundConstraint group
        return p.constrain(this);
    }


    public static final Comparator<MatchConstraint> costComparator = (a, b) -> {
        if (a.equals(b)) return 0;
        int i = Float.compare(a.cost(), b.cost());
        return i == 0 ? a.compareTo(b) : i;
    };

//    public static class CompoundConstraint extends AbstractPred<Derivation> {
//
//
//        private final MatchConstraint[] cache;
//
//        public CompoundConstraint(MatchConstraint[] c) {
//            super(/*$.func("MatchConstraint",*/ $.sete(c)/*)*/);
//            this.cache = c;
//        }
//
//        @Override
//        public boolean test(Derivation derivation) {
//            return derivation.constrain(cache);
//        }
//    }

    /**
     * @param targetVariable current value of the target variable (null if none is set)
     * @param potentialValue potential value to assign to the target variable
     * @param f              match context
     * @return true if match is INVALID, false if VALID (reversed)
     */
    abstract public boolean invalid(@NotNull Term y, @NotNull Unify f);
}
