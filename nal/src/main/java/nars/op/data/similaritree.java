package nars.op.data;

import jcog.Texts;
import nars.$;
import nars.term.Term;
import nars.term.transform.Functor;
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
        String as = a.toString();
        String bs = b.toString();
        int d = Texts.levenshteinDistance(as, bs);
        return $.the(d);
    }



}
