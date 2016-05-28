package nars.term;

import nars.Op;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Predicate;

public interface ProxyTerm<T extends Term> extends Term {

    T term();

    T target();

    @Override
    default @NotNull Op op() {
        return target().op();
    }

    @Override
    default int volume() {
        return target().volume();
    }

    @Override
    default int complexity() {
        return target().complexity();
    }

    @Override
    default int structure() {
        return target().structure();
    }

    @Override
    default int size() {
        return target().size();
    }

//    @Override
//    default boolean equals(Object o) {
//        return this == o || target().equals(o);
//    }
//
//    @Override
//    default int hashCode() {
//        return target().hashCode();
//    }

    @Override
    default void recurseTerms(@NotNull SubtermVisitor v, @Nullable Compound parent) {
        target().recurseTerms(v, parent);
    }

    @Override
    default boolean isCommutative() {
        return target().isCommutative();
    }

    @Override
    default int varIndep() {
        return target().varIndep();
    }

    @Override
    default int varDep() {
        return target().varDep();
    }

    @Override
    default int varQuery() {
        return target().varQuery();
    }

    @Override
    default int varPattern() {
        return target().varPattern();
    }

    @Override
    default int vars() {
        return target().vars();
    }


    @Override
    default boolean hasTemporal() {
        return target().hasTemporal();
    }


    @Override
    default boolean containsTerm(Termlike t) {
        return target().containsTerm(t);
    }


    @Override
    default boolean or(Predicate<Term> v) {
        return target().or(v);
    }
}
