package nars.term.atom;

import nars.Op;
import org.jetbrains.annotations.NotNull;


/** atom backed by a native java String */
public class StringAtom extends AbstractStringAtomRaw {

    public StringAtom(String id) {
        super(id);
    }

    @NotNull
    @Override
    public final Op op() {
        return Op.ATOM;
    }

    static final int AtomBit = Op.ATOM.bit();

    @Override
    public final int structure() {
        return AtomBit;
    }

    @Override
    public final int complexity() {
        return 1;
    }



    @Override
    public final int varIndep() {
        return 0;
    }

    @Override
    public final int varDep() {
        return 0;
    }

    @Override
    public final int varQuery() {
        return 0;
    }

    @Override
    public final int varPattern() {
        return 0;
    }

    @Override
    public final int vars() {
        return 0;
    }

}
