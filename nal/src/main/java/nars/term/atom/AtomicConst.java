package nars.term.atom;

import com.google.common.io.ByteArrayDataOutput;
import jcog.Util;
import nars.Op;

import java.util.Arrays;

import static java.lang.System.arraycopy;

/**
 * an Atomic impl which relies on the value provided by toString()
 */
public abstract class AtomicConst implements Atomic {


    public final transient byte[] bytesCached;
    protected final transient int hash;

    protected AtomicConst(byte[] raw) {
        this.bytesCached = raw;
        this.hash = (int) Util.hashELF(raw, 1); //Util.hashWangJenkins(s.hashCode());
    }

    protected AtomicConst(Op op, String s) {
        this(bytes(op, s));
    }

    private static byte[] bytes(Op op, String str) {
        //if (s == null) s = toString(); //must be a constant method
        //int slen = str.length(); //TODO will this work for UTF-16 containing strings?

        byte[] stringbytes = str.getBytes();
        int slen = stringbytes.length;

        byte[] sbytes = new byte[slen + 3];
        sbytes[0] = op.id; //(op != null ? op : op()).id;
        sbytes[1] = (byte) (slen >> 8 & 0xff);
        sbytes[2] = (byte) (slen & 0xff);
        arraycopy(stringbytes, 0, sbytes, 3, slen);
        return sbytes;
    }

    @Override
    public boolean equals(Object u) {
        if (this == u) return true;

        if (u instanceof AtomicConst) {
            AtomicConst c = (AtomicConst) u;
            return hash == c.hash && Arrays.equals(bytesCached, c.bytesCached);
        } else if (u instanceof Atomic) {
            return Atomic.equals(this, (Atomic)u);
        }
        return false;
    }

    @Override
    public void append(ByteArrayDataOutput out) {
//        out.writeByte(op().id);
//        byte[] b = bytesCached;
//        out.writeShort(b.length);
//        out.write(b);
        out.write(bytesCached);
    }

    @Override
    public int complexity() {
        return 1;
    }

    @Override
    public float voluplexity() {
        return 1;
    }

    @Override
    public int hashCode() {
        return hash;
    }

}
