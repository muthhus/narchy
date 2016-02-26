package nars.term.atom;

import nars.term.Compound;
import nars.term.SubtermVisitor;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Predicate;

/** Base class for Atomic types. */
public interface Atomic extends Term {


    @Override
    default boolean isCompound() { return false; }

    @Nullable
    @Override
    String toString();

    
    @Override
    default void recurseTerms(@NotNull SubtermVisitor v, Compound parent) {
        v.accept(this, parent);
    }

    @Override
    default boolean and(@NotNull Predicate<? super Term> v) {
        return v.test(this);
    }

    @Override
    default boolean or(@NotNull Predicate<? super Term> v) {
        return and(v); //re-use and, even though it's so similar
    }

    @Override
    default String toString(boolean pretty) {
        return toString();
    }

    @Override
    default void append(@NotNull Appendable w, boolean pretty) throws IOException {
        w.append(toString());
    }

    /** preferably use toCharSequence if needing a CharSequence; it avoids a duplication */
    @NotNull
    @Override
    default StringBuilder toStringBuilder(boolean pretty) {
        return new StringBuilder(toString());
    }

    /** number of subterms; for atoms this must be zero */
    @Override
    default int size() {
        return 0;
    }

    /** atoms contain no subterms so impossible for anything to fit "inside" it */
    @Override
    default boolean impossibleSubTermVolume(int otherTermVolume) {
        return true;
    }

    @Override
    default boolean containsTerm(Term t) {
        return false;
    }

    @Override
    default boolean isCommutative() {
        return false;
    }


    /** default volume = 1 */
    @Override
    default int volume() { return 1; }


    @Override
    int varIndep();

    @Override
    int varDep();

    @Override
    int varQuery();

    @Override
    default int structure() {
        return op().bit();
    }


}
