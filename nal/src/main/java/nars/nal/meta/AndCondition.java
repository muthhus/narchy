package nars.nal.meta;

import nars.Op;
import nars.term.Term;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Created by me on 12/31/15.
 */
public final class AndCondition<C> extends GenericCompound<BooleanCondition<C>> implements BooleanCondition<C> {

    @NotNull
    private final BooleanCondition[] termCache;

    public AndCondition(@NotNull BooleanCondition<C>[] p) {
        this(TermVector.the((Term[])p));
    }
    public AndCondition(@NotNull Collection<BooleanCondition<C>> p) {
        this(new TermVector(p, BooleanCondition.class));
    }

    public AndCondition(@NotNull TermContainer conds) {
        super(Op.CONJUNCTION, conds);
        this.termCache = (BooleanCondition[]) conds.terms();
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

}
