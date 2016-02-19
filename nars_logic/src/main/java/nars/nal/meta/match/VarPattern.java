package nars.nal.meta.match;

import nars.$;
import nars.Op;
import nars.term.Compound;
import nars.term.SubtermVisitor;
import nars.term.Term;
import nars.term.variable.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Predicate;

/**
 * normalized pattern variable
 */
public class VarPattern extends Variable {

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
