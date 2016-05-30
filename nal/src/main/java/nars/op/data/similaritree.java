package nars.op.data;

import nars.index.TermIndex;
import nars.nal.nal8.operator.TermFunction;
import nars.term.Compound;
import nars.term.Operator;
import nars.term.Term;
import nars.util.Texts;
import org.jetbrains.annotations.NotNull;

/**
 * Uses the levenshtein distance of two term's string represents to
 * compute a similarity metric
 */
public class similaritree extends TermFunction<Float> {

    //TODO integrate Ters Terms.termDistance()

    @Override
    public Float function(@NotNull Compound o, TermIndex i) {

        Term[] x = Operator.argArray(o);
        if (x.length!=2) return Float.NaN;

        String a = x[0].toString();
        String b = x[1].toString();

        float d = Texts.levenshteinDistance(a, b);
        return 1.0f - (d / (Math.max(a.length(), b.length())));
    }

}
