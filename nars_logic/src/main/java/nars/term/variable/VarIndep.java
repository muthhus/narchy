package nars.term.variable;

import nars.$;
import nars.Op;
import org.jetbrains.annotations.NotNull;


/**
 * normalized indep var
 */
public final class VarIndep extends Variable {

    public VarIndep(int id) {
        super(Op.VAR_INDEP, id);
    }


    @NotNull
    @Override
    public Op op() {
        return Op.VAR_INDEP;
    }

    @Override
    public int vars() {
        return 1;
    }

    @Override
    public int varDep() {
        return 0;
    }

    @Override
    public int varIndep() {
        return 1;
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
