package nars.op.data;

import jcog.Texts;
import nars.$;
import nars.term.Term;
import nars.term.Functor;
import nars.term.var.Variable;
import org.jetbrains.annotations.Nullable;

/**
 * Uses the levenshtein distance of two term's string represents to
 * compute a similarity metric
 */
public class similaritree extends Functor.BinaryFunctor {

    public similaritree() {
        super("similaritree");
    }

    @Override
    public @Nullable Term apply(Term a, Term b) {
        if ((a instanceof Variable || b instanceof Variable))
            return null;
        return $.the( Texts.levenshteinDistance(a.toString(), b.toString()) );
    }



}
