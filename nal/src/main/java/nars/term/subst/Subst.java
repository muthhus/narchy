package nars.term.subst;

import jcog.list.FasterList;
import nars.Op;
import nars.control.premise.Derivation;
import nars.derive.match.EllipsisMatch;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public interface Subst  {

    /** can be used to determine if this subst will have any possible effect on any transforms to any possible term,
     * used as a quick test to prevent transform initializations */
    boolean isEmpty();

    /** the assigned value for x */
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
    //boolean put(@NotNull Unify copied);


    @Nullable default Term transform(@NotNull Term x) {
        Term y = xy(x);
        if (y != null) {
            return y; //an assigned substitution, whether a variable or other type of term
        }

        if (!(x instanceof Compound))
            return x;
//        Op op = x.op();
//        switch (op) {
//            case ATOM:
//            case INT:
//            case VAR_DEP:
//            case VAR_INDEP:
//            case VAR_QUERY:
//            case VAR_PATTERN:
//                return x; //unassigned literal atom or non-pattern var
//        }

//        //shortcut for premise evaluation matching:
//        //no variables that could be substituted, so return this constant
//        if (this instanceof Derivation && (x.vars() + x.varPattern() == 0))
//            return x;



        Compound curr = (Compound) x;
        TermContainer subs = curr.subterms();

        int len = subs.size();


        Op op = x.op();

        FasterList<Term> next = new FasterList(len);

        //early prefilter for True/False subterms
        boolean filterTrueFalse = !op.allowsBool;

        for (int i = 0; i < len; i++) {
            Term t = subs.sub(i);
            Term u = transform(t);

            if (u instanceof EllipsisMatch) {

                ((EllipsisMatch)u).forEach(next::add);

//                for (; volAt < subAt; volAt++) {
//                    Term st = next.sub(volAt);
//                    if (filterTrueFalse && Op.isTrueOrFalse(st)) return null;
//                }

            } else {

                if (u == null || Term.invalidBoolSubterms(u, filterTrueFalse)) {
                    return null;
                }

                if (this instanceof Derivation && u.varPattern() > 0) {
                    //assert(false): "varPattern should have been filtered? " + u;
                    return null;
                }

                if (!next.add(u))
                    return null;

            }

        }


//        int ns = next.size();
//        if (ns > op.maxSize)
//            return null;
//        if (op.statement && ns < op.minSize) {
////
////            if (ns != 1 || next.sub(0).op()!=VAR_PATTERN) //exclude special single-element pattern variables
//                return null;
//        }

        return op.the(curr.dt(), next.array(Term[]::new));
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
