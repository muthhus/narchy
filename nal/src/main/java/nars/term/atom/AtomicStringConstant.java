package nars.term.atom;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.Serializable;

/**
 * Created by me on 2/18/16.
 */
public abstract class AtomicStringConstant extends AtomicString {

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
