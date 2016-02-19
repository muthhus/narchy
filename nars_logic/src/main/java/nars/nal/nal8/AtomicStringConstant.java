package nars.nal.nal8;

import nars.nal.meta.AtomicString;

/**
 * Created by me on 2/18/16.
 */
public abstract class AtomicStringConstant extends AtomicString {

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

    @Override
    public int vars() {
        return 0;
    }
}
