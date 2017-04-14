package nars.term.subst;

import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public interface Subst  {

    /** can be used to determine if this subst will have any possible effect on any transforms to any possible term,
     * used as a quick test to prevent transform initializations */
    boolean isEmpty();

    @Nullable Term xy(Term t);

//    /** suggests to this to store a cached result, which can be ignored if the impl chooses */
//    void cache(@NotNull Term x /* usually a Variable */, @NotNull Term y);


//    @NotNull
//    default Term termOrOriginal(@NotNull Term t) {
//        Term x = term(t);
//        return x == null ? t : x;
//    }

    void clear();

    /** copy in
     * @return whether all puts were successful
     * */
    boolean tryPut(@NotNull Unify copied);

//    void forEach(@NotNull BiConsumer<? super Term, ? super Term> each);
//
//    /** returns true only if each evaluates true; if empty, returns true also */
//    default boolean forEach(@NotNull BiPredicate<? super Term, ? super Term> each) {
//        final boolean[] b = {true};
//        forEach((k,v)-> {
//            if (!each.test(k, v)) {
//                b[0] = false;
//            }
//        });
//        return b[0];
//    }





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
