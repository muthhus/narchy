package nars.term.atom;

import com.google.common.io.ByteArrayDataOutput;
import jcog.Util;
import nars.Op;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static java.lang.System.arraycopy;

/**
 * an Atomic impl which relies on the value provided by toString()
 */
public abstract class AtomicConst implements Atomic {


    private final transient byte[] bytesCached;
    protected final transient int hashCached;

    protected AtomicConst(Op op, @Nullable String s) {
        if (s == null) s = toString(); //must be a constant method
        int slen = s.length();

        byte[] stringbytes = s.getBytes();
        byte[] sbytes = new byte[stringbytes.length + 3];
        sbytes[0] = (op != null ? op : op()).id;
        sbytes[1] = (byte) (slen >> 8 & 0xff);
        sbytes[2] = (byte) (slen & 0xff);
        arraycopy(stringbytes, 0, sbytes, 3, stringbytes.length);
        this.bytesCached = sbytes;

        this.hashCached = Util.hashWangJenkins(s.hashCode());
    }

    @Override
    public boolean equals(Object u) {
        if (this == u) return true;

        if (u instanceof AtomicConst) {
            AtomicConst c = (AtomicConst) u;
            return hashCached == c.hashCached && Arrays.equals(bytesCached, c.bytesCached);
        } else if (u instanceof Atomic) {
            if (hashCached != u.hashCode())
                return false;
            Atomic a = (Atomic) u;
            return opX() == a.opX() && toString().equals(a.toString());
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
        return hashCached;
    }

}
