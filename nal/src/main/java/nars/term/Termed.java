package nars.term;

import nars.Op;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * has, or is associated with a specific term
 */
public interface Termed extends Termlike {

    /*@NotNull*/ Term term();

    @Override default Term sub(int i) {
        return subterms().sub(i);
    }

    default TermContainer subterms() {
        return term().subterms();
    }

    @Override
    default int subs() {
        return term().subs();
    }

    /*@NotNull*/
    default Op op() {
        return term().op();
    }

//    default int varPattern() {
//        return term().varPattern();
//    }
//
//    default int varQuery() {
//        return term().varQuery();
//    }
//
//    default int varIndep() {
//        return term().varIndep();
//    }
//
//    default int varDep() {
//        return term().varDep();
//    }
//    default boolean levelValid(int nal) {
//        return term().levelValid(nal);
//    }

    default boolean isNormalized() {
        return term().isNormalized();
    }

    @Nullable
    static Term termOrNull(@Nullable Termed x) {
        return x == null ? null : x.term();
    }

//    default int volume() {
//        return term().volume();
//    }
//
//    default int complexity() {
//        return term().complexity();
//    }
//
//    default int structure() {
//        return term().structure();
//    }

    @NotNull
    default Term unneg() {
        return term().unneg();
    }


    @Override
    default boolean containsRecursively(Term t, Predicate<Term> inSubtermsOf) {
        return term().containsRecursively(t, inSubtermsOf);
    }

    @Override
    default Term sub(int i, Term ifOutOfBounds) {
        return term().sub(i, ifOutOfBounds);
    }

    @Override
    default boolean ORrecurse(Predicate<Term> v) {
        return term().ORrecurse(v);
    }

    @Override
    default boolean ANDrecurse(Predicate<Term> v) {
        return term().ANDrecurse(v);
    }


    @Override
    default void recurseTerms(Consumer<Term> v) {
        term().recurseTerms(v);
    }


}
