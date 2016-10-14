package nars.util;

import com.fasterxml.jackson.core.io.UTF8Writer;
import com.google.common.base.Utf8;
import com.googlecode.concurrenttrees.radix.node.concrete.bytearray.ByteArrayCharSequence;

import java.io.DataOutput;
import java.io.IOException;

/**
 * copied from Infinispan SimpleDataOutput
 */
public class ByteBufferlet implements DataOutput,CharSequence,Appendable {

    public static final int MIN_GROWTH_BYTES = 64;
    public byte[] buffer;
    public int position;

    public ByteBufferlet(int bufferSize) {
        super();
        this.buffer = new byte[bufferSize];
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


    @Override
    public void write(int v) throws IOException {
        byte[] e = this.buffer;

        ensureSized(1);
        e[this.position++] = (byte) v;
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        this.write(bytes, 0, bytes.length);
    }

    @Override
    public void write(byte[] bytes, int off, int len) throws IOException {
        int position = ensureSized(len);
        System.arraycopy(bytes, off, this.buffer, position, len);
        this.position = position + len;
    }

    private final int ensureSized(int extra) {
        int space = this.buffer.length;
        if (space - position <= extra) {
            byte[] newBuffer = new byte[space + Math.max(MIN_GROWTH_BYTES, 2 * extra)];
            System.arraycopy(this.buffer, 0, newBuffer, 0, position);
            this.buffer = newBuffer;
        }
        return this.position;
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        byte[] e = this.buffer;
        ensureSized(1);
        e[this.position++] = (byte) (v ? 1 : 0);
    }

    @Override
    public void writeByte(int v) throws IOException {
        byte[] e = this.buffer;
        ensureSized(1);
        e[this.position++] = (byte) v;
    }

    @Override
    public void writeShort(int v) throws IOException {
        byte[] e = this.buffer;
        int s = ensureSized(2);
        e[s] = (byte) (v >> 8);
        e[s + 1] = (byte) v;
        this.position += 2;
    }

    @Override
    public void writeChar(int v) throws IOException {

        byte[] e = this.buffer;
        int s = ensureSized(2);
        e[s] = (byte) (v >> 8);
        e[s + 1] = (byte) v;
        this.position += 2;

    }

    @Override
    public void writeInt(int v) throws IOException {

        byte[] e = this.buffer;
        int s = ensureSized(4);
        e[s] = (byte) (v >> 24);
        e[s + 1] = (byte) (v >> 16);
        e[s + 2] = (byte) (v >> 8);
        e[s + 3] = (byte) v;
        this.position += 4;
    }

    @Override
    public void writeLong(long v) throws IOException {

        byte[] e = this.buffer;
        int s = ensureSized(8);
        e[s] = (byte) ((int) (v >> 56));
        e[s + 1] = (byte) ((int) (v >> 48));
        e[s + 2] = (byte) ((int) (v >> 40));
        e[s + 3] = (byte) ((int) (v >> 32));
        e[s + 4] = (byte) ((int) (v >> 24));
        e[s + 5] = (byte) ((int) (v >> 16));
        e[s + 6] = (byte) ((int) (v >> 8));
        e[s + 7] = (byte) ((int) v);
        this.position += 8;
    }

    @Override
    public void writeFloat(float v) throws IOException {

        byte[] e = this.buffer;
        int s = ensureSized(4);
        int bits = Float.floatToIntBits(v);
        e[s] = (byte) (bits >> 24);
        e[s + 1] = (byte) (bits >> 16);
        e[s + 2] = (byte) (bits >> 8);
        e[s + 3] = (byte) bits;
        this.position += 4;
    }

    @Override
    public void writeDouble(double v) throws IOException {
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
    public void writeBytes(String s) throws IOException {
        int len = s.length();

        for (int i = 0; i < len; ++i) {
            this.write(s.charAt(i));
        }

    }

    @Override
    public void writeChars(String s) throws IOException {
        int len = s.length();

        for (int i = 0; i < len; ++i) {
            this.writeChar(s.charAt(i));
        }

    }

    @Override
    public void writeUTF(String s) throws IOException {
        //throw new UnsupportedOperationException("yet");

        //WARNING this isnt UTF8
        this.write(s.getBytes());

//
//        s.getBytes()
//        this.writeBytes(s);
//        UTF8Writer
//        this.writeShort(s.length());
//        UTFUtils.writeUTFBytes(this, s);
    }

    @Override
    public Appendable append(CharSequence csq) throws IOException {
        return append(csq, 0, csq.length());
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) throws IOException {
        for (int i = start; i < end; i++) {
            writeChar(csq.charAt(i)); //TODO optimize
        }
        return this;
    }

    @Override
    public Appendable append(char c) throws IOException {
        writeChar(c);
        return this;
    }

//    @Override
//    public void flush() throws IOException {
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

//    protected void shallowFlush() throws IOException {
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

//    protected void start(ByteOutput byteOutput) throws IOException {
//        this.byteOutput = byteOutput;
//        this.buffer = new byte[this.bufferSize];
//    }

//    protected void finish() throws IOException {
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
//    public void close() throws IOException {
//        
//        this.byteOutput.close();
//    }
}
