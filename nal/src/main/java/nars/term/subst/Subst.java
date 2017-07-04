package nars.term.subst;

import nars.Op;
import nars.control.premise.Derivation;
import nars.derive.meta.match.EllipsisMatch;
import nars.index.term.AppendProtoCompound;
import nars.index.term.PatternTermIndex;
import nars.index.term.TermContext;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.index.term.TermIndex.disallowTrueOrFalse;


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
    boolean put(@NotNull Unify copied);

    @Nullable default Term transform(@NotNull Term src, TermContext index) {
        Term y = xy(src);
        if (y != null) {
            return y; //an assigned substitution, whether a variable or other type of term
        }

        Op op = src.op();
        switch (op) {
            case ATOM:
            case VAR_DEP:
            case VAR_INDEP:
            case VAR_QUERY:
                return src; //unassigned literal atom or non-pattern var
            case VAR_PATTERN:
                return null; //unassigned pattern variable
        }

        //shortcut for premise evaluation matching:
        //no variables that could be substituted, so return this constant
        if (this instanceof Derivation && (src.vars() + src.varPattern() == 0))
            return src;


        boolean strict = !(index instanceof PatternTermIndex); //f instanceof Derivation;

        Compound curr = (Compound) src;
        TermContainer subs = curr.subterms();

        int len = subs.size();


        Op cop = curr.op();

        AppendProtoCompound next = new AppendProtoCompound(cop, len);

        //early prefilter for True/False subterms
        boolean filterTrueFalse = disallowTrueOrFalse(cop);


        for (int i = 0; i < len; i++) {
            Term t = subs.sub(i);
            Term u = transform(t, index);

            if (u instanceof EllipsisMatch) {

                ((EllipsisMatch) u).expand(op, next);

//                for (; volAt < subAt; volAt++) {
//                    Term st = next.sub(volAt);
//                    if (filterTrueFalse && Op.isTrueOrFalse(st)) return null;
//                }

            } else {

                if (u == null) {

                    if (strict) {
                        return null;
                    }

                    u = t; //keep value

                }

                if (Term.filterAbsolute(u, filterTrueFalse))
                    return null;

                next.add(u);


            }


        }

        int dt = curr.dt();
        return cop.the(dt, next.subterms());
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
