package nars.irc;

import nars.index.TermIndex;
import nars.nal.nal8.operator.TermFunction;
import nars.term.Compound;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 7/10/16.
 */
public abstract class TermProcedure extends TermFunction {
    public TermProcedure(String name) {
        super(name);
    }

    @Override
    public boolean autoReturnVariable() {
        return true;
    }

    @Nullable
    @Override
    public abstract Object function(Compound arguments, TermIndex i);

}
