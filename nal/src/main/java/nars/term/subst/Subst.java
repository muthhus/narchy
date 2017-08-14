package nars.term.subst;

import jcog.list.FasterList;
import nars.Op;
import nars.derive.match.EllipsisMatch;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.Null;


public interface Subst {

    /**
     * can be used to determine if this subst will have any possible effect on any transforms to any possible term,
     * used as a quick test to prevent transform initializations
     */
    boolean isEmpty();

    /**
     * the assigned value for x
     */
    @Nullable Term xy(Term t);


//    /** suggests to this to store a cached result, which can be ignored if the impl chooses */
//    void cache(@NotNull Term x /* usually a Variable */, @NotNull Term y);


//    @NotNull
//    default Term termOrOriginal(@NotNull Term t) {
//        Term x = term(t);
//        return x == null ? t : x;
//    }

    void clear();

    /**
     * copy in
     * @NotNull commented out for performance
     * @return whether all puts were successful
     */
    @NotNull
    default Term transform(/*@NotNull*/ Term x) {

        assert (!(x instanceof Bool));

        Term y = xy(x);
        if (y != null)
            return y; //an assigned substitution, whether a variable or other type of term

        TermContainer subs = x.subterms();
        int len = subs.size();

        if (len == 0)
            return x;

        FasterList<Term> next = new FasterList<>(len);

        Op op = x.op();

        boolean filterTrueFalse = !op.allowsBool; //early prefilter for True/False subterms

        for (int i = 0; i < len; i++) {
            Term t = subs.sub(i);
            Term u = transform(t);
            if (!addTransformed(u, next, filterTrueFalse))
                return Null;
        }

//        int ns = next.size();
//        if (ns > op.maxSize)
//            return null;
//        if (op.statement && ns < op.minSize) {
////
////            if (ns != 1 || next.sub(0).op()!=VAR_PATTERN) //exclude special single-element pattern variables
//                return null;
//        }

        if (!subs.equalTerms(next))
            return op.the(x.dt(), next.array(Term[]::new));
        else
            return x;
    }

    static boolean addTransformed(Term u, FasterList<Term> next, boolean filterTrueFalse) {

        if (u instanceof EllipsisMatch) {

            return (!((EllipsisMatch) u).forEachWhile(x -> addTransformed(x, next, filterTrueFalse)));

        } else {

            return !Term.invalidBoolSubterms(u, filterTrueFalse) && next.add(u);
        }
    }


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
