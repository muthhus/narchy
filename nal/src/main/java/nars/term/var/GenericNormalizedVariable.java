package nars.term.var;

import nars.Op;
import org.jetbrains.annotations.NotNull;


public abstract class GenericNormalizedVariable extends AbstractVariable {

    @NotNull
    public final Op type;

    public GenericNormalizedVariable(@NotNull Op type, int id) {
        super(type, id);
        this.type = type;
    }

    /** to combine multiple variables into a unique hash;
     *  this limits # of variables to 127 (or is it the full 255) per term */
    public static int multiVariable(int a, int b) {
        return ((a+1) << 8) | (b+1);
    }



    @Override
    public final int vars() {
        // pattern variable hidden in the count 0
        return type == Op.VAR_PATTERN ? 0 : 1;
    }

    @NotNull
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
        return type == Op.VAR_PATTERN ? 1 : 0;
    }

}
