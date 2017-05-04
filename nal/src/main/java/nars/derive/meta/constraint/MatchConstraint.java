package nars.derive.meta.constraint;

import nars.$;
import nars.derive.meta.AbstractPred;
import nars.derive.meta.BoolPred;
import nars.premise.Derivation;
import nars.term.ProxyCompound;
import nars.term.Term;
import nars.term.subst.Unify;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;


public abstract class MatchConstraint extends ProxyCompound implements BoolPred<Derivation> {

    public final Term target;

    public MatchConstraint(String func, Term target, Term... args) {
        super($.func(func, ArrayUtils.add(args, 0, target)));
        this.target = target;
    }

    /** cost of testing this, for sorting. higher value will be tested later than lower */
    public int cost() {
        return 0;
    }

    @Override
    public boolean test(Derivation p) {
        //this will not be called when it is part of a CompoundConstraint group
        return p.addConstraint(this);
    }

    public static class CompoundConstraint extends AbstractPred<Derivation> {


        private final MatchConstraint[] cache;

        public CompoundConstraint(MatchConstraint[] c) {
            super($.func("MatchConstraint", c ));
            this.cache = c;
        }

        @Override
        public boolean test(Derivation derivation) {
            return derivation.addConstraint(cache);
        }
    }

    /**
     * @param targetVariable current value of the target variable (null if none is set)
     * @param potentialValue potential value to assign to the target variable
     * @param f              match context
     * @return true if match is INVALID, false if VALID (reversed)
     */
    abstract public boolean invalid(@NotNull Term y, @NotNull Unify f);
}
