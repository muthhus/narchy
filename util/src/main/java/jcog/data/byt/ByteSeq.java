package jcog.data.byt;

import java.util.Arrays;

/**
 * Created by me on 10/14/16.
 */
public interface ByteSeq {

    ByteSeq EMPTY = new ByteSeq() {

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

    class OneByteSeq implements ByteSeq /*implements CharSequence*/ {
        public final byte b;

        public OneByteSeq(byte b) {
            this.b = b;
        }


        @Override
        public void toArray(byte[] c, int offset) { c[offset] = b; }

        @Override
        public byte[] array() {
            return new byte[] { b };
        }

        @Override
        public int length() {
            return 1;
        }

        @Override
        public byte at(int index) {
            if (index!=0)
                throw new RuntimeException();

            return this.b;
        }

        @Override
        public ByteSeq subSequence(int start, int end) {
            if ((start == 0) && (end == 1))
                return this;

            throw new UnsupportedOperationException();
//            if ((start!=0) || (end!=0))
//                throw new RuntimeException();
//            return this;
        }



        public String toString() {
            return String.valueOf((char)b);
        }

    }


    final class WindowByteSeq extends RawByteSeq /*implements CharSequence*/ {
        final int start;
        final int end;

        protected WindowByteSeq(byte[] bytes, int start, int end) {
            super(bytes);
            if(start < 0) {
                throw new IllegalArgumentException("start " + start + " < 0");
            } else if(end > bytes.length) {
                throw new IllegalArgumentException("end " + end + " > length " + bytes.length);
            } else if(end < start) {
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
