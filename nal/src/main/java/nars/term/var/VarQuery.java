package nars.term.var;

import nars.Op;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import static nars.Op.VAR_QUERY;

/**
 * normalized query variable
 */
public final class VarQuery extends AbstractVariable {

    public VarQuery(int id) {
        super(VAR_QUERY, id);
    }

    final static int RANK = Term.opX(VAR_QUERY, 0);
    @Override public int opX() { return RANK;    }

    @NotNull
    @Override
    public Op op() {
        return VAR_QUERY;
    }

    @Override
    public int vars() {
        return 1;
    }

    @Override
    public int varQuery() {
        return 1;
    }

}
