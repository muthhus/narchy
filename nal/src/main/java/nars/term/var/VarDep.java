package nars.term.var;

import nars.Op;
import org.jetbrains.annotations.NotNull;

/**
 * normalized dep var
 */
public final class VarDep extends AbstractVariable {

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

}
