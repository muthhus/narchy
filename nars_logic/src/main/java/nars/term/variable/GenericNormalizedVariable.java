package nars.term.variable;

import nars.Op;


public abstract class GenericNormalizedVariable extends Variable {

    public final Op type;

    public GenericNormalizedVariable(Op type, int id) {
        super(type, id);
        this.type = type;
    }
    public GenericNormalizedVariable(Op type, int id, String str) {
        super(type, id, str);
        this.type = type;
    }


    @Override
    public final int vars() {
        // pattern variable hidden in the count 0
        return type == Op.VAR_PATTERN ? 0 : 1;
    }

    @Override
    public final Op op() {
        return type;
    }


    @Override
    public final int varIndep() {
        return type == Op.VAR_INDEP ? 1 : 0;
    }

    @Override
    public final int varDep() {
        return type == Op.VAR_DEP ? 1 : 0;
    }

    @Override
    public final int varQuery() {
        return type == Op.VAR_QUERY ? 1 : 0;
    }

    @Override
    public final int varPattern() {
        return type == Op.VAR_QUERY ? 1 : 0;
    }

}
