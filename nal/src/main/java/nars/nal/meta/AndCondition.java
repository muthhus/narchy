package nars.nal.meta;

import com.google.common.collect.Lists;
import nars.Op;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Created by me on 12/31/15.
 */
public final class AndCondition extends GenericCompound<BoolCondition> implements BoolCondition {

    @NotNull
    protected final BoolCondition[] termCache;

    /*public AndCondition(@NotNull BooleanCondition<C>[] p) {
        this(TermVector.the((Term[])p));
    }*/
    public AndCondition(@NotNull Collection<BoolCondition> p) {
        this(new TermVector(p, BoolCondition.class));
    }

    public AndCondition(@NotNull TermContainer conds) {
        super(Op.CONJUNCTION, conds);
        this.termCache = (BoolCondition[]) conds.terms();
        if (termCache.length < 2)
            throw new RuntimeException("unnecessary use of AndCondition");
    }



    @Override
    public final boolean booleanValueOf(PremiseEval m) {
        for (BoolCondition x : termCache) {
            if (!x.booleanValueOf(m))
                return false;
        }
        return true;
    }

    public static @Nullable BoolCondition the(@NotNull List<BoolCondition> cond) {

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

    public @Nullable BoolCondition without(BoolCondition condition) {
        //TODO returns a new AndCondition with condition removed, or null if it was the only item
        BoolCondition[] x = ArrayUtils.removeElement(termCache, condition);
        if (x.length == termCache.length)
            throw new RuntimeException("element missing for removal");

        return AndCondition.the(Lists.newArrayList(x));
    }
}
