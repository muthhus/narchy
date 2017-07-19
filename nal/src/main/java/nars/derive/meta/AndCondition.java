package nars.derive.meta;

import com.google.common.collect.Lists;
import nars.$;
import nars.control.premise.Derivation;
import nars.derive.meta.constraint.MatchConstraint;
import nars.derive.meta.op.MatchOneSubtermPrototype;
import nars.term.Term;
import nars.term.compound.ProxyCompound;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created by me on 12/31/15.
 */
public final class AndCondition extends ProxyCompound implements PrediTerm<Derivation> {

    private static final Term AND_ATOM = $.quote("&&");

    @Override
    public final boolean test(@NotNull Derivation m) {
        for (PrediTerm<Derivation> x : cache) {
            boolean b = x.test(m);

//            if (m.now() > 0)
//                System.out.println(m.now() + " " + x.getClass() + " " + m + " " + x + " = " + b);
//            if (!b)
//                System.out.println("fail: " + m.task + " " + m.beliefTerm + "\t" + x);

            if (!b) {
                return false;
            }
        }
        return true;
    }

    public static @Nullable PrediTerm the(@NotNull List<PrediTerm> cond) {

        int s = cond.size();
        if (s == 0)
            return null;

        if (s == 1) return cond.get(0);

        return new AndCondition(cond);
    }


    @NotNull
    public final PrediTerm<Derivation>[] cache;

    /*public AndCondition(@NotNull BooleanCondition<C>[] p) {
        this(TermVector.the((Term[])p));
    }*/
    AndCondition(@NotNull Collection<PrediTerm> p) {
        super($.p(AND_ATOM, $.p(p.toArray(new Term[p.size()]))));

        this.cache = p.toArray(new PrediTerm[p.size()]);
        if (cache.length < 2)
            throw new RuntimeException("unnecessary use of AndCondition");
    }


    /**
     * combine certain types of items in an AND expression
     */
    public static List<PrediTerm> compile(List<PrediTerm> p) {
        if (p.size() == 1)
            return p;

        SortedSet<MatchConstraint> constraints = new TreeSet<MatchConstraint>(MatchConstraint.costComparator);
        Iterator<PrediTerm> il = p.iterator();
        while (il.hasNext()) {
            PrediTerm c = il.next();
            if (c instanceof MatchConstraint) {
                constraints.add((MatchConstraint) c);
                il.remove();
            }
        }


        if (!constraints.isEmpty()) {


            int iMatchTerm = -1; //first index of a MatchTerm op, if any
            for (int j = 0, cccSize = p.size(); j < cccSize; j++) {
                PrediTerm c = p.get(j);
                if ((c instanceof MatchOneSubtermPrototype || c instanceof Fork) && iMatchTerm == -1) {
                    iMatchTerm = j;
                }
            }
            if (iMatchTerm == -1)
                iMatchTerm = p.size();

            //1. sort the constraints and add them at the end
            int c = constraints.size();
            if (c > 1) {
                p.add(iMatchTerm, new MatchConstraint.CompoundConstraint(constraints.toArray(new MatchConstraint[c])));
            } else
                p.add(iMatchTerm, constraints.iterator().next()); //just add the singleton at the end
        }

        return p;
    }


//    /** just attempts to evaluate the condition, causing any desired side effects as a result */
//    @Override public final void accept(@NotNull PremiseEval m, int now) {
//        booleanValueOf(m, now);
////        m.revert(now);
//    }


    public @Nullable PrediTerm without(PrediTerm condition) {
        //TODO returns a new AndCondition with condition removed, or null if it was the only item
        PrediTerm[] x = ArrayUtils.removeElement(cache, condition);
        if (x.length == cache.length)
            throw new RuntimeException("element missing for removal");

        return AndCondition.the(Lists.newArrayList(x));
    }
}
