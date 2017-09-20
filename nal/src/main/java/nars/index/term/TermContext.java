package nars.index.term;

import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * interface necessary for evaluating terms
 */
public interface TermContext extends Function<Term,Termed> {

    /** if the result is null, return the input */
    default public Termed applyIfPossible(/*@NotNull*/ Term x) {
        Termed y = apply(x);
        if (y != null)
            return y;
        else
            return x;
    }

    /** elides superfluous .term() call */
    default public Term applyTermIfPossible(/*@NotNull*/ Term x) {
        Termed y = apply(x);
        if (y != null)
            return y.term();
        else
            return x;
    }

   /** elides superfluous .term() call */
    @Nullable
    default public Term applyTermOrNull(/*@NotNull*/ Term x) {
        Termed y = apply(x);
        if (y != null)
            return y.term();
        else
            return null;
    }

//    /**
//     * internal get procedure: get if not absent
//     */
//    @Nullable
//    default Termed apply(@NotNull Term t) {
//        return apply(t, false);
//    }



}
