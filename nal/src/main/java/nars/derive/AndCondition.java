package nars.derive;

import jcog.Util;
import nars.$;
import nars.control.Derivation;
import nars.term.Term;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * TODO generify beyond only Derivation
 */
public final class AndCondition<D> extends AbstractPred<D> {

    @Override
    public final boolean test(Object m) {
        for (PrediTerm x : cache) {
            boolean b = x.test(m);
            if (!b)
                return false;
        }
        return true;
    }
    AndCondition(@NotNull PrediTerm<D>[] p) {
        super($.p((Term[]) p));
        assert (p.length >= 2) : "unnecessary use of AndCondition";
        this.cache = p;
    }

    AndCondition(@NotNull Collection<PrediTerm<D>> p) {
        this(p.toArray(new PrediTerm[p.size()]));
    }

    @NotNull
    public final PrediTerm<D>[] cache;

    public static @Nullable <D> PrediTerm<D> the(@NotNull PrediTerm<D>... cond) {
        int s = cond.length;
        switch (s) {
            case 0: return null;
            case 1: return cond[0];
            default:
                final boolean[] needsFlat = {false};
                do {
                    for (Term c : cond) {
                        if (c instanceof AndCondition) {
                            needsFlat[0] = true;
                        }
                    }
                    if (needsFlat[0]) {
                        needsFlat[0] = false;
                        cond = Stream.of(cond).flatMap(x -> {
                            if (x instanceof AndCondition)
                                return Stream.of(((AndCondition) x).cache);
                            else
                                return Stream.of(x);
                        }).peek(x -> {
                            if ((x instanceof AndCondition))
                                needsFlat[0] = true;//does this need to be recursed
                        }).toArray(PrediTerm[]::new);
                    }
                } while (needsFlat[0]);

                return new AndCondition(cond);
        }
    }

    public PrediTerm<D> first() {
        return cache[0];
    }
    public PrediTerm<D> last() {
        return cache[cache.length-1];
    }

    /** chase the last of the last of the last(...etc.) condition in any number of recursive AND's */
    public static PrediTerm last(PrediTerm b) {
        while (b instanceof AndCondition)
            b = ((AndCondition)b).last();
        return b;
    }

     /** chase the last of the first of the first (...etc.) condition in any number of recursive AND's */
    public static PrediTerm first(PrediTerm b) {
        while (b instanceof AndCondition)
            b = ((AndCondition)b).first();
        return b;
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





    /*public AndCondition(@NotNull BooleanCondition<C>[] p) {
        this(TermVector.the((Term[])p));
    }*/



    /**
     * combine certain types of items in an AND expression
     */
    public static Stream<PrediTerm<Derivation>> compile(Stream<PrediTerm<Derivation>> p) {
//        if (p.size() == 1)
//            return p;

//        SortedSet<MatchConstraint> constraints = new TreeSet<>(MatchConstraint.costComparator);
//        Iterator<PrediTerm<Derivation>> il = p.iterator();
//        while (il.hasNext()) {
//            PrediTerm c = il.next();
//            if (c instanceof MatchConstraint) {
//                constraints.add((MatchConstraint) c);
//                il.remove();
//            }
//        }


//        if (!constraints.isEmpty()) {
//
//
//            int iMatchTerm = -1; //first index of a MatchTerm op, if any
//            for (int j = 0, cccSize = p.size(); j < cccSize; j++) {
//                PrediTerm c = p.get(j);
//                if ((c instanceof Fork) && iMatchTerm == -1) {
//                    iMatchTerm = j;
//                }
//            }
//            if (iMatchTerm == -1)
//                iMatchTerm = p.size();
//
//            //1. sort the constraints and add them at the end
//            int c = constraints.size();
//            if (c > 1) {
//                p.add(iMatchTerm, new MatchConstraint.CompoundConstraint(constraints.toArray(new MatchConstraint[c])));
//            } else
//                p.add(iMatchTerm, constraints.iterator().next()); //just add the singleton at the end
//        }

        return p;
    }


//    /** just attempts to evaluate the condition, causing any desired side effects as a result */
//    @Override public final void accept(@NotNull PremiseEval m, int now) {
//        booleanValueOf(m, now);
////        m.revert(now);
//    }


    public @Nullable PrediTerm<D> without(PrediTerm<D> condition) {
        PrediTerm[] x = ArrayUtils.remove(cache, ArrayUtils.indexOf(cache, condition)); assert(x.length != cache.length);
        return AndCondition.the(x);
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


}
