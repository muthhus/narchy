package nars.term.transform.subst;

import nars.Global;
import nars.term.Operator;
import nars.nal.op.ImmediateTermTransform;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;


public interface Subst  {

    /** can be used to determine if this subst will have any possible effect on any transforms to any possible term,
     * used as a quick test to prevent transform initializations */
    boolean isEmpty();

    @Nullable
    Term term(Term t);

    void clear();

    /** match a range of subterms of Y.  */
    @NotNull
    static List<Term> collect(@NotNull Compound y, int from, int to) {
        int s = to-from;

        List<Term> l = Global.newArrayList(s);

        for (int i = 0; i < s; i++) {
            l.add(y.term(i+from));
        }

        return l;
    }

    void forEach(BiConsumer<? super Term, ? super Term> each);

    @Nullable
    default ImmediateTermTransform getTransform(Operator t) {
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
