package nars.term.variable;

import nars.Op;
import org.jetbrains.annotations.NotNull;

/**
 * normalized query variable
 */
public final class VarQuery extends Variable {

    public VarQuery(int id) {
        super(Op.VAR_QUERY, id);
    }

    @NotNull
    @Override
    public Op op() {
        return Op.VAR_QUERY;
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
        return 0;
    }

    @Override
    public int varQuery() {
        return 1;
    }

    @Override
    public int varPattern() {
        return 0;
    }

}
