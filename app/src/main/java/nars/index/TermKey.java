package nars.index;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.googlecode.concurrenttrees.radix.node.concrete.bytearray.ByteArrayCharSequence;
import nars.IO;
import nars.term.Compound;
import nars.term.Term;

import java.io.IOException;

/**
 * TODO lazily compute
 */
public class TermKey implements CharSequence {

    private final byte[] key;

    public TermKey(Term a) {

        ByteArrayDataOutput data = ByteStreams.newDataOutput(a.volume() * 8 /* ESTIMATE */);
        try {
            IO.writeTermSeq(data,a);
            //data.writeByte(0); //null terminator, signifying end-of-term
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        key = data.toByteArray();
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
    @Deprecated public int length() {
        return key.length;
//        return Integer.MAX_VALUE;
    }

    @Override
    public char charAt(int index) {
        return (char) Byte.toUnsignedInt(key[index]);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        //return str.subSequence(start, Math.min(str.length(),end));
        return new ByteArrayCharSequence(key,start,end);
    }
}
