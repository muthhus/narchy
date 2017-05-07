package nars.term.atom;

import jcog.Texts;
import nars.$;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.lang.Integer.MIN_VALUE;


/** Base class for Atomic types. */
public interface Atomic extends Term {

    @NotNull
    static Atomic the(@NotNull String id) {
        //special cases
        switch (id) {
            case "_":
                return Op.Imdex;
        }

        if ($.isQuoteNecessary(id))
            return $.quote(id);

        //try to parse int
        int i = Texts.i(id, MIN_VALUE);
        if (i != MIN_VALUE)
            return $.the(i); //parsed as integer, so

        return new Atom(id);

    }


    @NotNull
    @Override
    String toString();


    /** default atomic unification is equality */
    @Override default boolean unify(@NotNull Term y, @NotNull Unify subst) {
        return false;
    }

    @Override
    default boolean recurseTerms(BiPredicate<Term, Compound> whileTrue, Compound parent) {
        return whileTrue.test(this, parent);
    }

    @Override
    default void recurseTerms(@NotNull Consumer<Term> v) {
        v.accept(this);
    }

    @Override
    default boolean AND(@NotNull Predicate<Term> v) {
        return v.test(this);
    }

    @Override
    default boolean ANDrecurse(@NotNull Predicate<Term> v) { return AND(v); }

    @Override
    default boolean OR(@NotNull Predicate<Term> v) {
        return AND(v); //re-use and, even though it's so similar
    }

//    @Override
//    default String toString() {
//        return toString();
//    }




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
    default boolean contains(Termlike t) {
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
    default Term sub(int i, @Nullable Term ifOutOfBounds) {
        //no superterms to select
        return ifOutOfBounds;
    }

    @Override
    default int structure() {
        return op().bit;
    }

    @Override
    default boolean subOpIs(int i, Op o) {
        return false;
    }
}
