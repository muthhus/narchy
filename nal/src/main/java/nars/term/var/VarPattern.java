package nars.term.var;

import nars.Op;
import org.jetbrains.annotations.NotNull;

/**
 * normalized pattern variable
 */
public class VarPattern extends AbstractVariable {

    public VarPattern(int id) {
        super(Op.VAR_PATTERN, id);
    }


    /** special case: pattern variables contribute no structure currently */
    @Override public final int structure() {
        return 0;
    }


    @NotNull
    @Override
    public final Op op() {
        return Op.VAR_PATTERN;
    }


    /**
     * pattern variable hidden in the count 0
     */
    @Override
    public final int vars() {
        return 0;
    }

    @Override
    public final int varDep() {
        return 0;
    }

    @Override
    public final int varIndep() {
        return 0;
    }

    @Override
    public final int varQuery() {
        return 0;
    }

    @Override
    public final int varPattern() {
        return 1;
    }


}
