package nars.derive.meta.constraint;

import nars.$;
import nars.derive.meta.BoolPredicate;
import nars.premise.Derivation;
import nars.term.ProxyCompound;
import nars.term.Term;
import nars.term.subst.Unify;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;


public abstract class MatchConstraint extends ProxyCompound implements BoolPredicate<Derivation> {

    public final Term target;

    public MatchConstraint(String func, Term target, Term... args) {
        super($.func(func, ArrayUtils.add(args, 0, target)));
        this.target = target;
    }

    @Override
    public boolean test(Derivation p) {
        return p.constraints.add(target, this);
    }


    /**
     * @param targetVariable X target variable that is being considering assignment
     * @param potentialValue Y potential value
     * @param f              match context
     * @return true if match is INVALID, false if VALID (reversed)
     */
    abstract public boolean invalid(@NotNull Term targetVariable, @NotNull Term potentialValue, @NotNull Unify f);
}
