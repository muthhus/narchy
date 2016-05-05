package nars.nal.meta;

import com.google.common.collect.Lists;
import nars.Op;
import nars.term.Term;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by me on 12/31/15.
 */
public final class AndCondition<C> extends GenericCompound<BooleanCondition<C>> implements BooleanCondition<C> {

    @NotNull
    protected final BooleanCondition[] termCache;

    public AndCondition(@NotNull BooleanCondition<C>[] p) {
        this(TermVector.the((Term[])p));
    }
    public AndCondition(@NotNull Collection<BooleanCondition<C>> p) {
        this(new TermVector(p, BooleanCondition.class));
    }

    public AndCondition(@NotNull TermContainer conds) {
        super(Op.CONJUNCTION, conds);
        this.termCache = (BooleanCondition[]) conds.terms();
        if (termCache.length < 2)
            throw new RuntimeException("unnecessary use of AndCondition");
    }



    @Override
    public final boolean booleanValueOf(C m) {
        for (BooleanCondition x : termCache) {
            if (!x.booleanValueOf(m))
                return false;
        }
        return true;
    }

    public void appendJavaCondition(StringBuilder s) {
//        Joiner.on(" && ").appendTo(s, Stream.of(terms()).map(
//                b -> ('(' + b.toJavaConditionString() + ')'))
//                .iterator()
//        );
    }

    @Nullable
    public static BooleanCondition<PremiseEval> the(List<BooleanCondition<PremiseEval>> cond) {

        //remove suffix 'TRUE'
        int s = cond.size();
        if (s == 0) return null;


        if (cond.get(s - 1) == BooleanCondition.TRUE) {
            cond = cond.subList(0, s - 1);
            s--;
            if (s == 0) return null;
        }


        if (s == 1) return cond.get(0);
        return new AndCondition(cond);
    }

    public BooleanCondition<PremiseEval> without(BooleanCondition<C> condition) {
        //TODO returns a new AndCondition with condition removed, or null if it was the only item
        BooleanCondition[] x = ArrayUtils.removeElement(termCache, condition);
        if (x.length == termCache.length)
            throw new RuntimeException("element missing for removal");

        return AndCondition.the(Lists.newArrayList(x));
    }
}
