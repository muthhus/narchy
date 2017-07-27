package nars.derive.constraint;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;

/** containment test of x to y's subterms and y to x's subterms */
public final class NoCommonSubtermConstraint extends CommonalityConstraint {

    public final boolean recurse;

    /**
     * @param recurse
     *   true: recursive
     *   false: only cross-compares the first layer of subterms.
     */
    public NoCommonSubtermConstraint(Term target, @NotNull Term x, boolean recurse) {
        super(recurse ? "neqRCom" : "neqCom",
                target, x );
        this.recurse = recurse;
    }

    @Override
    public int cost() {
        return recurse ? 10 : 5;
    }

    @NotNull
    @Override protected boolean invalid(Term x, Term y) {
        return isSubtermOfTheOther(x, y, recurse, true);
    }

    static boolean isSubtermOfTheOther(@NotNull Term a, @NotNull Term b, boolean recurse, boolean excludeVariables) {

        if ((excludeVariables) && (a instanceof Variable || b instanceof Variable))
            return false;

        return recurse ?

                //a.containsRecursively(b) || b.containsRecursively(a) :
                a.containsRecursively(b, Op.nonProduct) || b.containsRecursively(a, Op.nonProduct) :

                a.contains(b) || b.contains(a);
    }
    //commonSubtermsRecurse((Compound) B, C, true, new HashSet())
                    //commonSubterms((Compound) B, C, true, scratch.get())



}
