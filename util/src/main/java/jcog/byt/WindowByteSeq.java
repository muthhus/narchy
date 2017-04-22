package jcog.byt;

import java.util.Arrays;

/**
 * Created by me on 4/17/17.
 */
public class WindowByteSeq extends ArrayByteSeq /*implements CharSequence*/ {
    final int start;
    final int end;

    protected WindowByteSeq(byte[] bytes, int start, int end) {
        super(bytes);
        if (start < 0) {
            throw new IllegalArgumentException("start " + start + " < 0");
        } else if (end > bytes.length) {
            throw new IllegalArgumentException("end " + end + " > length " + bytes.length);
        } else if (end < start) {
            throw new IllegalArgumentException("end " + end + " < start " + start);
        } else if (start == 0 && end == bytes.length) {
            throw new IllegalArgumentException("window unnecessary");
        }

        this.start = start;
        this.end = end;

    }

    @Override
    public final void toArray(byte[] c, int offset) {
        System.arraycopy(bytes, start, c, offset, length());
    }

    @Override
    public byte[] array() {
        return Arrays.copyOfRange(bytes, start, end);
    }

    @Override
    public int length() {
        return this.end - this.start;
    }

    @Override
    public byte at(int index) {
        return this.bytes[index + this.start];
    }

    @Override
    public ByteSeq subSequence(int start, int end) {
        if (start < 0) {
            throw new IllegalArgumentException("start " + start + " < 0");
        } else if (end > this.length()) {
            throw new IllegalArgumentException("end " + end + " > length " + this.length());
        } else if (end < start) {
            throw new IllegalArgumentException("end " + end + " < start " + start);
        } else {
            return new WindowByteSeq(this.bytes, this.start + start, this.start + end);
        }
    }

    public String toString() {
        return new String(array());
    }


}
