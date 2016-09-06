package nars.index;

import com.googlecode.concurrenttrees.radix.node.concrete.bytearray.ByteArrayCharSequence;
import nars.IO;
import nars.term.Term;
import nars.util.ByteBufferlet;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * TODO lazily compute
 */
public class TermKey extends ByteBufferlet implements CharSequence {

    public TermKey(@NotNull Term a) {
        super(a.volume() * 8 /* ESTIMATE */);
        try {
            IO.writeTermSeq(this, a);
            this.writeByte(0); //null terminator, signifying end-of-term
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }


    @Override
    @Deprecated
    public final int length() {
        return position;
//        return Integer.MAX_VALUE;
    }

    @Override
    public final char charAt(int index) {
        return (char) buffer[index];
        //return (char) Byte.toUnsignedInt(key[index]);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        //return str.subSequence(start, Math.min(str.length(),end));
        return new ByteArrayCharSequence(buffer, start, end);
    }


}
