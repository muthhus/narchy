package nars.nal.meta.op;

import nars.nal.meta.AtomicBoolCondition;
import nars.nal.meta.PremiseEval;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * 'container' contains 'contained' but also not equal to it
 */
public final class Contains extends AtomicBoolCondition {

    final Term container, contained;

    @NotNull
    private final String id;

    /** TODO the shorter path should be set for 'a' if possible, because it will be compared first */
    public Contains(Term container, Term contained) {
        this.id = getClass().getSimpleName() + '(' + container + "," + contained + ')';
        this.container = container;
        this.contained = contained;
    }


    @NotNull
    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean run(@NotNull PremiseEval ff, int now) {
        Term container = ff.resolve(this.container);
        if (container!=null) {
            Term contained = ff.resolve(this.contained);
            return (contained instanceof Compound && !container.equals(contained) && ((Compound)container).containsTermRecursively(contained));
        }
        return false;
    }

}
