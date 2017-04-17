package jcog.byt;

import jcog.Util;


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

    public static int hash(byte[] bytes, int from, int to) {
        return (int) Util.hashELF(bytes, 1, from, to);
    }

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


}
