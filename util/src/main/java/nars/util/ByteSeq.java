package nars.util;

import org.eclipse.collections.impl.factory.primitive.CharSets;

/**
 * Created by me on 10/14/16.
 */
public interface ByteSeq {

    static ByteSeq EMPTY = new ByteSeq() {

        @Override
        public int length() {
            return 0;
        }

        @Override
        public byte at(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ByteSeq subSequence(int start, int end) {
            throw new UnsupportedOperationException();
        }
    };

    int length();

    byte at(int index);

    ByteSeq subSequence(int start, int end);

    default void toArray(byte[] c, int offset) {
        int l = length();
        for (int i = 0; i < l; ) {
            c[offset++] = at(i++);
        }
    }

    /** clones a new copy TODO when can it share a ref, if start==0 and end==length ? */
    default byte[] array() {
        byte[] b = new byte[length()];
        toArray(b, 0);
        return b;
    }



    public class RawByteSeq implements ByteSeq /*implements CharSequence*/ {
        public final byte[] bytes;

        protected RawByteSeq(int capacity) {
            this(new byte[capacity]);
        }

        public RawByteSeq(byte[] bytes) {
            this.bytes = bytes;
        }

        public RawByteSeq(String s) {
            this(s.getBytes());
        }

        public void toArray(byte[] c, int offset) {
            System.arraycopy(bytes, 0, c, offset, length());
        }

        public int length() {
            return bytes.length;
        }

        public byte at(int index) {
            return this.bytes[index];
        }

        public ByteSeq subSequence(int start, int end) {
            return subSeq(start, end);
        }

        public ByteSeq subSeq(int start, int end) {
            if (start == 0 && end == length())
                return this; //no change
//
//            if(start < 0) {
//                throw new IllegalArgumentException("start " + start + " < 0");
//            } else if(end > this.length()) {
//                throw new IllegalArgumentException("end " + end + " > length " + this.length());
//            } else if(end < start) {
//                throw new IllegalArgumentException("end " + end + " < start " + start);
//            }

            return new WindowByteSeq(this.bytes, start, end);
        }

        public String toString() {
            return new String(bytes);
        }

    }
    public final class WindowByteSeq extends RawByteSeq /*implements CharSequence*/ {
        final int start;
        final int end;

        public WindowByteSeq(byte[] bytes, int start, int end) {
            super(bytes);
            if(start < 0) {
                throw new IllegalArgumentException("start " + start + " < 0");
            } else if(end > bytes.length) {
                throw new IllegalArgumentException("end " + end + " > length " + bytes.length);
            } else if(end < start) {
                throw new IllegalArgumentException("end " + end + " < start " + start);
            } else {
                this.start = start;
                this.end = end;
            }
        }

        public final void toArray(byte[] c, int offset) {
            System.arraycopy(bytes, start, c, offset, length());
        }

        public int length() {
            return this.end - this.start;
        }

        public byte at(int index) {
            return this.bytes[index + this.start];
        }

        public ByteSeq subSequence(int start, int end) {
            if(start < 0) {
                throw new IllegalArgumentException("start " + start + " < 0");
            } else if(end > this.length()) {
                throw new IllegalArgumentException("end " + end + " > length " + this.length());
            } else if(end < start) {
                throw new IllegalArgumentException("end " + end + " < start " + start);
            } else {
                return new WindowByteSeq(this.bytes, this.start + start, this.start + end);
            }
        }

        public String toString() {
            return new String(bytes);
        }


    }
}
