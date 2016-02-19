package nars.java;

import nars.Op;
import nars.nal.nal8.AtomicStringConstant;

/** refers to a java object instance TODO */
public final class AtomObject<O> extends AtomicStringConstant {

    public final O value;

    public AtomObject(String name, O value) {

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
