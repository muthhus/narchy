package nars.nal.meta;

import nars.$;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.term.transform.CompoundTransform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

import static nars.Op.INHERIT;

/**
 * Created by me on 6/9/16.
 */
abstract class PremiseTruthTransform implements CompoundTransform<Compound, Term>, Function<Term,Term> {

    final Atom belief = $.the("Belief");
    final Atom desire = $.the("Desire");

    @Override
    public @Nullable Termed<?> apply(Compound parent, Term subterm) {

        Compound tf = (Compound) subterm;
        Term func = tf.term(0);
        Term mode = tf.term(1);

        return $.inh(apply(func), mode);

    }


    @Override
    public boolean test(@NotNull Term o) {
        if (o.op() == INHERIT) {
            Term pred = ((Compound) o).term(1);
            return pred.equals(belief) || pred.equals(desire);
        }
        return false;
    }
}
