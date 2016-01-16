package nars.term.transform;

import nars.Global;
import nars.nal.nal8.Operator;
import nars.nal.op.ImmediateTermTransform;
import nars.term.Term;
import nars.term.compound.Compound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public interface Subst  {

    boolean isEmpty();

    @Nullable
    Term getXY(Object t);

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
