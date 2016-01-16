package nars.nal.meta;

import nars.Op;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 1/1/16.
 */
public abstract class AbstractLiteral extends Atomic {
    @Override
    public int complexity() {
        return 1;
    }

    @Override
    public int vars() {
        return 0;
    }

    @Override
    public int varQuery() {
        return 0;
    }

    @Override
    public int varDep() {
        return 0;
    }

    @Override
    public int varIndep() {
        return 0;
    }

    @NotNull
    @Override
    public Op op() {
        return Op.ATOM;
    }
}
