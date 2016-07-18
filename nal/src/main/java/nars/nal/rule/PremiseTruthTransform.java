package nars.nal.rule;

import nars.$;
import nars.nal.meta.TruthOperator;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.term.transform.CompoundTransform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

import static nars.Op.INH;

/**
 * Created by me on 6/9/16.
 */
abstract class PremiseTruthTransform implements CompoundTransform<Compound, Term>, Function<Term,Term> {

    public final boolean includeBelief, includeDesire;



    final Atom belief = $.the("Belief");
    final Atom desire = $.the("Desire");


    protected PremiseTruthTransform(boolean includeBelief, boolean includeDesire) {
        this.includeBelief = includeBelief;
        this.includeDesire = includeDesire;
    }

    @Override
    public @Nullable Termed<?> apply(@NotNull Compound parent, @NotNull Term subterm) {

        Compound tf = (Compound) subterm;
        Term func = tf.term(0);
        Term mode = tf.term(1);

        if (func.equals(TruthOperator.NONE))
            return subterm; //no change

        if ((!includeDesire && mode.equals(desire)) || (!includeBelief && mode.equals(belief)))
            return $.inh(TruthOperator.NONE, mode);

        return $.inh(apply(func), mode);

    }


    @Override
    public boolean test(@NotNull Term o) {
        if (o.op() == INH) {
            Term pred = ((Compound) o).term(1);
            return (pred.equals(belief)) || (pred.equals(desire));
        }
        return false;
    }
}
