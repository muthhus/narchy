package nars.derive.rule;

import nars.$;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.transform.CompoundTransform;
import nars.truth.func.TruthOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

import static nars.Op.INH;

/**
 * Created by me on 6/9/16.
 */
abstract class PremiseTruthTransform implements CompoundTransform, Function<Term,Term> {

    public final boolean includeBelief, includeDesire;



    final static Atomic belief = Atomic.the("Belief");
    final static Atomic desire = Atomic.the("Desire");


    protected PremiseTruthTransform(boolean includeBelief, boolean includeDesire) {
        this.includeBelief = includeBelief;
        this.includeDesire = includeDesire;
    }

    @Nullable
    @Override
    public Term apply(@NotNull Compound parent, @NotNull Term o) {
        if (o.op() == INH) {
            Term pred = ((Compound) o).term(1);
            if ((pred.equals(belief)) || (pred.equals(desire))) {
                Compound tf = (Compound) o;
                Term func = tf.term(0);
                Term mode = tf.term(1);

                if (func.equals(TruthOperator.NONE))
                    return o; //no change

                if ((!includeDesire && mode.equals(desire)) || (!includeBelief && mode.equals(belief)))
                    return $.inh(TruthOperator.NONE, mode);

                return $.inh(apply(func), mode);

            }
        }
        return o;
    }

}
