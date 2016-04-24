package nars.term.transform.subst;

import nars.nal.op.ImmediateTermTransform;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.util.version.Versioned;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;


public interface Subst  {

    /** can be used to determine if this subst will have any possible effect on any transforms to any possible term,
     * used as a quick test to prevent transform initializations */
    boolean isEmpty();

    @Nullable
    Term term(Term t);

//    @NotNull
//    default Term termOrOriginal(@NotNull Term t) {
//        Term x = term(t);
//        return x == null ? t : x;
//    }

    void clear();

    void forEach(@NotNull BiConsumer<? super Term, ? super Term> each);

    /** returns true only if each evaluates true; if empty, returns true also */
    default boolean forEach(@NotNull BiPredicate<? super Term, ? super Term> each) {
        final boolean[] b = {true};
        forEach((k,v)-> {
            if (!each.test(k, v)) {
                b[0] = false;
            }
        });
        return b[0];
    }

    @Nullable
    default ImmediateTermTransform getTransform(@NotNull Atomic t) {
        return null;
    }


//
//    boolean match(final Term X, final Term Y);
//
//    /** matches when x is of target variable type */
//    boolean matchXvar(Variable x, Term y);
//
//    /** standard matching */
//    boolean next(Term x, Term y, int power);
//
//    /** compiled matching */
//    boolean next(TermPattern x, Term y, int power);
//
//    void putXY(Term x, Term y);
//    void putYX(Term x, Term y);
//





}
