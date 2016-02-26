package nars.op.sys.java;

import nars.Op;
import nars.term.atom.AtomicStringConstant;

/** refers to a java object instance TODO */
public final class ObjRef<O> extends AtomicStringConstant {

    public final O value;

    public ObjRef(String name, O value) {

        this.value = value;
    }

    public O object() {
        return value;
    }

    @Override
    public String toString() {
        return null;
    }

    @Override
    public    Op op() {
        return null;
    }
}
