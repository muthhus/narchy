package nars.term.atom;

import nars.term.Compound;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.subst.FindSubst;
import nars.term.visit.SubtermVisitor;
import nars.term.visit.SubtermVisitorX;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Predicate;

import static nars.term.atom.Atom.toUnquoted;

/** Base class for Atomic types. */
public interface Atomic extends Term {

    @NotNull
    @Override
    String toString();


    /** default atomic unification is equality */
    @Override default boolean unify(@NotNull Term y, @NotNull FindSubst subst) {
        return equals(y);
    }

    @Override
    default void recurseTerms(@NotNull SubtermVisitorX v, Compound parent) {
        v.accept(this, parent);
    }

    @Override
    default void recurseTerms(@NotNull SubtermVisitor v) {
        v.accept(this);
    }

    @Override
    default boolean and(@NotNull Predicate<Term> v) {
        return v.test(this);
    }

    @Override
    default boolean or(@NotNull Predicate<Term> v) {
        return and(v); //re-use and, even though it's so similar
    }

//    @Override
//    default String toString() {
//        return toString();
//    }

    @Override
    default boolean hasTemporal() {
        return false;
    }

    @NotNull
    default String toStringUnquoted() {
        return toUnquoted(toString());
    }

    @Override
    default void append(@NotNull Appendable w) throws IOException {
        w.append(toString());
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
    default boolean containsTerm(Termlike t) {
        return false;
    }

    @Override
    default boolean isCommutative() {
        return false;
    }


    /** default volume = 1 */
    @Override
    default int volume() { return 1; }

    @Nullable
    @Override
    default Term termOr(int i, @Nullable Term ifOutOfBounds) {
        //no superterms to select
        return ifOutOfBounds;
    }

    @Override
    int varIndep();

    @Override
    int varDep();

    @Override
    int varQuery();

    @Override
    default int structure() {
        return op().bit;
    }


}
