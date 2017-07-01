package jcog.byt;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

/**
 * copied from Infinispan SimpleDataOutput
 */
public class DynByteSeq implements DataOutput, Appendable, ByteSeq {

    public static final int MIN_GROWTH_BYTES = 8;
    protected byte[] bytes;
    public int len;

    public DynByteSeq(int bufferSize) {
        this.bytes = new byte[bufferSize];
    }

    public DynByteSeq(byte[] zeroCopy) {
        this(zeroCopy, zeroCopy.length);
    }

    public DynByteSeq(byte[] zeroCopy, int len) {
        this.bytes = zeroCopy;
        this.len = len;
    }

    @Override
    public int hashCode() {
        return hash(0, len);
    }

    public int hash(int from, int to) {
        return ByteSeq.hash(bytes, from, to);
    }
    public long hash64(int from, int to) {
        return ByteSeq.hash64(bytes, from, to);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        DynByteSeq d = (DynByteSeq) obj;
        return d.len == len && Arrays.equals(bytes, 0, len, d.bytes, 0, len);
    }

    @Override
    public final int length() {
        return len;
//        return Integer.MAX_VALUE;
    }

    @Override
    public byte at(int index) {
        return bytes[index];
    }

    @Override
    public ByteSeq subSequence(int start, int end) {
        if (end-start == 1)
            return new OneByteSeq(at(start));

        if (start == 0 && end == length())
            return this; //no change

        return new WindowByteSeq(bytes, start, end);
    }

    @Override
    public void write(int v)  {
        writeByte(v);
    }


    @Override
    public void writeByte(int v)  {
        ensureSized(1);
        this.bytes[this.len++] = (byte) v;
    }

    public void fillBytes(byte b, int next) {
        int start = this.len;
        this.len += next;
        int end = this.len;
        Arrays.fill(bytes, start, end, b);
    }


    @Override
    public void write(@NotNull byte[] bytes)  {
        this.write(bytes, 0, bytes.length);
    }

    @Override
    public void write(@NotNull byte[] bytes, int off, int len)  {
        int position = ensureSized(len);
        System.arraycopy(bytes, off, this.bytes, position, len);
        this.len = position + len;
    }

    private int ensureSized(int extra) {
        int space = this.bytes.length;
        int p = this.len;
        if (space - p <= extra) {
            this.bytes = Arrays.copyOf(this.bytes, space + Math.max(extra, MIN_GROWTH_BYTES));
        }
        return p;
    }

    @Override
    public byte[] array() {
        compact();
        return bytes;
    }

    public void compact() {
        compact(null);
    }
    public void compact(byte[] forceIfSameAs) {
        int l = this.len;
        if (l > 0) {
            byte[] b = this.bytes;
            if ( b.length != l || forceIfSameAs==bytes)
                this.bytes = Arrays.copyOfRange(b, 0, l);
        }
    }


    @Override
    public void toArray(byte[] c, int offset) {
        System.arraycopy(bytes, 0, c, offset, length());
    }

    @Override
    public String toString() {
        return Arrays.toString(ArrayUtils.subarray(bytes, 0, length()));
    }


    @Override
    public void writeBoolean(boolean v)  {
        ensureSized(1);
        byte[] e = this.bytes;
        e[this.len++] = (byte) (v ? 1 : 0);
    }

    @Override
    public void writeShort(int v)  {

        int s = ensureSized(2);
        byte[] e = this.bytes;
        e[s] = (byte)(v >> 8);
        e[s + 1] = (byte) v;
        this.len += 2;
    }

    @Override
    public void writeChar(int v)  {

        int s = ensureSized(2);
        byte[] e = this.bytes;
        e[s] = (byte) (v >> 8);
        e[s + 1] = (byte) v;
        this.len += 2;

    }

    @Override
    public void writeInt(int v)  {

        int s = ensureSized(4);
        byte[] e = this.bytes;
        e[s] = (byte) (v >> 24);
        e[s + 1] = (byte) (v >> 16);
        e[s + 2] = (byte) (v >> 8);
        e[s + 3] = (byte) v;
        this.len += 4;
    }

    @Override
    public void writeLong(long v)  {

        int s = ensureSized(8);
        this.len += 8;
        byte[] e = this.bytes;
        e[s] = (byte) ((int) (v >> 56));
        e[s + 1] = (byte) ((int) (v >> 48));
        e[s + 2] = (byte) ((int) (v >> 40));
        e[s + 3] = (byte) ((int) (v >> 32));
        e[s + 4] = (byte) ((int) (v >> 24));
        e[s + 5] = (byte) ((int) (v >> 16));
        e[s + 6] = (byte) ((int) (v >> 8));
        e[s + 7] = (byte) ((int) v);
    }

    @Override
    public void writeFloat(float v)  {

        int s = ensureSized(4);
        byte[] e = this.bytes;
        this.len += 4;
        int bits = Float.floatToIntBits(v);
        e[s] = (byte) (bits >> 24);
        e[s + 1] = (byte) (bits >> 16);
        e[s + 2] = (byte) (bits >> 8);
        e[s + 3] = (byte) bits;
    }

    @Override
    public void writeDouble(double v)  {
        throw new UnsupportedOperationException("yet");
//        long bits = Double.doubleToLongBits(v);
//
//        int e = this.buffer.length - this.position;
//        if (e < 8) {
//
//            this.buffer[0] = (byte) ((int) (bits >> 56));
//            this.buffer[1] = (byte) ((int) (bits >> 48));
//            this.buffer[2] = (byte) ((int) (bits >> 40));
//            this.buffer[3] = (byte) ((int) (bits >> 32));
//            this.buffer[4] = (byte) ((int) (bits >> 24));
//            this.buffer[5] = (byte) ((int) (bits >> 16));
//            this.buffer[6] = (byte) ((int) (bits >> 8));
//            this.buffer[7] = (byte) ((int) bits);
//            this.position = 8;
//        } else {
//            int s = this.position;
//            this.position = s + 8;
//            this.buffer[s] = (byte) ((int) (bits >> 56));
//            this.buffer[s + 1] = (byte) ((int) (bits >> 48));
//            this.buffer[s + 2] = (byte) ((int) (bits >> 40));
//            this.buffer[s + 3] = (byte) ((int) (bits >> 32));
//            this.buffer[s + 4] = (byte) ((int) (bits >> 24));
//            this.buffer[s + 5] = (byte) ((int) (bits >> 16));
//            this.buffer[s + 6] = (byte) ((int) (bits >> 8));
//            this.buffer[s + 7] = (byte) ((int) bits);
//        }
//

    }

    @Override
    public void writeBytes(String s)  {
//        int len = s.length();
//
//        for (int i = 0; i < len; ++i) {
//            this.write(s.charAt(i));
//        }
        throw new UnsupportedOperationException("TODO");

    }

    @Override
    public void writeChars(String s)  {
//        int len = s.length();
//
//        for (int i = 0; i < len; ++i) {
//            this.writeChar(s.charAt(i));
//        }
        throw new UnsupportedOperationException("TODO");

    }

    @Override
    public void writeUTF(@NotNull String s) throws IOException {

        throw new UnsupportedOperationException("yet");

        //WARNING this isnt UTF8
//        this.write(strToBytes(s));
//        this.writeByte(0); //null-terminated

        //IO.writeWithPreLen(s, this);


//        byte[] ss = Hack.bytes(s);
//        this.writeShort(ss.length);
//        this.write(ss);

//        UTF8Writer
//        UTFUtils.writeUTFBytes(this, s);
    }

    @Override
    public Appendable append(CharSequence csq)  {
        return append(csq, 0, csq.length());
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end)  {
        for (int i = start; i < end; i++) {
            writeChar(csq.charAt(i)); //TODO optimize
        }
        return this;
    }

    @Override
    public Appendable append(char c)  {
        writeChar(c);
        return this;
    }

    public void appendTo(@NotNull DataOutput out) throws IOException {
        out.write(bytes, 0, len);
    }

//    @Override
//    public void flush()  {
//        int pos = this.position;
//        ByteOutput byteOutput = this.byteOutput;
//        if (byteOutput != null) {
//            if (pos > 0) {
//                byteOutput.write(this.buffer, 0, pos);
//            }
//
//            this.position = 0;
//            byteOutput.flush();
//        }
//
//    }

//    protected void shallowFlush()  {
//        int pos = this.position;
//        ByteOutput byteOutput = this.byteOutput;
//        if (byteOutput != null) {
//            if (pos > 0) {
//                byteOutput.write(this.buffer, 0, pos);
//            }
//
//            this.position = 0;
//        }
//
//    }

//    protected void start(ByteOutput byteOutput)  {
//        this.byteOutput = byteOutput;
//        this.buffer = new byte[this.bufferSize];
//    }

//    protected void finish()  {
//        try {
//            
//        } finally {
//            this.buffer = null;
//            this.byteOutput = null;
//        }
//
//    }
//
//    @Override
//    public void close()  {
//        
//        this.byteOutput.close();
//    }
}
