package nars.term.var;

import nars.Op;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import static nars.Op.VAR_INDEP;


/**
 * normalized indep var
 */
public final class VarIndep extends AbstractVariable {

    public VarIndep(int id) {
        super(VAR_INDEP, id);
    }

    final static int RANK = Term.opX(VAR_INDEP, 0);
    @Override public int opX() { return RANK;    }


    @NotNull
    @Override
    public Op op() {
        return VAR_INDEP;
    }

    @Override
    public int vars() {
        return 1;
    }

    @Override
    public int varIndep() {
        return 1;
    }


}
