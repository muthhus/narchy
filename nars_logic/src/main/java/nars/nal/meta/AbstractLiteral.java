package nars.nal.meta;

import nars.Op;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 1/1/16.
 */
public abstract class AbstractLiteral extends Atomic {
    @Override
    public final int complexity() {
        return 1;
    }

    @Override
    public final int vars() {
        return 0;
    }

    @Override
    public final int varQuery() {
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
    public final int varPattern() {
        return 0;
    }

    @NotNull
    @Override
    public final Op op() {
        return Op.ATOM;
    }
}
