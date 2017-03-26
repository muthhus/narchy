package nars.derive.meta;

import com.google.common.collect.Lists;
import nars.Op;
import nars.premise.Derivation;
import nars.term.compound.GenericCompound;
import nars.term.container.TermVector;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Created by me on 12/31/15.
 */
public final class AndCondition extends GenericCompound implements BoolPredicate<Derivation> {

    @NotNull
    public final BoolPredicate[] termCache;

    /*public AndCondition(@NotNull BooleanCondition<C>[] p) {
        this(TermVector.the((Term[])p));
    }*/
    public AndCondition(@NotNull Collection<BoolPredicate> p) {
        super(Op.PROD, TermVector.the(p));
        this.termCache = p.toArray(new BoolPredicate[p.size()]);
        if (termCache.length < 2)
            throw new RuntimeException("unnecessary use of AndCondition");
    }


//    /** just attempts to evaluate the condition, causing any desired side effects as a result */
//    @Override public final void accept(@NotNull PremiseEval m, int now) {
//        booleanValueOf(m, now);
////        m.revert(now);
//    }

    @Override
    public final boolean test(@NotNull Derivation m) {
        boolean result = true;
        int start = m.now();
        for (BoolPredicate x : termCache) {
            boolean b = x.test(m);
//            if (m.now() > 0)
//                System.out.println(m.now() + " " + x.getClass() + " " + m + " " + x + " = " + b);
            if (!b) {
                result = false;
                break;
            }
        }

        m.revert(start);
        return result;
    }

    public static @Nullable BoolPredicate the(@NotNull List<BoolPredicate> cond) {

        //remove suffix 'TRUE'
        int s = cond.size();
        if (s == 0) return null;


//        if (cond.get(s - 1) == BoolCondition.TRUE) {
//            cond = cond.subList(0, s - 1);
//            s--;
//            if (s == 0) return null;
//        }


        if (s == 1) return cond.get(0);
        return new AndCondition(cond);
    }

    public @Nullable BoolPredicate without(BoolPredicate condition) {
        //TODO returns a new AndCondition with condition removed, or null if it was the only item
        BoolPredicate[] x = ArrayUtils.removeElement(termCache, condition);
        if (x.length == termCache.length)
            throw new RuntimeException("element missing for removal");

        return AndCondition.the(Lists.newArrayList(x));
    }
}
