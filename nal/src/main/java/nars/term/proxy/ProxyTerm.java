package nars.term.proxy;

import nars.Op;
import nars.term.Compound;
import nars.term.SubtermVisitor;
import nars.term.Term;
import nars.term.Termlike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public interface ProxyTerm<T extends Term> extends Term {

    @NotNull
    @Override
    T term();

    T target();

    @Override
    default @NotNull Op op() {
        return target().op();
    }

//    @Override
//    default int volume() {
//        return target().volume();
//    }
//
//    @Override
//    default int complexity() {
//        return target().complexity();
//    }
//
//    @Override
//    default int structure() {
//        return target().structure();
//    }
//
//    @Override
//    default int size() {
//        return target().size();
//    }


//    @Override
//    default void recurseTerms(@NotNull SubtermVisitor v, @Nullable Compound parent) {
//        target().recurseTerms(v, parent);
//    }
//
//    @Override
//    default boolean isCommutative() {
//        return target().isCommutative();
//    }

//    @Override
//    default int varIndep() {
//        return target().varIndep();
//    }
//
//    @Override
//    default int varDep() {
//        return target().varDep();
//    }
//
//    @Override
//    default int varQuery() {
//        return target().varQuery();
//    }
//
//    @Override
//    default int varPattern() {
//        return target().varPattern();
//    }
//
//    @Override
//    default int vars() {
//        return target().vars();
//    }


//    @Override
//    default boolean hasTemporal() {
//        return target().hasTemporal();
//    }
//
//
//    @Override
//    default boolean containsTerm(Termlike t) {
//        return target().containsTerm(t);
//    }



}
