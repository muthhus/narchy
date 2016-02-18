package nars.term.variable;

import nars.$;
import nars.Op;
import org.jetbrains.annotations.NotNull;

/**
 * normalized dep var
 */
public final class VarDep extends Variable {

    public VarDep(int id) {
        super(Op.VAR_DEP, id);
    }


    @NotNull
    @Override
    public Op op() {
        return Op.VAR_DEP;
    }

    @Override
    public int vars() {
        return 1;
    }

    @Override
    public int varDep() {
        return 1;
    }

    @Override
    public int varIndep() {
        return 0;
    }

    @Override
    public int varQuery() {
        return 0;
    }

    @Override
    public int varPattern() {
        return 0;
    }
}
