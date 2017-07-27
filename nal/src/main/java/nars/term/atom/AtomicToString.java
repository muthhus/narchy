package nars.term.atom;

import com.google.common.io.ByteArrayDataOutput;
import jcog.Util;
import nars.Op;
import nars.term.Term;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

/**
 * an Atomic impl which relies on the value provided by toString()
 */
public abstract class AtomicToString implements Atomic {


    private final transient byte[] bytesCached;
    protected final transient int hashCached;

    public AtomicToString(Op op, @Nullable String s) {
        if (s == null) s = toString(); //must be a constant method
        int slen = s.length();
        this.bytesCached = ArrayUtils.addAll(
            new byte[] { (op!=null ? op : op()) .id, (byte)(slen>>8), (byte)slen },
            s.getBytes()
        );
        this.hashCached = Util.hashWangJenkins( s.hashCode() );
    }

    public AtomicToString() {
        this(null, null);
    }

    @Override public boolean equals(Object u) {

        return  (this == u)
                ||
                (
                        u instanceof Atomic &&
                        hashCached == u.hashCode() &&
                        opX() == ((Term) u).opX()) &&
                        toString().equals(u.toString()
                );

    }

    @Override
    public void append(ByteArrayDataOutput out) {
//        out.writeByte(op().id);
//        byte[] b = bytesCached;
//        out.writeShort(b.length);
//        out.write(b);
        out.write(bytesCached);
    }



    @Override abstract public String toString();

    @Override
    public int complexity() {
        return 1;
    }

    @Override
    public int hashCode() {
        return hashCached;
    }

}
