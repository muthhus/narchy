package nars.nal.meta.match;

import nars.Op;
import nars.term.variable.AbstractVariable;
import org.jetbrains.annotations.NotNull;

/**
 * normalized pattern variable
 */
public class VarPattern extends AbstractVariable {

    public VarPattern(int id) {
        super(Op.VAR_PATTERN, id);
    }




    @Override
    public final int structure() {
        return 0;
    }


    @NotNull
    @Override
    public final Op op() {
        return Op.VAR_PATTERN;
    }


    @Override
    public int volume() {
        return 1;
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
