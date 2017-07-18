package nars.term.atom;

import com.google.common.io.ByteArrayDataOutput;
import jcog.Util;

/**
 * an Atomic impl which relies on the value provided by toString()
 */
public abstract class AtomicToString implements Atomic {


    @Override public boolean equals(Object u) {

        return  (this == u)
                ||
                (
                        u instanceof Atomic &&
                        hashCode() == u.hashCode() &&
                        opX() == ((Atomic) u).opX()) &&
                        toString().equals(u.toString()
                );

    }

    @Override
    public void append(ByteArrayDataOutput out) {
        out.writeByte(op().id);
        byte[] b = bytes();
        out.writeShort(b.length);
        out.write(b);
    }

    @Override
    public byte[] bytes() {
        return toString().getBytes(/*UTF8*/);
    }

    @Override abstract public String toString();

    @Override
    public int complexity() {
        return 1;
    }

    @Override
    public int hashCode() {
        //return toString().hashCode();
        //return Util.hashCombine(toString().hashCode(), op().bit);
        return Util.hashWangJenkins( toString().hashCode() );
    }

}
