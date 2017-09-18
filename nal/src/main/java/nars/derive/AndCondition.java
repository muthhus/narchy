package nars.derive;

import com.google.common.collect.Lists;
import jcog.Util;
import jcog.list.FasterList;
import nars.$;
import nars.control.Derivation;
import nars.derive.constraint.MatchConstraint;
import nars.term.Term;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * TODO generify beyond only Derivation
 */
public final class AndCondition<D> extends AbstractPred<D> {

    //private static final Term AND_ATOM = $.quote("&&");

    @Override
    public final boolean test(@NotNull Object m) {
        for (PrediTerm x : cache) {
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

    @Override
    public PrediTerm<D> transform(Function<PrediTerm<D>, PrediTerm<D>> f) {
        PrediTerm[] yy = transformedConditions(f);
        if (yy!=cache)
            return new AndCondition(yy);
        else
            return this;
    }

    public PrediTerm[] transformedConditions(Function<PrediTerm<D>, PrediTerm<D>> f) {
        final boolean[] changed = {false};
        PrediTerm[] yy = Util.map(x -> {
            PrediTerm<D> y = x.transform(f);
            if (y != x)
                changed[0] = true;
            return y;
        }, new PrediTerm[cache.length], cache);
        if (!changed[0])
            return cache;
        else
            return yy;
    }


    public static @Nullable <D> PrediTerm<D> the(@NotNull PrediTerm<D>... cond) {
        return the(new FasterList<>(cond)); //HACK
    }

    public static @Nullable <D> PrediTerm<D> the(@NotNull List<PrediTerm<D>> cond) {

        int s = cond.size();
        if (s == 0)
            return null;

        if (s == 1) return cond.get(0);

        return new AndCondition(cond);
    }


    @NotNull
    public final PrediTerm<D>[] cache;

    /*public AndCondition(@NotNull BooleanCondition<C>[] p) {
        this(TermVector.the((Term[])p));
    }*/

    AndCondition(@NotNull PrediTerm<D>[] p) {
        super($.p((Term[]) p));
        assert (p.length >= 2) : "unnecessary use of AndCondition";
        this.cache = p;
    }

    AndCondition(@NotNull Collection<PrediTerm<D>> p) {
        this(p.toArray(new PrediTerm[p.size()]));
    }


    /**
     * combine certain types of items in an AND expression
     */
    public static List<PrediTerm<Derivation>> compile(List<PrediTerm<Derivation>> p) {
        if (p.size() == 1)
            return p;

        SortedSet<MatchConstraint> constraints = new TreeSet<>(MatchConstraint.costComparator);
        Iterator<PrediTerm<Derivation>> il = p.iterator();
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
                if ((c instanceof Fork) && iMatchTerm == -1) {
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


    public @Nullable PrediTerm<D> without(PrediTerm<D> condition) {
        int subterm = ref.subterms().indexOf(condition);
        assert(subterm!=-1);

        //TODO returns a new AndCondition with condition removed, or null if it was the only item
        PrediTerm[] x = ArrayUtils.remove(cache, subterm);
        if (x.length == cache.length)
            throw new RuntimeException("element missing for removal");

        return AndCondition.the(Lists.newArrayList(x));
    }

//    @Override
//    public PrediTerm exec(D d, CPU c) {
//
//        int i;
//        final int cacheLength = cache.length;
//        for (i = 0; i < cacheLength; i++) {
//            PrediTerm p = cache[i];
//
//            //if p.exec returns the same value (stored in 'q') and not a different or null, this is the signal that p.test FAILED
//            PrediTerm q = p.exec(d, c);
//            if (q == p)
//                break;
//        }
//
//        ((Derivation)d).use((1+i) * Param.TTL_PREDICATE);
//
//        return null;
//    }

    public PrediTerm<D> last() {
        return cache[cache.length-1];
    }

    /** chase the last of the last of the last(...etc.) condition in any number of recursive AND's */
    public static PrediTerm last(PrediTerm b) {
        while (b instanceof AndCondition) {
            b = ((AndCondition)b).last();
        }
        return b;
    }
}
