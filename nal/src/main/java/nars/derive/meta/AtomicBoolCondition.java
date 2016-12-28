package nars.derive.meta;

import nars.Op;
import nars.term.atom.AtomicString;
import org.jetbrains.annotations.NotNull;

/**
 * each precondition is testesd for equality by its toString() method reprsenting an immutable key.
 * so subclasses must implement a valid toString() identifier containing its components.
 * this will only be used at startup when compiling
 *
 * WARNING: no preconditions should store any state so that their instances may be used by
 * different contexts (ex: NAR's)
 */
public abstract class AtomicBoolCondition extends AtomicString implements BoolCondition {


    @NotNull
    public abstract String toString();


    @Override
    public @NotNull Op op() {
        return Op.ATOM;
    }



    @Override
    public int varIndep() {
        return 0;
    }

    @Override
    public int varDep() {
        return 0;
    }

    @Override
    public int varQuery() {
        return 0;
    }

    @Override
    public int varPattern() {
        return 0;
    }

}
