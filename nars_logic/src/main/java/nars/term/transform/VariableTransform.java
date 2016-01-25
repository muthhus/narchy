package nars.term.transform;

import nars.term.Compound;
import nars.term.Term;
import nars.term.variable.Variable;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 6/1/15.
 */
public abstract class VariableTransform implements CompoundTransform<Compound, Variable> {

    @Override
    public final boolean test(Term possiblyAVariable) {
        return (possiblyAVariable instanceof Variable);
    }

    @Override
    public boolean testSuperTerm(@NotNull Compound t) {
        //prevent executing on any superterms that contain no variables, because this would have no effect
        return t.hasVar();
    }
}
